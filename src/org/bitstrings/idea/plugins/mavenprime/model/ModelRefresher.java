package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.build.SpyHandshake;
import org.bitstrings.idea.plugins.mavenprime.build.SpySession;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenCommandLineBuilder;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.execution.OutputLevel;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

@Service(Service.Level.PROJECT)
public final class ModelRefresher
{
    public static final Topic<RefreshListener> TOPIC =
        Topic.create("Maven Prime model refresh", RefreshListener.class);

    private static final String VALIDATE_GOAL = "validate";

    private final Project project;

    private final RefreshCoalescer coalescer = new RefreshCoalescer();

    public ModelRefresher(Project project)
    {
        this.project = project;
    }

    public static ModelRefresher getInstance(Project project)
    {
        return project.getService(ModelRefresher.class);
    }

    public boolean isRunning()
    {
        return coalescer.isRunning();
    }

    public void refresh()
    {
        if (!start())
        {
            MavenPrimeNotifications.error(project, MavenPrimeBundle.message("mavenprime.model.refresh.noRoot"));
        }
    }

    public void refreshUnread()
    {
        if (MavenPrimeSettings.getInstance(project).autoRefresh
            && ProvenanceLog.getInstance(project).getModules().isEmpty())
        {
            start();
        }
    }

    private boolean start()
    {
        List<MavenProject> roots = MavenProjects.roots(project);

        if (roots.isEmpty())
        {
            return false;
        }

        if (coalescer.begin())
        {
            UiTopics.publish(project, TOPIC, RefreshListener::modelRefreshStateChanged);

            try
            {
                new RefreshTask(project, roots.get(0).getDirectory(), this).queue();
            }
            catch (RuntimeException failure)
            {
                coalescer.end();

                UiTopics.publish(project, TOPIC, RefreshListener::modelRefreshStateChanged);

                throw failure;
            }
        }

        return true;
    }

    private void finished(boolean cancelled)
    {
        boolean owed = coalescer.end();

        UiTopics.publish(project, TOPIC, RefreshListener::modelRefreshStateChanged);

        if (owed && !cancelled)
        {
            refresh();
        }
    }

    public interface RefreshListener
    {
        void modelRefreshStateChanged();
    }

    private static MavenPrimeRequest requestFor(Project project, String workingDirectory)
    {
        MavenPrimeRequest request = MavenPrimeRequest.in(workingDirectory, List.of(VALIDATE_GOAL));

        request.outputLevel = OutputLevel.QUIET;
        request.flags.add(MavenFlag.BATCH_MODE);

        BuildContext.getInstance(project).applyTo(request);

        return request;
    }

    private static final class RefreshTask
        extends Task.Backgroundable
    {
        private final String workingDirectory;

        private final ModelRefresher refresher;

        private boolean cancelled;

        RefreshTask(Project project, String workingDirectory, ModelRefresher refresher)
        {
            super(project, MavenPrimeBundle.message("mavenprime.model.refresh.title"), true);

            this.workingDirectory = workingDirectory;
            this.refresher = refresher;
        }

        @Override
        public void onCancel()
        {
            cancelled = true;
        }

        @Override
        public void onFinished()
        {
            refresher.finished(cancelled);
        }

        @Override
        public void run(ProgressIndicator indicator)
        {
            Project project = getProject();

            SpySession session =
                SpySession.untimed(project, MavenPrimeBundle.message("mavenprime.model.refresh.build"));

            try
            {
                run(project, indicator, session.handshake());
            }
            catch (ExecutionException failure)
            {
                MavenPrimeNotifications.error(
                    project,
                    MavenPrimeBundle.message("mavenprime.model.refresh.failed", failure.getMessage()));
            }
            finally
            {
                session.finish();
            }
        }

        private void run(Project project, ProgressIndicator indicator, SpyHandshake handshake)
            throws ExecutionException
        {
            MavenPrimeRequest request = requestFor(project, workingDirectory);

            MavenInstallation installation =
                MavenInstallationService.getInstance(project).resolve(request.distribution, workingDirectory);

            if (!installation.isValid())
            {
                throw new ExecutionException(installation.getError());
            }

            ProcessOutput output =
                new CapturingProcessHandler(
                    MavenCommandLineBuilder.build(project, request, installation, handshake))
                    .runProcessWithProgressIndicator(indicator);

            if ((output.getExitCode() != 0) && !output.isCancelled())
            {
                MavenPrimeNotifications.warning(
                    project,
                    MavenPrimeBundle.message(
                        "mavenprime.model.refresh.incomplete",
                        StringUtils.defaultIfBlank(
                            lastLineOf(output.getStderr()), String.valueOf(output.getExitCode()))));
            }
        }
    }

    private static String lastLineOf(String output)
    {
        String trimmed = StringUtils.trimToEmpty(output);

        return StringUtils.contains(trimmed, '\n')
            ? StringUtils.substringAfterLast(trimmed, '\n')
            : trimmed;
    }
}
