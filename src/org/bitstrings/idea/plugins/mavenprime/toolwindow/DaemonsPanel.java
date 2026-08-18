package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;
import org.bitstrings.idea.plugins.mavenprime.distribution.DaemonInstallations;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;
import org.bitstrings.idea.plugins.mavenprime.distribution.MvndDownload;
import org.bitstrings.idea.plugins.mavenprime.distribution.MvndDownloadTask;
import org.bitstrings.idea.plugins.mavenprime.distribution.MvndLayout;
import org.bitstrings.idea.plugins.mavenprime.distribution.MvndPlatform;
import org.bitstrings.idea.plugins.mavenprime.distribution.MvndReleases;
import org.bitstrings.idea.plugins.mavenprime.ui.DistributionItem;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;

public final class DaemonsPanel
    extends JPanel
{
    private static final long serialVersionUID = 1L;

    private static final int COMBO_WIDTH = 320;

    private final transient Project project;

    final ComboBox<DistributionItem> installationCombo = new ComboBox<>();

    final JBCheckBox useForBuildsBox =
        new JBCheckBox(MavenPrimeBundle.message("mavenprime.daemon.useForBuilds"));

    private final transient List<String> pendingHomes = new ArrayList<>();

    private transient String pendingBuildDaemon;

    public DaemonsPanel(Project project)
    {
        super(new BorderLayout());

        this.project = project;

        installationCombo.setMinimumAndPreferredWidth(JBUI.scale(COMBO_WIDTH));
        installationCombo.addActionListener(event -> showPendingChoice());

        useForBuildsBox.addActionListener(event -> rememberPendingChoice());

        add(buildToolbar(), BorderLayout.NORTH);
        add(new DaemonCommandsPanel(project, this::selectedSpec), BorderLayout.CENTER);

        reset();
    }

    public boolean isModified()
    {
        return !pendingHomes.equals(DaemonInstallations.getInstance().getHomes())
            || !Objects.equals(pendingBuildDaemon, configuredBuildDaemon());
    }

    public void apply()
    {
        DaemonInstallations.getInstance().setHomes(pendingHomes);

        if (Objects.equals(pendingBuildDaemon, configuredBuildDaemon()))
        {
            return;
        }

        BuildContext context = BuildContext.getInstance(project);

        BuildContextEnvironment environment = context.getEnvironment();

        environment.distribution =
            (pendingBuildDaemon == null)
                ? DistributionSpec.ide()
                : DistributionSpec.daemonHome(pendingBuildDaemon);

        context.setEnvironment(environment);
    }

    public void reset()
    {
        pendingHomes.clear();
        pendingHomes.addAll(DaemonInstallations.getInstance().getHomes());
        pendingBuildDaemon = configuredBuildDaemon();

        reloadInstallations();
    }

    private String configuredBuildDaemon()
    {
        DistributionSpec configured = BuildContext.getInstance(project).getDistribution();

        return configured.isDaemon() ? configured.path : null;
    }

    private void reloadInstallations()
    {
        DistributionItem selected = installationCombo.getItem();

        MavenInstallationService
            .getInstance(project)
            .detectDistributions(pendingSpecs(), null, detected -> showInstallations(detected, selected));
    }

    private List<DistributionSpec> pendingSpecs()
    {
        List<DistributionSpec> specs = new ArrayList<>();

        for (String home : pendingHomes)
        {
            specs.add(DistributionSpec.daemonHome(home));
        }

        return specs;
    }

    private void getInstallation()
    {
        List<String> versions = availableVersions();

        String version =
            Messages.showEditableChooseDialog(
                MavenPrimeBundle.message("mavenprime.daemon.get.prompt", MvndPlatform.current().getClassifier()),
                MavenPrimeBundle.message("mavenprime.daemon.get.title"),
                null,
                versions.toArray(new String[0]),
                MvndReleases.newestStable(versions),
                null);

        if (StringUtils.isBlank(version))
        {
            return;
        }

        MvndDownloadTask.start(project, MvndDownload.of(version.trim()), this::installed);
    }

    private List<String> availableVersions()
    {
        AtomicReference<List<String>> versions = new AtomicReference<>(List.of());

        ProgressManager
            .getInstance()
            .runProcessWithProgressSynchronously(
                () -> versions.set(MvndReleases.available()),
                MavenPrimeBundle.message("mavenprime.daemon.get.listing"),
                true,
                project);

        return versions.get().isEmpty() ? List.of(MvndDownload.DEFAULT_VERSION) : versions.get();
    }

    private void installed(Path home)
    {
        if (!pendingHomes.contains(home.toString()))
        {
            pendingHomes.add(home.toString());
        }

        pendingBuildDaemon = home.toString();

        reloadInstallations();
    }

    private void addInstallation()
    {
        VirtualFile chosen =
            FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, null);

        if (chosen == null)
        {
            return;
        }

        if (!MvndLayout.isDaemonHome(Paths.get(chosen.getPath())))
        {
            MavenPrimeNotifications.warning(
                project,
                MavenPrimeBundle.message("mavenprime.daemon.notADistribution", chosen.getPresentableUrl()));

            return;
        }

        if (!pendingHomes.contains(chosen.getPath()))
        {
            pendingHomes.add(chosen.getPath());
        }

        reloadInstallations();
    }

    private void removeInstallation()
    {
        DistributionSpec selected = selectedSpec();

        if (selected == null)
        {
            return;
        }

        if (!pendingHomes.remove(selected.path))
        {
            MavenPrimeNotifications.info(
                project, MavenPrimeBundle.message("mavenprime.daemon.notRemovable", selected.path));

            return;
        }

        if (selected.path.equals(pendingBuildDaemon))
        {
            pendingBuildDaemon = null;
        }

        reloadInstallations();
    }

    private void showInstallations(Map<DistributionSpec, MavenInstallation> detected, DistributionItem selected)
    {
        installationCombo.removeAllItems();

        for (Map.Entry<DistributionSpec, MavenInstallation> distribution : detected.entrySet())
        {
            if (distribution.getKey().isDaemon())
            {
                installationCombo.addItem(
                    DistributionItem.of(distribution.getKey(), distribution.getValue()));
            }
        }

        select((selected == null) ? pendingBuildDaemon : selected.spec().path);

        showPendingChoice();
    }

    private void select(String path)
    {
        for (int index = 0; (path != null) && (index < installationCombo.getItemCount()); index++)
        {
            if (path.equals(installationCombo.getItemAt(index).spec().path))
            {
                installationCombo.setSelectedIndex(index);

                return;
            }
        }
    }

    private void showPendingChoice()
    {
        DistributionSpec selected = selectedSpec();

        useForBuildsBox.setEnabled(selected != null);
        useForBuildsBox.setSelected((selected != null) && selected.path.equals(pendingBuildDaemon));
    }

    private void rememberPendingChoice()
    {
        DistributionSpec selected = selectedSpec();

        pendingBuildDaemon = ((selected != null) && useForBuildsBox.isSelected()) ? selected.path : null;

        showPendingChoice();
    }

    private DistributionSpec selectedSpec()
    {
        DistributionItem selected = installationCombo.getItem();

        return (selected == null) ? null : selected.spec();
    }

    private JPanel buildToolbar()
    {
        JPanel toolbar = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, JBUI.scale(2), true, false));

        toolbar.add(row(installationCombo));
        toolbar.add(
            row(
                button(MavenPrimeBundle.message("mavenprime.daemon.add"), this::addInstallation),
                button(MavenPrimeBundle.message("mavenprime.daemon.remove"), this::removeInstallation),
                button(MavenPrimeBundle.message("mavenprime.daemon.get"), this::getInstallation)));
        toolbar.add(row(useForBuildsBox));

        return toolbar;
    }

    private static JPanel row(JComponent... components)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));

        for (JComponent component : components)
        {
            row.add(component);
        }

        return row;
    }

    private static JButton button(String title, Runnable command)
    {
        JButton button = new JButton(title);

        button.addActionListener(event -> command.run());

        return button;
    }
}
