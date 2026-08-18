package org.bitstrings.idea.plugins.mavenprime.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.junit.Test;

public class MavenPrimeConfigTest
{
    @Test
    public void getGoalDefinitions_readTwice_yieldsTheSameIdentities()
    {
        MavenPrimeConfig config = configOf("clean install", "clean verify");

        assertEquals(idsOf(config.getGoalDefinitions()), idsOf(config.getGoalDefinitions()));
    }

    @Test
    public void getGoalDefinitions_twoIndistinguishableGoals_givesThemDistinctIdentities()
    {
        List<GoalDefinition> definitions = configOf("clean install", "clean install").getGoalDefinitions();

        assertNotEquals(definitions.get(0).id, definitions.get(1).id);
    }

    @Test
    public void getGoalDefinitions_goalArrayReordered_derivesTheSameIdsSoKeymapBindingsSurvive()
    {
        assertEquals(
            Set.copyOf(idsOf(configOf("clean install", "clean verify").getGoalDefinitions())),
            Set.copyOf(idsOf(configOf("clean verify", "clean install").getGoalDefinitions())));
    }

    @Test
    public void getGoalDefinitions_noGoalsDeclared_reportsNothing()
    {
        assertEquals(List.of(), new MavenPrimeConfig().getGoalDefinitions());
    }

    private static MavenPrimeConfig configOf(String... goalLines)
    {
        MavenPrimeConfig config = new MavenPrimeConfig();

        config.goals = new ArrayList<>();

        for (String goalLine : goalLines)
        {
            config.goals.add(GoalConfigTest.goal(goalLine));
        }

        return config;
    }

    private static List<String> idsOf(List<GoalDefinition> definitions)
    {
        List<String> ids = new ArrayList<>();

        for (GoalDefinition definition : definitions)
        {
            ids.add(definition.id);
        }

        return ids;
    }
}
