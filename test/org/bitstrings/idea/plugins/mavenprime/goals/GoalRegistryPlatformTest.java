package org.bitstrings.idea.plugins.mavenprime.goals;

import java.util.List;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class GoalRegistryPlatformTest
    extends BasePlatformTestCase
{
    private static final String UNKNOWN_ID = "no-such-goal";

    private List<GoalDefinition> savedGlobalGoals;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        savedGlobalGoals = ApplicationGoalStore.getInstance().getGoals();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            ApplicationGoalStore.getInstance().setGoals(savedGlobalGoals);
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testContains_registeredGlobalGoal_isTrue()
    {
        assertTrue(registry().contains(store(GoalDefinition.of("clean install")).id));
    }

    public void testContains_registeredGlobalGoal_agreesWithFind()
    {
        String id = store(GoalDefinition.of("clean install")).id;

        assertEquals(registry().find(id) != null, registry().contains(id));
    }

    public void testContains_unknownId_isFalse()
    {
        store(GoalDefinition.of("clean install"));

        assertFalse(registry().contains(UNKNOWN_ID));
    }

    public void testContains_unknownId_agreesWithFind()
    {
        store(GoalDefinition.of("clean install"));

        assertEquals(registry().find(UNKNOWN_ID) != null, registry().contains(UNKNOWN_ID));
    }

    public void testContains_goalWithoutGoals_isFalseJustLikeFind()
    {
        GoalDefinition incomplete = store(new GoalDefinition());

        assertNull(registry().find(incomplete.id));
        assertFalse(registry().contains(incomplete.id));
    }

    public void testContains_goalRemovedAfterBeingStored_isFalseAgain()
    {
        String id = store(GoalDefinition.of("clean install")).id;

        ApplicationGoalStore.getInstance().setGoals(List.of());

        assertFalse(registry().contains(id));
    }

    public void testMove_globalGoalMovedDown_swapsItWithItsNeighbour()
    {
        GoalDefinition first = GoalDefinition.of("clean install");

        storeAll(first, GoalDefinition.of("clean verify"));

        registry().move(GoalScope.GLOBAL, first.id, 1);

        assertEquals(List.of("clean verify", "clean install"), goalLinesOf(registry().global()));
    }

    public void testMove_globalGoalMovedUp_swapsItWithItsNeighbour()
    {
        GoalDefinition second = GoalDefinition.of("clean verify");

        storeAll(GoalDefinition.of("clean install"), second);

        registry().move(GoalScope.GLOBAL, second.id, -1);

        assertEquals(List.of("clean verify", "clean install"), goalLinesOf(registry().global()));
    }

    public void testMove_theFirstGoalMovedUp_leavesTheOrderAlone()
    {
        GoalDefinition first = GoalDefinition.of("clean install");

        storeAll(first, GoalDefinition.of("clean verify"));

        registry().move(GoalScope.GLOBAL, first.id, -1);

        assertEquals(List.of("clean install", "clean verify"), goalLinesOf(registry().global()));
    }

    public void testMove_theFirstGoalMovedUp_reportsThatNothingMoved()
    {
        GoalDefinition first = GoalDefinition.of("clean install");

        storeAll(first, GoalDefinition.of("clean verify"));

        assertFalse(registry().move(GoalScope.GLOBAL, first.id, -1));
    }

    public void testMove_unknownGoalId_reportsThatNothingMoved()
    {
        storeAll(GoalDefinition.of("clean install"));

        assertFalse(registry().move(GoalScope.GLOBAL, UNKNOWN_ID, 1));
    }

    public void testCanMove_theLastGoalMovedDown_isFalseSoTheButtonDisables()
    {
        GoalDefinition last = GoalDefinition.of("clean verify");

        storeAll(GoalDefinition.of("clean install"), last);

        assertFalse(registry().canMove(GoalScope.GLOBAL, last.id, 1));
    }

    public void testCanMove_aGoalWithRoomBelow_isTrue()
    {
        GoalDefinition first = GoalDefinition.of("clean install");

        storeAll(first, GoalDefinition.of("clean verify"));

        assertTrue(registry().canMove(GoalScope.GLOBAL, first.id, 1));
    }

    public void testCanMove_agreesWithWhatMoveActuallyDoes()
    {
        GoalDefinition last = GoalDefinition.of("clean verify");

        storeAll(GoalDefinition.of("clean install"), last);

        assertEquals(
            registry().canMove(GoalScope.GLOBAL, last.id, 1),
            registry().move(GoalScope.GLOBAL, last.id, 1));
    }

    private GoalRegistry registry()
    {
        return GoalRegistry.getInstance(getProject());
    }

    private static void storeAll(GoalDefinition... definitions)
    {
        ApplicationGoalStore.getInstance().setGoals(List.of(definitions));
    }

    private static List<String> goalLinesOf(List<GoalDefinition> definitions)
    {
        return definitions.stream().map(GoalDefinition::getGoalLine).toList();
    }

    private static GoalDefinition store(GoalDefinition definition)
    {
        ApplicationGoalStore.getInstance().setGoals(List.of(definition));

        return definition;
    }
}
