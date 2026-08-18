package org.bitstrings.idea.plugins.mavenprime.run;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class MavenPrimeBeforeRunTaskTest
{
    private static final String GOAL_ID = "fast-compile";

    @Test
    public void loadState_aTaskStoredWithItsRunConfiguration_readsBackItsGoal()
    {
        MavenPrimeBeforeRunTask stored = new MavenPrimeBeforeRunTask();

        stored.setGoalId(GOAL_ID);

        MavenPrimeBeforeRunTask read = new MavenPrimeBeforeRunTask();

        read.loadState(stored.getState());

        assertEquals(
            "a goal that does not survive the round trip leaves the before-launch step pointing at "
                + "nothing after the IDE restarts",
            GOAL_ID,
            read.getGoalId());
    }

    @Test
    public void loadState_aTaskStoredBeforeAGoalWasChosen_readsBackAsUnconfigured()
    {
        MavenPrimeBeforeRunTask read = new MavenPrimeBeforeRunTask();

        read.loadState(new MavenPrimeBeforeRunTask().getState());

        assertEquals("an unset goal must not read back as the literal null", "", read.getGoalId());
    }

    @Test
    public void equals_twoTasksNamingDifferentGoals_areNotInterchangeable()
    {
        MavenPrimeBeforeRunTask compile = new MavenPrimeBeforeRunTask();
        MavenPrimeBeforeRunTask verify = new MavenPrimeBeforeRunTask();

        compile.setGoalId(GOAL_ID);
        verify.setGoalId("verify");

        assertNotEquals(
            "the run configuration keeps one task per equal value, so ignoring the goal drops the "
                + "second step the user added",
            compile,
            verify);
    }
}
