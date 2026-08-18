package org.bitstrings.idea.plugins.mavenprime.run;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalScope;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenPrimeBeforeRunTaskProviderPlatformTest
    extends BasePlatformTestCase
{
    private static final String GOAL_NAME = "Fast Compile";

    private static final String GOAL_LINE = "clean compile";

    private List<GoalDefinition> savedGoals;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        savedGoals = GoalRegistry.getInstance(getProject()).global();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            GoalRegistry.getInstance(getProject()).updateGlobal(savedGoals);
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testGetProvider_everyRunConfigurationInTheIde_offersTheMavenPrimeStep()
    {
        assertNotNull(
            "without its registration the step never appears in any run configuration's before-launch list",
            BeforeRunTaskProvider.getProvider(getProject(), MavenPrimeBeforeRunTaskProvider.ID));
    }

    public void testCanExecuteTask_aStepNamingAGoalThatWasDeleted_refusesToRun()
    {
        MavenPrimeBeforeRunTask task = new MavenPrimeBeforeRunTask();

        task.setGoalId("a-goal-nobody-stored");

        assertFalse(
            "running a step whose goal is gone would launch Maven with an empty command line",
            provider().canExecuteTask(null, task));
    }

    public void testCanExecuteTask_aStepNamingAStoredGoal_isReadyToRun()
    {
        assertTrue(
            "a step the user configured has to survive until they change it",
            provider().canExecuteTask(null, taskFor(storedGoal())));
    }

    public void testGetDescription_aStepNamingAStoredGoal_namesThatGoalInTheBeforeLaunchList()
    {
        assertTrue(
            "the before-launch row is the only place the chosen goal is visible",
            provider().getDescription(taskFor(storedGoal())).contains(GOAL_NAME));
    }

    public void testGetDescription_aStepWhoseGoalIsGone_saysSoInsteadOfNamingNothing()
    {
        MavenPrimeBeforeRunTask task = new MavenPrimeBeforeRunTask();

        task.setGoalId("a-goal-nobody-stored");

        assertFalse(
            "a row that reads as configured hides that the launch will fail",
            provider().getDescription(task).contains(GOAL_NAME));
    }

    public void testRequestFor_theChosenGoal_carriesItsGoalLineAndName()
    {
        MavenPrimeRequest request =
            MavenPrimeBeforeRunTaskProvider.requestFor(
                GoalRegistry.getInstance(getProject()).find(storedGoal()));

        assertEquals(
            "a request built without the goal line would run Maven with no goals at all",
            GOAL_LINE,
            request.getGoalLine());
        assertEquals(GOAL_NAME, request.name);
    }

    private String storedGoal()
    {
        return GoalRegistry
            .getInstance(getProject())
            .add(GoalScope.GLOBAL, GoalDefinition.of(GOAL_NAME, GOAL_LINE));
    }

    private static MavenPrimeBeforeRunTask taskFor(String goalId)
    {
        MavenPrimeBeforeRunTask task = new MavenPrimeBeforeRunTask();

        task.setGoalId(goalId);

        return task;
    }

    private MavenPrimeBeforeRunTaskProvider provider()
    {
        return new MavenPrimeBeforeRunTaskProvider(getProject());
    }
}
