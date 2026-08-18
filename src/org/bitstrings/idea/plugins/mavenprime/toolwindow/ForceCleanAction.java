package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.ui.ProjectToggleAction;

import com.intellij.openapi.project.Project;

public final class ForceCleanAction
    extends ProjectToggleAction
{
    @Override
    protected boolean isSelectedIn(Project project)
    {
        return BuildContext.getInstance(project).isForceClean();
    }

    @Override
    protected void setSelectedIn(Project project, boolean selected)
    {
        BuildContext.getInstance(project).setForceClean(selected);
    }
}
