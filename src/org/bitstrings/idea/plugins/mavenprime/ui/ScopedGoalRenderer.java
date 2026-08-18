package org.bitstrings.idea.plugins.mavenprime.ui;

import javax.swing.Icon;
import javax.swing.JList;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalScope;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

public final class ScopedGoalRenderer
    extends ColoredListCellRenderer<ScopedGoal>
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void customizeCellRenderer(
        JList<? extends ScopedGoal> list, ScopedGoal goal, int index, boolean selected, boolean hasFocus)
    {
        if (goal == null)
        {
            return;
        }

        setIcon(iconOf(goal.scope()));

        GoalPresentation.appendTo(this, goal);

        append(
            MavenPrimeBundle.message("mavenprime.goals.scopeSuffix", goal.scope().getTitle()),
            SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
    }

    private static Icon iconOf(GoalScope scope)
    {
        return (scope == GoalScope.PROJECT) ? AllIcons.Nodes.Module : AllIcons.Nodes.ConfigFolder;
    }
}
