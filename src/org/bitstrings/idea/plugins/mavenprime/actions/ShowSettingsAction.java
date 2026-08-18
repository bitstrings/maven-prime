package org.bitstrings.idea.plugins.mavenprime.actions;

import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeConfigurable;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

public final class ShowSettingsAction
    extends AnAction
    implements DumbAware
{
    @Override
    public void update(AnActionEvent event)
    {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        Project project = event.getProject();

        if (project == null)
        {
            return;
        }

        ShowSettingsUtil.getInstance().showSettingsDialog(project, MavenPrimeConfigurable.class);
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }
}
