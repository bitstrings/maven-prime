package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;

public final class GoalPresentation
{
    private static final int MAX_GOAL_LINE = 44;

    private GoalPresentation()
    {
    }

    public static void appendTo(SimpleColoredComponent target, ScopedGoal goal)
    {
        target.append(goal.getDisplayName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

        String goalLine = goal.definition().getGoalLine();

        if (Objects.equals(goal.getDisplayName(), goalLine))
        {
            return;
        }

        target.append(
            StringUtils.SPACE + StringUtil.shortenTextWithEllipsis(goalLine, MAX_GOAL_LINE, 0, true),
            SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
    }

    public static String filterTextOf(ScopedGoal goal)
    {
        return goal.getDisplayName() + ' ' + goal.definition().getGoalLine();
    }
}
