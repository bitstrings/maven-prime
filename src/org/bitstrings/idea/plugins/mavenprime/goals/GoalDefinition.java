package org.bitstrings.idea.plugins.mavenprime.goals;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;

public final class GoalDefinition
{
    public String id;

    public String name;

    public MavenPrimeRequest template = new MavenPrimeRequest();

    public GoalDefinition()
    {
    }

    public static GoalDefinition of(String goalLine)
    {
        GoalDefinition definition = new GoalDefinition();

        definition.template.setGoalLine(goalLine);
        definition.id = definition.derivedId();

        return definition;
    }

    public static GoalDefinition of(String name, String goalLine)
    {
        GoalDefinition definition = of(goalLine);

        definition.name = name;
        definition.id = definition.derivedId();

        return definition;
    }

    public String getDisplayName()
    {
        return StringUtils.defaultIfBlank(name, template.getGoalLine());
    }

    public String getGoalLine()
    {
        return template.getGoalLine();
    }

    public String derivedId()
    {
        return GoalIds.slug(getDisplayName());
    }

    public boolean isComplete()
    {
        return template.hasGoals();
    }

    public GoalDefinition copy()
    {
        GoalDefinition copy = new GoalDefinition();

        copy.id = id;
        copy.name = name;
        copy.template = template.copy();

        return copy;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof GoalDefinition))
        {
            return false;
        }

        GoalDefinition that = (GoalDefinition) other;

        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && template.equals(that.template);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, name, template);
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
