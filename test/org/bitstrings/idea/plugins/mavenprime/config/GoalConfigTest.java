package org.bitstrings.idea.plugins.mavenprime.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;

import org.junit.Test;

public class GoalConfigTest
{
    @Test
    public void toDefinition_configWithoutAnId_derivesTheSameIdEveryTime()
    {
        assertEquals(goal("clean install").toDefinition().id, goal("clean install").toDefinition().id);
    }

    @Test
    public void toDefinition_configCarryingAnExplicitId_keepsThatId()
    {
        GoalConfig config = goal("clean install");

        config.id = "team-baseline";

        assertEquals("team-baseline", config.toDefinition().id);
    }

    @Test
    public void toDefinition_editedGoalLine_derivesADifferentIdBecauseTheIdFollowsTheCommand()
    {
        assertNotEquals(goal("clean install").toDefinition().id, goal("clean verify").toDefinition().id);
    }

    @Test
    public void toDefinition_renamedGoal_keepsTheSameIdSoKeymapBindingsSurvive()
    {
        GoalConfig named = goal("clean install");

        named.name = "Full build";

        assertEquals(named.toDefinition().id, goal("clean install").toDefinition().id);
    }

    static GoalConfig goal(String goalLine)
    {
        GoalConfig config = new GoalConfig();

        config.request = new RequestConfig();
        config.request.goals = List.of(goalLine);

        return config;
    }
}
