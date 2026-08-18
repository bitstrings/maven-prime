package org.bitstrings.idea.plugins.mavenprime.ui;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

public abstract class ProjectToggleAction
    extends ToggleAction
    implements DumbAware
{
    @Override
    public final boolean isSelected(AnActionEvent event)
    {
        Project project = event.getProject();

        return (project != null) && isSelectedIn(project);
    }

    @Override
    public final void setSelected(AnActionEvent event, boolean selected)
    {
        Project project = event.getProject();

        if (project != null)
        {
            setSelectedIn(project, selected);
        }
    }

    @Override
    public void update(AnActionEvent event)
    {
        super.update(event);

        event.getPresentation().setEnabled(event.getProject() != null);
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }

    protected abstract boolean isSelectedIn(Project project);

    protected abstract void setSelectedIn(Project project, boolean selected);
}
