package org.bitstrings.idea.plugins.mavenprime.ui;

import static com.intellij.ui.dsl.builder.TextFieldKt.COLUMNS_SHORT;

import javax.swing.JComponent;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalScope;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

public final class GoalEditorDialog
    extends DialogWrapper
{
    private final JBTextField nameField = new JBTextField(COLUMNS_SHORT);

    private final ComboBox<GoalScope> scopeCombo = new ComboBox<>(GoalScope.values());

    private final MavenPrimeRequestPanel requestPanel;

    private final GoalDefinition definition;

    public GoalEditorDialog(Project project, GoalDefinition definition, GoalScope scope)
    {
        super(project);

        this.definition = definition.copy();
        this.requestPanel = new MavenPrimeRequestPanel(project, false);

        setTitle(MavenPrimeBundle.message("mavenprime.goalEditor.title"));

        init();

        nameField.setText(StringUtils.defaultString(this.definition.name));
        scopeCombo.setSelectedItem(scope);
        requestPanel.resetFrom(this.definition.template, MavenProjects.allProfiles(project));
    }

    public GoalDefinition getDefinition()
    {
        definition.name = StringUtils.trimToNull(nameField.getText());

        requestPanel.applyTo(definition.template);

        return definition;
    }

    public GoalScope getScope()
    {
        return (GoalScope) scopeCombo.getSelectedItem();
    }

    @Override
    protected JComponent createCenterPanel()
    {
        return FieldForm
            .create()
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.goalEditor.name"), nameField)
            .addLabeledComponent(MavenPrimeBundle.message("mavenprime.goalEditor.scope"), scopeCombo)
            .addComponentFillVertically(requestPanel.getComponent(), 0)
            .getPanel();
    }

    @Override
    public JComponent getPreferredFocusedComponent()
    {
        return nameField;
    }

    @Override
    protected ValidationInfo doValidate()
    {
        return requestPanel.hasGoals()
            ? null
            : new ValidationInfo(
                MavenPrimeBundle.message("mavenprime.goalEditor.error.noGoals"),
                requestPanel.getPreferredFocusedComponent());
    }
}
