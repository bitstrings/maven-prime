package org.bitstrings.idea.plugins.mavenprime.settings;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.ui.HintLabel;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;

public final class RepositoryConfigurable
    implements Configurable
{
    private final Project project;

    private JBCheckBox searchEverywhereBox;

    public RepositoryConfigurable(Project project)
    {
        this.project = project;
    }

    @Override
    public String getDisplayName()
    {
        return MavenPrimeBundle.message("mavenprime.settings.repository");
    }

    @Override
    public JComponent createComponent()
    {
        searchEverywhereBox =
            new JBCheckBox(MavenPrimeBundle.message("mavenprime.repository.searchEverywhere"));

        JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));

        panel.setBorder(JBUI.Borders.empty(8));

        panel.add(searchEverywhereBox);
        panel.add(
            HintLabel.under(searchEverywhereBox, "mavenprime.repository.searchEverywhere.description"));

        reset();

        return panel;
    }

    @Override
    public boolean isModified()
    {
        return (searchEverywhereBox != null)
            && (searchEverywhereBox.isSelected() != settings().searchEverywhere);
    }

    @Override
    public void apply()
    {
        if (searchEverywhereBox != null)
        {
            settings().searchEverywhere = searchEverywhereBox.isSelected();
        }
    }

    @Override
    public void reset()
    {
        if (searchEverywhereBox != null)
        {
            searchEverywhereBox.setSelected(settings().searchEverywhere);
        }
    }

    @Override
    public void disposeUIResources()
    {
        searchEverywhereBox = null;
    }

    private MavenPrimeSettings settings()
    {
        return MavenPrimeSettings.getInstance(project);
    }
}
