package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileAdvice;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class BuildProfileNotifier
    implements BuildProfileService.BuildFinishedListener
{
    private final Project project;

    private Notification current;

    public BuildProfileNotifier(Project project)
    {
        this.project = project;
    }

    public static BuildProfileNotifier getInstance(Project project)
    {
        return project.getService(BuildProfileNotifier.class);
    }

    public void listen()
    {
        project.getMessageBus().connect(project).subscribe(BuildProfileService.FINISHED_TOPIC, this);
    }

    @Override
    public void buildFinished()
    {
        if (!MavenPrimeSettings.getInstance(project).profileNotifications)
        {
            return;
        }

        BuildProfile profile = BuildProfileService.getInstance(project).getCurrent();

        if (!profile.hasModuleTimings() || profile.hasFailures() || !profile.getCompleteness().isComplete())
        {
            return;
        }

        BuildProfileSummary summary = profile.summarize();

        if (!ProfileAdvice.isWorthReporting(summary))
        {
            return;
        }

        if (current != null)
        {
            current.expire();
        }

        current =
            MavenPrimeNotifications.info(
                project,
                ProfileVerdict.sentenceOf(summary),
                NotificationAction.createSimpleExpiring(
                    MavenPrimeBundle.message("mavenprime.profile.notification.open"),
                    () -> BuildProfileEditor.getInstance(project).open()));
    }
}
