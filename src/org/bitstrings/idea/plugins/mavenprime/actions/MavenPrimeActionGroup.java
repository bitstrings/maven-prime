package org.bitstrings.idea.plugins.mavenprime.actions;

import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

public final class MavenPrimeActionGroup
    extends DefaultActionGroup
    implements DumbAware
{
    @Override
    public void update(AnActionEvent event)
    {
        Project project = event.getProject();

        event
            .getPresentation()
            .setEnabledAndVisible((project != null) && MavenProjects.isPossiblyMavenized(project));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }
}
