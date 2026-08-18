package org.bitstrings.idea.plugins.mavenprime.ui;

import static com.intellij.ui.dsl.builder.TextFieldKt.COLUMNS_SHORT;

import javax.swing.JComponent;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

public final class RepositoryCredentialsDialog
    extends DialogWrapper
{
    private final JBTextField usernameField = new JBTextField(COLUMNS_SHORT);

    private final JBPasswordField passwordField = new JBPasswordField();

    public RepositoryCredentialsDialog(Project project, String serverId)
    {
        super(project);

        setTitle(MavenPrimeBundle.message("mavenprime.repository.credentials.title", serverId));

        init();
    }

    @Override
    protected JComponent createCenterPanel()
    {
        return FieldForm
            .create()
            .addLabeledComponent(
                MavenPrimeBundle.message("mavenprime.repository.credentials.username"), usernameField)
            .addLabeledComponent(
                MavenPrimeBundle.message("mavenprime.repository.credentials.password"), passwordField)
            .addComponentToRightColumn(HintLabel.forKey("mavenprime.repository.credentials.hint"))
            .getPanel();
    }

    @Override
    public JComponent getPreferredFocusedComponent()
    {
        return usernameField;
    }

    @Override
    protected ValidationInfo doValidate()
    {
        return StringUtils.isBlank(getUsername())
            ? new ValidationInfo(
                MavenPrimeBundle.message("mavenprime.repository.credentials.usernameRequired"),
                usernameField)
            : null;
    }

    public String getUsername()
    {
        return StringUtils.trimToEmpty(usernameField.getText());
    }

    public String getPassword()
    {
        return new String(passwordField.getPassword());
    }
}
