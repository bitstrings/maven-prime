package org.bitstrings.idea.plugins.mavenprime.goals;

public record ScopedGoal(GoalScope scope, GoalDefinition definition)
{
    public String getDisplayName()
    {
        return definition.getDisplayName();
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
