package org.bitstrings.idea.plugins.mavenprime.goals;

public record GoalLabel(String displayName, GoalScope scope)
{
    public static GoalLabel of(ScopedGoal goal)
    {
        return new GoalLabel(goal.getDisplayName(), goal.scope());
    }

    public String getText()
    {
        return displayName + " (" + scope.getTitle() + ')';
    }
}
