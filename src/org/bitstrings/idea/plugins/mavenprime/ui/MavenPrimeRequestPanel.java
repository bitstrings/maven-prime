package org.bitstrings.idea.plugins.mavenprime.ui;

import static com.intellij.ui.dsl.builder.TextFieldKt.COLUMNS_SHORT;
import static com.intellij.ui.dsl.builder.TextFieldKt.COLUMNS_TINY;

import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.FailureBehavior;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.execution.OutputLevel;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

public final class MavenPrimeRequestPanel
{
    private static final int FLAG_COLUMNS = 3;

    private static final int FLAG_GAP = 8;

    private final Project project;

    private final boolean withWorkingDirectory;

    private final TextFieldWithAutoCompletion<String> goalsField;

    private final TextFieldWithBrowseButton workingDirectoryField = new TextFieldWithBrowseButton();

    private final JBTextField settingsFileText = new JBTextField(COLUMNS_SHORT);

    private final TextFieldWithBrowseButton settingsFileField = new TextFieldWithBrowseButton(settingsFileText);

    private final DistributionChooser distributionChooser;

    private final ProfileListPanel profilesPanel = new ProfileListPanel();

    private final PropertiesTableModel propertiesModel = new PropertiesTableModel();

    final TableView<PropertyEntry> propertiesTable = new TableView<>(propertiesModel);

    private final Map<MavenFlag, JBCheckBox> flagBoxes = new EnumMap<>(MavenFlag.class);

    private final ComboBox<FailureBehavior> failureCombo = new ComboBox<>(FailureBehavior.values());

    private final ComboBox<OutputLevel> outputCombo = new ComboBox<>(OutputLevel.values());

    private final JBTextField threadsField = new JBTextField(COLUMNS_TINY);

    private final TextFieldWithAutoCompletion<String> projectsField;

    private final JBTextField optionsField = new JBTextField(COLUMNS_SHORT);

    private final JBTextField vmOptionsField = new JBTextField(COLUMNS_SHORT);

    private final JreChooser jreChooser = new JreChooser(true);

    private final JPanel root;

    public MavenPrimeRequestPanel(Project project, boolean withWorkingDirectory)
    {
        this.project = project;
        this.withWorkingDirectory = withWorkingDirectory;
        this.distributionChooser = new DistributionChooser(project, true);

        goalsField = GoalsTextField.create(project);
        projectsField = ProjectsTextField.create(project);

        root = buildRoot();
    }

    public JComponent getComponent()
    {
        return root;
    }

    public JComponent getPreferredFocusedComponent()
    {
        return goalsField;
    }

    public void resetFrom(MavenPrimeRequest request, Collection<String> availableProfiles)
    {
        goalsField.setText(request.getGoalLine());
        workingDirectoryField.setText(StringUtils.defaultString(request.workingDirectory));

        settingsFileField.setText(StringUtils.defaultString(request.settingsFile));

        distributionChooser.reset(request.distribution, request.workingDirectory);

        profilesPanel.setProfiles(availableProfiles, request.profiles);
        propertiesModel.setProperties(request.properties);

        for (Map.Entry<MavenFlag, JBCheckBox> flagBox : flagBoxes.entrySet())
        {
            flagBox.getValue().setSelected(request.flags.contains(flagBox.getKey()));
        }

        failureCombo.setSelectedItem(request.failureBehavior);
        outputCombo.setSelectedItem(request.outputLevel);
        threadsField.setText(StringUtils.defaultString(request.threads));
        projectsField.setText(String.join(",", request.projects));
        optionsField.setText(StringUtils.defaultString(request.rawOptions));
        vmOptionsField.setText(StringUtils.defaultString(request.vmOptions));

        jreChooser.reset(request.jreName);
    }

    public boolean hasGoals()
    {
        return StringUtils.isNotBlank(goalsField.getText());
    }

    public void setWorkingDirectory(String directory)
    {
        distributionChooser.setWorkingDirectory(directory);
    }

    public void applyTo(MavenPrimeRequest request)
    {
        request.setGoalLine(goalsField.getText());

        if (withWorkingDirectory)
        {
            request.workingDirectory = StringUtils.trimToNull(workingDirectoryField.getText());
        }

        request.distribution = distributionChooser.getSelected();
        request.settingsFile = StringUtils.trimToNull(settingsFileField.getText());

        request.profiles.clear();
        request.profiles.putAll(profilesPanel.getSelectedProfiles());

        request.properties.clear();
        request.properties.putAll(propertiesModel.getProperties());

        request.flags.clear();

        for (Map.Entry<MavenFlag, JBCheckBox> flagBox : flagBoxes.entrySet())
        {
            if (flagBox.getValue().isSelected())
            {
                request.flags.add(flagBox.getKey());
            }
        }

        request.failureBehavior = (FailureBehavior) failureCombo.getSelectedItem();
        request.outputLevel = (OutputLevel) outputCombo.getSelectedItem();
        request.threads = StringUtils.trimToNull(threadsField.getText());

        request.projects.clear();
        request.projects.addAll(splitProjects(projectsField.getText()));

        request.rawOptions = StringUtils.trimToNull(optionsField.getText());
        request.vmOptions = StringUtils.trimToNull(vmOptionsField.getText());
        request.jreName = jreChooser.getSelected();
    }

    private static List<String> splitProjects(String text)
    {
        return Arrays
            .stream(StringUtils.split(StringUtils.defaultString(text), ','))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .toList();
    }

