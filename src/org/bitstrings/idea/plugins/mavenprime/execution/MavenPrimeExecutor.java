package org.bitstrings.idea.plugins.mavenprime.execution;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.build.SpySession;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionKind;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenVersion;
import org.bitstrings.idea.plugins.mavenprime.distribution.MvndLayout;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class MavenPrimeExecutor
{
    private static final Logger LOG = Logger.getInstance(MavenPrimeExecutor.class);

    private final Project project;

    public MavenPrimeExecutor(Project project)
    {
        this.project = project;
    }

    public static MavenPrimeExecutor getInstance(Project project)
    {
        return project.getService(MavenPrimeExecutor.class);
    }

    public void execute(MavenPrimeRequest request, ExecutionMode mode)
    {
        launchRequest(request, mode, null);
    }

    public boolean executeAndWait(MavenPrimeRequest request, ExecutionMode mode)
    {
        CompletionGate gate = new CompletionGate();

        AtomicBoolean launched = new AtomicBoolean();

        ApplicationManager
            .getApplication()
            .invokeAndWait(() -> launched.set(launchRequest(request, mode, gate)));

        return launched.get() && gate.awaitSuccess();
    }

    private boolean launchRequest(MavenPrimeRequest request, ExecutionMode mode, ProgramRunner.Callback callback)
    {
        MavenPrimeRequest effectiveRequest = BuildContext.getInstance(project).effective(request);

        effectiveRequest.name = buildName(effectiveRequest);

        LOG.info(
            "Maven Prime execute goals=" + effectiveRequest.getGoalLine()
                + " mode=" + mode
                + " workingDirectory=" + effectiveRequest.workingDirectory
                + " distribution=" + effectiveRequest.distribution);

        RequestProblem problem = effectiveRequest.validate();

        if (problem != null)
        {
            MavenPrimeNotifications.error(project, problem.getMessage());

            return false;
        }

        MavenInstallation installation = resolveWithProgress(effectiveRequest, mode);

        if (installation == null)
        {
            return false;
        }

        if (!installation.isValid())
        {
            MavenPrimeNotifications.error(project, installation.getError());

            return false;
        }

        LOG.info(
            "Maven Prime resolved installation=" + installation.getDisplayName()
                + " daemon=" + installation.isDaemon()
                + " home=" + installation.getHome());

        warnAboutDroppedFlags(effectiveRequest, installation);

        if (installation.isDaemon())
        {
            return launch(new DaemonEngine(project).createSettings(effectiveRequest, installation), mode, callback);
        }

        SpySession session = SpySession.timed(project, effectiveRequest.name, effectiveRequest.getGoalLine(), null);

        try
        {
            RunnerAndConfigurationSettings settings =
                new IdeaMavenEngine(project, session.handshake())
                    .createSettings(effectiveRequest, installation);

            SpySessionAttachment attachment =
                SpySessionAttachment.attach(project, settings.getConfiguration(), session);

            if (launch(settings, mode, callback))
            {
                return true;
            }

            attachment.detach();

            session.finish();

            return false;
        }
        catch (RuntimeException failure)
        {
            session.finish();

            throw failure;
        }
    }

    private MavenInstallation resolveWithProgress(MavenPrimeRequest request, ExecutionMode mode)
    {
        AtomicReference<MavenInstallation> resolved = new AtomicReference<>();

        boolean completed =
            ProgressManager
                .getInstance()
                .runProcessWithProgressSynchronously(
                    () -> resolved.set(resolveFor(request, mode)),
                    MavenPrimeBundle.message("mavenprime.execution.resolving"),
                    true,
                    project);

        return completed ? resolved.get() : null;
    }

    private MavenInstallation resolveFor(MavenPrimeRequest request, ExecutionMode mode)
    {
        MavenInstallation installation = resolve(request.distribution, request.workingDirectory);

        return (mode.isDebug() && installation.isDaemon())
            ? switchToEmbeddedMaven(request, installation)
            : installation;
    }

    private boolean launch(
        RunnerAndConfigurationSettings settings, ExecutionMode mode, ProgramRunner.Callback callback)
    {
        Executor executor = mode.getExecutor();

        ProgramRunner<?> runner = ProgramRunner.getRunner(executor.getId(), settings.getConfiguration());

        if (runner == null)
        {
            LOG.warn(
                "Maven Prime has no ProgramRunner for executor=" + executor.getId()
                    + " configuration=" + settings.getType().getId());

            MavenPrimeNotifications.error(
                project, MavenPrimeBundle.message("mavenprime.execution.error.noRunner", executor.getId()));

            return false;
        }

        LOG.info("Maven Prime launching with runner=" + runner.getRunnerId() + " executor=" + executor.getId());

        RunManager.getInstance(project).setTemporaryConfiguration(settings);

        try
        {
            ProgramRunnerUtil.executeConfigurationAsync(
                ExecutionEnvironmentBuilder.create(executor, settings).build(), true, true, callback);
        }
        catch (ExecutionException failure)
        {
            LOG.warn("Maven Prime could not build an execution environment", failure);

            MavenPrimeNotifications.error(
                project, MavenPrimeBundle.message("mavenprime.execution.error.noRunner", executor.getId()));

            return false;
        }

        return true;
    }

    private static String buildName(MavenPrimeRequest request)
    {
        return StringUtils.defaultIfBlank(request.name, request.getGoalLine());
    }

    private static final class CompletionGate
        implements ProgramRunner.Callback
    {
        private static final int START_TIMEOUT_SECONDS = 30;

        private final CountDownLatch started = new CountDownLatch(1);

        private final CountDownLatch finished = new CountDownLatch(1);

        private final AtomicBoolean running = new AtomicBoolean();

        private final AtomicInteger exitCode = new AtomicInteger(-1);

        @Override
        public void processStarted(RunContentDescriptor descriptor)
        {
            ProcessHandler handler = (descriptor == null) ? null : descriptor.getProcessHandler();

            if (handler == null)
            {
                abandon();

                return;
            }

            running.set(true);

            handler.addProcessListener(
                new ProcessListener()
                {
                    @Override
                    public void processTerminated(ProcessEvent event)
                    {
                        complete(event.getExitCode());
                    }
                });

            started.countDown();

            if (handler.isProcessTerminated())
            {
                complete(Objects.requireNonNullElse(handler.getExitCode(), Integer.valueOf(-1)).intValue());
            }
        }

        @Override
        public void processNotStarted(Throwable error)
        {
            LOG.info("Maven Prime execution did not start", error);

            abandon();
        }

        boolean awaitSuccess()
        {
            try
            {
                if (!started.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS) || !running.get())
                {
                    return false;
                }

                finished.await();
            }
            catch (InterruptedException interrupted)
            {
                Thread.currentThread().interrupt();

                return false;
            }

            return exitCode.get() == 0;
        }

        private void abandon()
        {
            started.countDown();
            finished.countDown();
        }

        private void complete(int code)
        {
            exitCode.set(code);

            finished.countDown();
        }
    }

    private MavenInstallation switchToEmbeddedMaven(MavenPrimeRequest request, MavenInstallation daemon)
    {
        if (!daemon.isValid())
        {
            return daemon;
        }

        Optional<Path> daemonHome = daemon.getHome();

        if (daemonHome.isEmpty())
        {
            return MavenInstallation.invalid(
                DistributionKind.MVND_HOME,
                MavenPrimeBundle.message("mavenprime.execution.error.noEmbeddedMaven"));
        }

        DistributionSpec embedded =
            DistributionSpec.mavenHome(MvndLayout.embeddedMavenHome(daemonHome.get()).toString());

        MavenInstallation installation = resolve(embedded, request.workingDirectory);

        if (!installation.isValid())
        {
            return MavenInstallation.invalid(
                DistributionKind.MVND_HOME,
                MavenPrimeBundle.message("mavenprime.execution.error.debugNotAvailable", installation.getError()));
        }

        request.distribution = embedded;

        MavenPrimeNotifications.info(
            project, MavenPrimeBundle.message("mavenprime.execution.debugWithEmbeddedMaven", embedded.path));

        return installation;
    }

    private void warnAboutDroppedFlags(MavenPrimeRequest request, MavenInstallation installation)
    {
        MavenVersion mavenVersion = installation.getMavenVersion();

        Set<MavenFlag> dropped =
            CommandLineRenderer.unsupportedFlags(
                request.flags,
                installation,
                MavenOptionCatalog.getInstance(project).optionsOf(installation, request.jreName));

        for (MavenFlag flag : dropped)
        {
            MavenPrimeNotifications.warning(
                project,
                mavenVersion.isKnown()
                    ? MavenPrimeBundle.message(
                        "mavenprime.execution.unsupportedFlag", flag.getOption(), mavenVersion.toString())
                    : MavenPrimeBundle.message(
                        "mavenprime.execution.unsupportedFlagUnknownVersion", flag.getOption()));
        }
    }

    private MavenInstallation resolve(DistributionSpec spec, String workingDirectory)
    {
        return MavenInstallationService.getInstance(project).resolve(spec, workingDirectory);
    }
}
