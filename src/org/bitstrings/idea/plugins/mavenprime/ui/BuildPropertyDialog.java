package org.bitstrings.idea.plugins.mavenprime.ui;

import static com.intellij.ui.dsl.builder.TextFieldKt.COLUMNS_SHORT;

import javax.swing.JComponent;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperty;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

public final class BuildPropertyDialog
    extends DialogWrapper
{
    private final JBTextField keyField = new JBTextField(COLUMNS_SHORT);

    private final JBTextField valueField = new JBTextField(COLUMNS_SHORT);

    private final JBCheckBox importBox =
        new JBCheckBox(MavenPrimeBundle.message("mavenprime.context.appliesToImport"));

    private final boolean importable;

    private BuildPropertyDialog(Project project, String key, String value, Boolean appliesToImport)
    {
        super(project);

        this.importable = appliesToImport != null;

        setTitle(MavenPrimeBundle.message("mavenprime.context.property.title"));

        init();

        keyField.setText(key);
        valueField.setText(value);
        importBox.setSelected(importable && appliesToImport.booleanValue());
        importBox.setToolTipText(MavenPrimeBundle.message("mavenprime.context.appliesToImport.description"));
    }

    public static BuildPropertyDialog forBuildContext(Project project, BuildContextProperty property)
    {
        return new BuildPropertyDialog(
            project, property.key, property.value, Boolean.valueOf(property.appliesToImport));
    }

    public static BuildPropertyDialog forGoal(Project project, PropertyEntry entry)
    {
        return new BuildPropertyDialog(project, entry.getKey(), entry.getValue(), null);
    }

    @Override
    protected JComponent createCenterPanel()
    {
        FormBuilder form =
            FieldForm
                .create()
                .addLabeledComponent(MavenPrimeBundle.message("mavenprime.properties.column.key"), keyField)
                .addLabeledComponent(MavenPrimeBundle.message("mavenprime.properties.column.value"), valueField);

        return (importable ? form.addComponent(importBox) : form).getPanel();
    }

    @Override
    protected ValidationInfo doValidate()
    {
        return StringUtils.isBlank(keyField.getText())
            ? new ValidationInfo(MavenPrimeBundle.message("mavenprime.context.property.error.noKey"), keyField)
            : null;
    }

    @Override
    public JComponent getPreferredFocusedComponent()
    {
        return keyField;
    }

    public String getKey()
    {
        return keyField.getText().trim();
    }

    public String getValue()
    {
        return valueField.getText();
    }

    public boolean appliesToImport()
    {
        return importable && importBox.isSelected();
    }
}
