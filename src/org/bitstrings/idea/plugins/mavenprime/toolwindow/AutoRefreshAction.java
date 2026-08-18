package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.ui.ProjectToggleAction;

import com.intellij.openapi.project.Project;

public final class AutoRefreshAction
    extends ProjectToggleAction
{
    @Override
    protected boolean isSelectedIn(Project project)
    {
        return MavenPrimeSettings.getInstance(project).autoRefresh;
    }

    @Override
    protected void setSelectedIn(Project project, boolean selected)
    {
        MavenPrimeSettings.getInstance(project).autoRefresh = selected;
    }
}
