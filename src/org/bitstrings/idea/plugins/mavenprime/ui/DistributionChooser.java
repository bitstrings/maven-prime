package org.bitstrings.idea.plugins.mavenprime.ui;

import static com.intellij.ui.dsl.builder.TextFieldKt.COLUMNS_SHORT;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.distribution.DaemonInstallations;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;
import org.bitstrings.idea.plugins.mavenprime.distribution.MvndLayout;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;

public final class DistributionChooser
{
    private static final String LABEL_SEPARATOR = " - ";

    private final Project project;

    private final boolean withContext;

    private final ComboBox<DistributionItem> combo = new ComboBox<>();

    private final JPanel component = new JPanel(new BorderLayout(JBUI.scale(4), 0));

    private String workingDirectory;

    private int detectionGeneration;

    public DistributionChooser(Project project, boolean withContext)
    {
        this.project = project;
        this.withContext = withContext;

        combo.setMinimumAndPreferredWidth(
            combo.getFontMetrics(combo.getFont()).charWidth('m') * COLUMNS_SHORT);

        JButton browseButton = new JButton(MavenPrimeBundle.message("mavenprime.distribution.browse"));

        browseButton.addActionListener(event -> browse());

        component.add(combo, BorderLayout.CENTER);
        component.add(browseButton, BorderLayout.EAST);
    }

    public JComponent getComponent()
    {
        return component;
    }

    public DistributionSpec getSelected()
    {
        DistributionItem selected = combo.getItem();

        return (selected == null) ? fallback() : selected.spec().copy();
    }

    public void reset(DistributionSpec selected, String directory)
    {
        workingDirectory = directory;

        reset(selected);
    }

    public void setWorkingDirectory(String directory)
    {
        if (Objects.equals(workingDirectory, directory))
        {
            return;
        }

        workingDirectory = directory;

        reset(getSelected());
    }

    private void reset(DistributionSpec selected)
    {
        DistributionSpec requested =
            (!withContext && selected.isContext()) ? DistributionSpec.ide() : selected.copy();

        detectionGeneration++;

        int generation = detectionGeneration;

        combo.removeAllItems();
        combo.addItem(new DistributionItem(requested, requested.kind.getTitle()));
        combo.setSelectedIndex(0);

        MavenInstallationService
            .getInstance(project)
            .detectDistributions(
                candidates(requested), workingDirectory, detected -> show(detected, requested, generation));
    }

    private List<DistributionSpec> candidates(DistributionSpec requested)
    {
        List<DistributionSpec> candidates = new ArrayList<>();

        if (!withContext)
        {
            candidates.add(requested);
        }
        else
        {
            if (!requested.isContext())
            {
                candidates.add(requested);
            }

            candidates.add(BuildContext.getInstance(project).getDistribution());
        }

        for (String home : DaemonInstallations.getInstance().getHomes())
        {
            candidates.add(DistributionSpec.daemonHome(home));
        }

        return candidates;
    }

    private void show(
        Map<DistributionSpec, MavenInstallation> detected, DistributionSpec selected, int generation)
    {
        if (generation != detectionGeneration)
        {
            return;
        }

        combo.removeAllItems();

        DistributionItem match = null;

        if (withContext)
        {
            match = contextItem(detected);

            combo.addItem(match);

            if (!selected.isContext())
            {
                match = null;
            }
        }

        for (Map.Entry<DistributionSpec, MavenInstallation> distribution : detected.entrySet())
        {
            DistributionItem item = DistributionItem.of(distribution.getKey(), distribution.getValue());

            combo.addItem(item);

            if (distribution.getKey().equals(selected))
            {
                match = item;
            }
        }

        combo.setSelectedItem(match);
    }

    private DistributionItem contextItem(Map<DistributionSpec, MavenInstallation> detected)
    {
        MavenInstallation resolved = detected.get(BuildContext.getInstance(project).getDistribution());

        String title = MavenPrimeBundle.message("mavenprime.distribution.context");

        return new DistributionItem(
            DistributionSpec.context(),
            (resolved == null) ? title : (title + LABEL_SEPARATOR + resolved.getDisplayName()));
    }

    private DistributionSpec fallback()
    {
        return withContext ? DistributionSpec.context() : DistributionSpec.ide();
    }

    private void browse()
    {
        VirtualFile chosen =
            FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, null);

        if (chosen == null)
        {
            return;
        }

        Path home = Paths.get(chosen.getPath());

        reset(
            MvndLayout.isDaemonHome(home)
                ? DistributionSpec.daemonHome(home.toString())
                : DistributionSpec.mavenHome(home.toString()));
    }
}
