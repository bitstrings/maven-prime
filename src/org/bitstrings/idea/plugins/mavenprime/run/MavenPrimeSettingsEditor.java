package org.bitstrings.idea.plugins.mavenprime.run;

import javax.swing.JComponent;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.ui.MavenPrimeRequestPanel;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;

public final class MavenPrimeSettingsEditor
    extends SettingsEditor<MavenPrimeRunConfiguration>
{
    private final Project project;

    private final MavenPrimeRequestPanel panel;

    MavenPrimeSettingsEditor(Project project)
    {
        this.project = project;
        this.panel = new MavenPrimeRequestPanel(project, true);
    }

    @Override
    protected void resetEditorFrom(MavenPrimeRunConfiguration configuration)
    {
        panel.resetFrom(configuration.getRequest(), MavenProjects.allProfiles(project));
    }

    @Override
    protected void applyEditorTo(MavenPrimeRunConfiguration configuration)
    {
        MavenPrimeRequest request = configuration.getRequest().copy();

        panel.applyTo(request);

        configuration.setRequest(request);
    }

    @Override
    protected JComponent createEditor()
    {
        return panel.getComponent();
    }
}
