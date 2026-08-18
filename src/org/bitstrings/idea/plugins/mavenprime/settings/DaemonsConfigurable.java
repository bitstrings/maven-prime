package org.bitstrings.idea.plugins.mavenprime.settings;

import javax.swing.JComponent;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.DaemonsPanel;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

public final class DaemonsConfigurable
    implements Configurable
{
    private final Project project;

    private DaemonsPanel panel;

    public DaemonsConfigurable(Project project)
    {
        this.project = project;
    }

    @Override
    public String getDisplayName()
    {
        return MavenPrimeBundle.message("mavenprime.settings.daemons");
    }

    @Override
    public JComponent createComponent()
    {
        panel = new DaemonsPanel(project);

        return panel;
    }

    @Override
    public boolean isModified()
    {
        return (panel != null) && panel.isModified();
    }

    @Override
    public void apply()
    {
        if (panel != null)
        {
            panel.apply();
        }
    }

    @Override
    public void reset()
    {
        if (panel != null)
        {
            panel.reset();
        }
    }

    @Override
    public void disposeUIResources()
    {
        panel = null;
    }
}
