package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.nio.file.Path;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;
import org.bitstrings.idea.plugins.mavenprime.distribution.DaemonInstallations;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;
import org.bitstrings.idea.plugins.mavenprime.ui.ProjectToggleAction;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public final class UseDaemonAction
    extends ProjectToggleAction
{
    @Override
    protected boolean isSelectedIn(Project project)
    {
        return BuildContext.getInstance(project).getDistribution().isDaemon();
    }

    @Override
    protected void setSelectedIn(Project project, boolean selected)
    {
        DistributionSpec target = selected ? firstDaemon(project) : DistributionSpec.ide();

        if (target == null)
        {
            MavenPrimeNotifications.warning(
                project, MavenPrimeBundle.message("mavenprime.daemon.noneAvailable"));

            return;
        }

        BuildContext context = BuildContext.getInstance(project);

        BuildContextEnvironment environment = context.getEnvironment();

        environment.distribution = target;

        context.setEnvironment(environment);
    }

    @Override
    public void update(AnActionEvent event)
    {
        super.update(event);

        Project project = event.getProject();

        event
            .getPresentation()
            .setEnabled((project != null) && (isSelectedIn(project) || (firstDaemon(project) != null)));
    }

    static DistributionSpec firstDaemon(Project project)
    {
        List<String> configured = DaemonInstallations.getInstance().getHomes();

        if (!configured.isEmpty())
        {
            return DistributionSpec.daemonHome(configured.get(0));
        }

        List<Path> detected = MavenInstallationService.getInstance(project).detectDaemonHomes();

        return detected.isEmpty() ? null : DistributionSpec.daemonHome(detected.get(0).toString());
    }
}
