package org.bitstrings.idea.plugins.mavenprime.run;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.build.MavenBuildOutputListener;
import org.bitstrings.idea.plugins.mavenprime.build.MavenBuildProgressDescriptor;
import org.bitstrings.idea.plugins.mavenprime.build.SpyEvents;
import org.bitstrings.idea.plugins.mavenprime.build.SpySession;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenCommandLineBuilder;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.execution.RequestProblem;
import org.bitstrings.idea.plugins.mavenprime.tests.ForkedTestDebug;
import org.bitstrings.idea.plugins.mavenprime.tests.TestReportWatcher;
import org.bitstrings.idea.plugins.mavenprime.tests.TestTreeMessages;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.build.BuildViewManager;
import com.intellij.build.progress.BuildProgress;
import com.intellij.build.progress.BuildProgressDescriptor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;

public final class MavenPrimeRunProfileState
    extends CommandLineState
{
    private static final String TEST_FRAMEWORK = "MavenPrime";

    private static final long POLL_DELAY_MILLIS = 500L;

    private static final String DEFAULT_BUILD_DIRECTORY = "target";

    private final MavenPrimeRunConfiguration configuration;

    private MavenPrimeRequest effectiveRequest;

    private String debugHost;

    private int debugPort;

    MavenPrimeRunProfileState(ExecutionEnvironment environment, MavenPrimeRunConfiguration configuration)
    {
        super(environment);

        this.configuration = configuration;
    }

    void debugForkedTestsAt(String host, int port)
    {
        this.debugHost = host;
        this.debugPort = port;
    }

    @Override
    protected ConsoleView createConsole(Executor executor)
        throws ExecutionException
    {
        if (!effectiveRequest().selectsTests())
        {
            return super.createConsole(executor);
        }

        return SMTestRunnerConnectionUtil.createConsole(
            TEST_FRAMEWORK, new SMTRunnerConsoleProperties(configuration, TEST_FRAMEWORK, executor));
    }

    @Override
    protected ProcessHandler startProcess()
        throws ExecutionException
    {
        Project project = configuration.getProject();

        MavenPrimeRequest request = effectiveRequest();

        RequestProblem problem = request.validate();

        if (problem != null)
        {
            throw new ExecutionException(problem.getMessage());
        }

        if ((debugPort > 0) && !ForkedTestDebug.attach(request, debugHost, debugPort))
        {
            throw new ExecutionException(MavenPrimeBundle.message("mavenprime.test.debug.error.noTests"));
        }

        MavenInstallation installation =
            MavenInstallationService
                .getInstance(project)
                .resolve(request.distribution, request.workingDirectory);

        if (!installation.isValid())
        {
            throw new ExecutionException(installation.getError());
        }

        MavenBuildOutputListener listener = buildOutputListener(project, request);

        listener.start();

        SpySession session =
            SpySession.timed(
                project,
                configuration.getName(),
                request.getGoalLine(),
                line -> SpyEvents.parse(line).ifPresent(listener::accept));

        KillableColoredProcessHandler processHandler;

        try
        {
            processHandler =
                new KillableColoredProcessHandler(
                    MavenCommandLineBuilder.build(project, request, installation, session.handshake()));
        }
        catch (ExecutionException | RuntimeException failure)
        {
            listener.fail(StringUtils.defaultIfBlank(failure.getMessage(), failure.toString()));

            session.finish();

            throw failure;
        }

        ProcessTerminatedListener.attach(processHandler, project);

        processHandler.addProcessListener(new SessionCloser(session, listener));

        if (request.selectsTests())
        {
            watchTestReports(project, processHandler, request);
        }

        return processHandler;
    }

    private MavenPrimeRequest effectiveRequest()
    {
        if (effectiveRequest == null)
        {
            effectiveRequest =
                BuildContext.getInstance(configuration.getProject()).effective(configuration.getRequest());
        }

        return effectiveRequest;
    }

    private static void watchTestReports(
        Project project, ProcessHandler processHandler, MavenPrimeRequest request)
    {
        List<Path> buildDirectories = testedBuildDirectories(project, request);

        TestReportWatcher watcher =
            new TestReportWatcher(
                buildDirectories,
                System.currentTimeMillis(),
                message -> processHandler.notifyTextAvailable(message + '\n', ProcessOutputTypes.STDOUT));

        AtomicReference<ScheduledFuture<?>> polling = new AtomicReference<>();

        processHandler.addProcessListener(
            new ProcessListener()
            {
                @Override
                public void startNotified(ProcessEvent event)
                {
                    processHandler.notifyTextAvailable(
                        TestTreeMessages.frameworkStarted() + '\n', ProcessOutputTypes.STDOUT);

                    polling.set(
                        AppExecutorUtil
                            .getAppScheduledExecutorService()
                            .scheduleWithFixedDelay(
                                watcher::scan, POLL_DELAY_MILLIS, POLL_DELAY_MILLIS, TimeUnit.MILLISECONDS));
                }

                @Override
                public void processTerminated(ProcessEvent event)
                {
                    ScheduledFuture<?> scheduled = polling.getAndSet(null);

                    if (scheduled != null)
                    {
                        scheduled.cancel(false);
                    }

                    watcher.scan();

                    if (!watcher.foundAnyReport())
                    {
                        processHandler.notifyTextAvailable(
                            MavenPrimeBundle.message(
                                "mavenprime.test.error.noReports", pathsOf(buildDirectories)) + '\n',
                            ProcessOutputTypes.STDERR);
                    }
                }
            });
    }

    static List<Path> testedBuildDirectories(Project project, MavenPrimeRequest request)
    {
        Path root = Paths.get(request.workingDirectory);

        List<Path> directories = new ArrayList<>();

        for (MavenProject module : MavenProjects.all(project))
        {
            Path directory = Paths.get(module.getDirectory());

            if (directory.equals(root) || directory.startsWith(root))
            {
                directories.add(Paths.get(module.getBuildDirectory()));
            }
        }

        if (directories.isEmpty())
        {
            directories.add(root.resolve(DEFAULT_BUILD_DIRECTORY));
        }

        return directories;
    }

    private static String pathsOf(List<Path> directories)
    {
        return directories.stream().map(Path::toString).collect(Collectors.joining(", "));
    }

    private static final class SessionCloser
        implements ProcessListener
    {
        private final SpySession session;

        private final MavenBuildOutputListener listener;

        SessionCloser(SpySession session, MavenBuildOutputListener listener)
        {
            this.session = session;
            this.listener = listener;
        }

        @Override
        public void processTerminated(ProcessEvent event)
        {
            int exitCode = event.getExitCode();

            session.finish(() -> listener.finish(exitCode));
        }
    }

    private MavenBuildOutputListener buildOutputListener(Project project, MavenPrimeRequest request)
    {
        BuildProgress<BuildProgressDescriptor> progress = BuildViewManager.createBuildProgress(project);

        return new MavenBuildOutputListener(
            project,
            progress,
            descriptorFor(getEnvironment(), configuration.getName(), request.workingDirectory),
            Paths.get(request.workingDirectory));
    }

    // A progress has no id until start(descriptor), so the id has to come from the caller.
    static MavenBuildProgressDescriptor descriptorFor(Object buildId, String title, String workingDirectory)
    {
        return new MavenBuildProgressDescriptor(buildId, title, workingDirectory);
    }
}
