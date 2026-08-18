package org.bitstrings.idea.plugins.mavenprime.ui;

import static com.intellij.ui.dsl.builder.TextFieldKt.COLUMNS_SHORT;

import javax.swing.JComponent;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

public final class BuildEnvironmentDialog
    extends DialogWrapper
{
    private final DistributionChooser distributionChooser;

    private final JreChooser jreChooser = new JreChooser(false);

    private final JBTextField settingsFileText = new JBTextField(COLUMNS_SHORT);

    private final TextFieldWithBrowseButton settingsFileField =
        new TextFieldWithBrowseButton(settingsFileText);

    private final JBTextField vmOptionsField = new JBTextField(COLUMNS_SHORT);

    public BuildEnvironmentDialog(Project project, BuildContextEnvironment environment)
    {
        super(project);

        this.distributionChooser = new DistributionChooser(project, false);

        setTitle(MavenPrimeBundle.message("mavenprime.context.environment.title"));

        init();

        settingsFileText.getEmptyText().setText(MavenPrimeBundle.message("mavenprime.field.settingsFile.empty"));
        settingsFileField.addBrowseFolderListener(
            project, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());

        distributionChooser.reset(environment.getDistribution(), null);
        settingsFileField.setText(StringUtils.defaultString(environment.settingsFile));
        jreChooser.reset(environment.jreName);
        vmOptionsField.setText(StringUtils.defaultString(environment.vmOptions));
    }

    @Override
    protected JComponent createCenterPanel()
    {
        return FieldForm
            .create()
            .addLabeledComponent(
                MavenPrimeBundle.message("mavenprime.field.distribution"), distributionChooser.getComponent())
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.settingsFile"), settingsFileField)
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.jre"), jreChooser.getComponent())
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.field.vmOptions"), vmOptionsField)
            .addComponent(HintLabel.forKey("mavenprime.context.environment.hint"))
            .getPanel();
    }

    public BuildContextEnvironment getEnvironment()
    {
        BuildContextEnvironment environment = new BuildContextEnvironment();

        environment.distribution = distributionChooser.getSelected();
        environment.settingsFile = settingsFileField.getText().trim();
        environment.jreName = StringUtils.defaultString(jreChooser.getSelected());
        environment.vmOptions = vmOptionsField.getText().trim();

        return environment;
    }
}