    private JPanel buildRoot()
    {
        configurePropertiesTable();
        configureWorkingDirectoryField();
        configureSettingsFileField();

        optionsField.setToolTipText(MavenPrimeBundle.message("mavenprime.field.options.hint"));

        FormBuilder builder =
            FieldForm
                .create()
                .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.goals"), goalsField);

        if (withWorkingDirectory)
        {
            builder.addLabeledComponent(
                MavenPrimeBundle.message("mavenprime.field.workingDirectory"), workingDirectoryField);
        }

        return builder
            .addLabeledComponent(
                MavenPrimeBundle.message("mavenprime.field.distribution"), distributionChooser.getComponent())
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.settingsFile"), settingsFileField)
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.threads"), threadsField)
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.projects"), projectsField)
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.options"), optionsField)
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.jre"), jreChooser.getComponent())
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.vmOptions"), vmOptionsField)
            .addComponentFillVertically(buildTabs(), JBUI.scale(8))
            .getPanel();
    }

    private JComponent buildTabs()
    {
        JBTabbedPane tabs = new JBTabbedPane();

        tabs.addTab(MavenPrimeBundle.message("mavenprime.tab.profiles"), profilesPanel);
        tabs.addTab(
            MavenPrimeBundle.message("mavenprime.tab.properties"),
            ToolbarDecorator
                .createDecorator(propertiesTable)
                .setAddAction(button -> addProperty())
                .setEditAction(button -> editSelectedProperty())
                .setRemoveAction(button -> removeSelectedProperties())
                .setEditActionUpdater(event -> propertiesTable.getSelectedObject() != null)
                .disableUpDownActions()
                .createPanel());
        tabs.addTab(
            MavenPrimeBundle.message("mavenprime.tab.flags"),
            ScrollPaneFactory.createScrollPane(buildFlagsPanel(), true));

        return tabs;
    }

    private void addProperty()
    {
        BuildPropertyDialog dialog =
            BuildPropertyDialog.forGoal(project, new PropertyEntry(StringUtils.EMPTY, StringUtils.EMPTY));

        if (dialog.showAndGet())
        {
            propertiesModel.addProperty(dialog.getKey(), dialog.getValue());

            int row = propertiesModel.getRowCount() - 1;

            propertiesTable.getSelectionModel().setSelectionInterval(row, row);
            propertiesTable.scrollRectToVisible(propertiesTable.getCellRect(row, 0, true));
        }
    }

    boolean editSelectedProperty()
    {
        PropertyEntry selected = propertiesTable.getSelectedObject();

        if (selected == null)
        {
            return false;
        }

        BuildPropertyDialog dialog = BuildPropertyDialog.forGoal(project, selected);

        if (!dialog.showAndGet())
        {
            return false;
        }

        return applyPropertyEdit(propertiesTable.getSelectedRow(), dialog.getKey(), dialog.getValue());
    }

    boolean applyPropertyEdit(int row, String key, String value)
    {
        if ((row < 0) || (row >= propertiesModel.getRowCount()))
        {
            return false;
        }

        PropertyEntry entry = propertiesModel.getItem(row);

        entry.setKey(key);
        entry.setValue(value);

        propertiesModel.fireTableRowsUpdated(row, row);

        return true;
    }

    private void removeSelectedProperties()
    {
        int[] rows = propertiesTable.getSelectedRows();

        for (int index = rows.length - 1; index >= 0; index--)
        {
            propertiesModel.removeProperty(rows[index]);
        }
    }

    private JComponent buildFlagsPanel()
    {
        JPanel flagsGrid = new JPanel(new GridLayout(0, FLAG_COLUMNS, JBUI.scale(FLAG_GAP), 0));

        for (MavenFlag flag : MavenFlag.values())
        {
            JBCheckBox flagBox = new JBCheckBox(flag.getTitle());

            flagBox.setToolTipText(flag.getDescription());
            flagBoxes.put(flag, flagBox);
            flagsGrid.add(flagBox);
        }

        JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));

        panel.add(
            FieldForm
                .create()
                .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.failureBehavior"), failureCombo)
                .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.outputLevel"), outputCombo)
                .getPanel());
        panel.add(flagsGrid);

        return panel;
    }

    private void configurePropertiesTable()
    {
        propertiesTable.setShowGrid(false);
        propertiesTable.setTableHeader(null);
        propertiesTable.getEmptyText().setText(MavenPrimeBundle.message("mavenprime.properties.empty"));

        new DoubleClickListener()
        {
            @Override
            protected boolean onDoubleClick(MouseEvent event)
            {
                return editSelectedProperty();
            }
        }.installOn(propertiesTable);
    }

    private void configureWorkingDirectoryField()
    {
        workingDirectoryField.addBrowseFolderListener(
            project, FileChooserDescriptorFactory.createSingleFolderDescriptor());

        workingDirectoryField
            .getTextField()
            .addFocusListener(
                new FocusAdapter()
                {
                    @Override
                    public void focusLost(FocusEvent event)
                    {
                        setWorkingDirectory(StringUtils.trimToNull(workingDirectoryField.getText()));
                    }
                });
    }

    private void configureSettingsFileField()
    {
        settingsFileText.getEmptyText().setText(MavenPrimeBundle.message("mavenprime.field.settingsFile.empty"));

        settingsFileField.addBrowseFolderListener(
            project, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
    }
}
