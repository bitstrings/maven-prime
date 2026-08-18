package org.bitstrings.idea.plugins.mavenprime.ui;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

public final class GoalPickerDialog
    extends DialogWrapper
{
    private static final int WIDTH = 420;

    private static final int HEIGHT = 260;

    private final JBList<ScopedGoal> goals;

    private GoalPickerDialog(Project project, List<ScopedGoal> available, ScopedGoal selected)
    {
        super(project, false);

        goals = new JBList<>(new CollectionListModel<>(available));

        goals.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        goals.setCellRenderer(new ScopedGoalRenderer());
        goals.setSelectedValue((selected == null) ? available.get(0) : selected, true);

        new DoubleClickListener()
        {
            @Override
            protected boolean onDoubleClick(MouseEvent event)
            {
                if (goals.getSelectedValue() == null)
                {
                    return false;
                }

                doOKAction();

                return true;
            }
        }.installOn(goals);

        setTitle(MavenPrimeBundle.message("mavenprime.beforeRun.picker.title"));

        init();
    }

    public static ScopedGoal pick(Project project, ScopedGoal selected)
    {
        List<ScopedGoal> available = GoalRegistry.getInstance(project).all();

        if (available.isEmpty())
        {
            MavenPrimeNotifications.info(project, MavenPrimeBundle.message("mavenprime.popup.noGoals"));

            return null;
        }

        GoalPickerDialog dialog = new GoalPickerDialog(project, available, selected);

        return dialog.showAndGet() ? dialog.goals.getSelectedValue() : null;
    }

    @Override
    protected JComponent createCenterPanel()
    {
        JBScrollPane scroll = new JBScrollPane(goals);

        scroll.setPreferredSize(JBUI.size(WIDTH, HEIGHT));

        return scroll;
    }

    @Override
    public JComponent getPreferredFocusedComponent()
    {
        return goals;
    }

    @Override
    protected ValidationInfo doValidate()
    {
        return (goals.getSelectedValue() == null)
            ? new ValidationInfo(MavenPrimeBundle.message("mavenprime.beforeRun.picker.error.noGoal"), goals)
            : null;
    }
}
