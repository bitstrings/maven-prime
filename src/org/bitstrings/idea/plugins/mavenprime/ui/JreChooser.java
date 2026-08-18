package org.bitstrings.idea.plugins.mavenprime.ui;

import static com.intellij.ui.dsl.builder.TextFieldKt.COLUMNS_SHORT;

import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JList;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;

import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleListCellRenderer;

public final class JreChooser
{
    private static final String CONTEXT = StringUtils.EMPTY;

    private final ComboBox<String> combo = new ComboBox<>();

    private final boolean withContext;

    public JreChooser(boolean withContext)
    {
        this.withContext = withContext;

        combo.setMinimumAndPreferredWidth(
            combo.getFontMetrics(combo.getFont()).charWidth('m') * COLUMNS_SHORT);

        if (withContext)
        {
            combo.addItem(CONTEXT);
            combo.setRenderer(new JreRenderer());
        }

        combo.addItem(MavenRunnerSettings.USE_PROJECT_JDK);
        combo.addItem(MavenRunnerSettings.USE_INTERNAL_JAVA);
        combo.addItem(MavenRunnerSettings.USE_JAVA_HOME);

        for (Sdk jdk : ProjectJdkTable.getInstance().getAllJdks())
        {
            combo.addItem(jdk.getName());
        }
    }

    public JComponent getComponent()
    {
        return combo;
    }

    public String getSelected()
    {
        return StringUtils.trimToNull((String) combo.getSelectedItem());
    }

    public void reset(String jreName)
    {
        String selected =
            withContext
                ? StringUtils.defaultString(jreName)
                : StringUtils.defaultIfBlank(jreName, MavenRunnerSettings.USE_PROJECT_JDK);

        for (int index = 0; index < combo.getItemCount(); index++)
        {
            if (Objects.equals(combo.getItemAt(index), selected))
            {
                combo.setSelectedIndex(index);

                return;
            }
        }

        combo.addItem(selected);
        combo.setSelectedItem(selected);
    }

    private static final class JreRenderer
        extends SimpleListCellRenderer<String>
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void customize(
            JList<? extends String> list, String value, int index, boolean selected, boolean hasFocus)
        {
            setText(StringUtils.isEmpty(value) ? MavenPrimeBundle.message("mavenprime.jre.context") : value);
        }
    }
}
