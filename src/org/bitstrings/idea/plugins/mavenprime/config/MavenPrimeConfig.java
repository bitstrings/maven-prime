package org.bitstrings.idea.plugins.mavenprime.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalIds;

public final class MavenPrimeConfig
{
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;

    public ContextConfig defaults;

    public List<GoalConfig> goals;

    public MavenPrimeConfig()
    {
    }

    public static MavenPrimeConfig of(ContextConfig defaults, List<GoalDefinition> definitions)
    {
        MavenPrimeConfig config = new MavenPrimeConfig();

        config.defaults = defaults;
        config.goals = new ArrayList<>(definitions.size());

        for (GoalDefinition definition : definitions)
        {
            config.goals.add(GoalConfig.of(definition));
        }

        return config;
    }

    public List<GoalDefinition> getGoalDefinitions()
    {
        List<GoalDefinition> definitions = new ArrayList<>();

        if (goals == null)
        {
            return definitions;
        }

        Set<String> taken = new HashSet<>();

        for (GoalConfig goal : goals)
        {
            if (goal == null)
            {
                continue;
            }

            GoalDefinition definition = goal.toDefinition();

            if (definition.isComplete())
            {
                definition.id = unique(definition.id, taken);

                definitions.add(definition);
            }
        }

        return definitions;
    }

    private static String unique(String id, Set<String> taken)
    {
        String candidate = GoalIds.uniqueIn(id, taken);

        taken.add(candidate);

        return candidate;
    }
}
