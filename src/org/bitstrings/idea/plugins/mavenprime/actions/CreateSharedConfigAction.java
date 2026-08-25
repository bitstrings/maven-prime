package org.bitstrings.idea.plugins.mavenprime.actions;

import org.bitstrings.idea.plugins.mavenprime.config.MavenPrimeConfigService;
import org.bitstrings.idea.plugins.mavenprime.config.SharedConfigTemplate;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

public final class CreateSharedConfigAction
    extends AnAction
    implements DumbAware
{
    @Override
    public void update(AnActionEvent event)
    {
        Project project = event.getProject();
        Presentation presentation = event.getPresentation();

        presentation.setVisible(project != null);
        presentation.setEnabled(
            (project != null) && (MavenPrimeConfigService.getInstance(project).findConfigFile() == null));
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        Project project = event.getProject();

        if (project == null)
        {
            return;
        }

        MavenPrimeConfigService.getInstance(project).createSharedFile(SharedConfigTemplate.of(project));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }
}
