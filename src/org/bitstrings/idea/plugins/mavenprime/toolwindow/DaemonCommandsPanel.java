package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.daemon.DaemonCommand;
import org.bitstrings.idea.plugins.mavenprime.daemon.DaemonCommandRunner;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

public final class DaemonCommandsPanel
    extends JPanel
{
    private static final long serialVersionUID = 1L;

    private final transient Project project;

    private final transient Supplier<DistributionSpec> target;

    final JBTextArea output = new JBTextArea();

    public DaemonCommandsPanel(Project project, Supplier<DistributionSpec> target)
    {
        super(new BorderLayout());

        this.project = project;
        this.target = target;

        output.setEditable(false);
        output.setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
        output.setText(MavenPrimeBundle.message("mavenprime.daemon.hint"));

        add(buildToolbar(), BorderLayout.NORTH);
        add(new JBScrollPane(output), BorderLayout.CENTER);
    }

    private JPanel buildToolbar()
    {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        for (DaemonCommand command : DaemonCommand.values())
        {
            toolbar.add(button(command));
        }

        return toolbar;
    }

    private JButton button(DaemonCommand command)
    {
        JButton button = new JButton(command.getTitle());

        button.addActionListener(event -> run(command));

        return button;
    }

    private void run(DaemonCommand command)
    {
        DistributionSpec selected = target.get();

        if (selected == null)
        {
            output.setText(MavenPrimeBundle.message("mavenprime.daemon.noInstallation"));

            return;
        }

        output.setText(MavenPrimeBundle.message("mavenprime.daemon.running", command.getTitle()));

        DaemonCommandRunner.runInBackground(project, selected, command, output::setText);
    }
}
