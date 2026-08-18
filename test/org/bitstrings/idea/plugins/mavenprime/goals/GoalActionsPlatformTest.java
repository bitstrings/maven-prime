package org.bitstrings.idea.plugins.mavenprime.goals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class GoalActionsPlatformTest
    extends BasePlatformTestCase
{
    private static final String GOAL_ID = "mavenprime-platform-test";

    private static final String RUN_ACTION_ID = GoalActions.ID_PREFIX + GOAL_ID;

    private static final String DEBUG_ACTION_ID = GoalActions.DEBUG_ID_PREFIX + GOAL_ID;

    private static final GoalLabel INITIAL = new GoalLabel("clean install", GoalScope.PROJECT);

    private static final GoalLabel RENAMED = new GoalLabel("Full build", GoalScope.PROJECT);

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            GoalActions.getInstance().apply(Map.of());
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testApply_goalThatWasNeverRegistered_registersARunActionUnderItsStableId()
    {
        GoalActions.getInstance().apply(Map.of(GOAL_ID, INITIAL));

        assertNotNull(ActionManager.getInstance().getAction(RUN_ACTION_ID));
    }

    public void testApply_goalThatWasNeverRegistered_registersADebugActionUnderItsOwnStableId()
    {
        GoalActions.getInstance().apply(Map.of(GOAL_ID, INITIAL));

        assertNotNull(
            "a debug entry with no action id cannot be given a keyboard shortcut",
            ActionManager.getInstance().getAction(DEBUG_ACTION_ID));
    }

    public void testApply_goalThatWasNeverRegistered_addsItToTheMavenPrimeGoalsGroup()
    {
        GoalActions.getInstance().apply(Map.of(GOAL_ID, INITIAL));

        assertTrue(childIdsOf(GoalActions.GOALS_GROUP_ID).contains(RUN_ACTION_ID));
    }

    public void testApply_goalThatWasNeverRegistered_addsItToTheDebugGoalsGroup()
    {
        GoalActions.getInstance().apply(Map.of(GOAL_ID, INITIAL));

        assertTrue(
            "every goal you can run has to be one you can debug",
            childIdsOf(GoalActions.DEBUG_GOALS_GROUP_ID).contains(DEBUG_ACTION_ID));
    }

    public void testApply_goalThatWasNeverRegistered_marksTheDebugActionWithTheDebuggerIcon()
    {
        GoalActions.getInstance().apply(Map.of(GOAL_ID, INITIAL));

        assertEquals(
            "the debug entry must not read as another way to run the goal",
            AllIcons.Actions.StartDebugger,
            ActionManager.getInstance().getAction(DEBUG_ACTION_ID).getTemplatePresentation().getIcon());
    }

    public void testApply_goalNoLongerWanted_dropsItFromTheMavenPrimeGoalsGroup()
    {
        GoalActions goalActions = GoalActions.getInstance();

        goalActions.apply(Map.of(GOAL_ID, INITIAL));
        goalActions.apply(Map.of());

        assertFalse(childIdsOf(GoalActions.GOALS_GROUP_ID).contains(RUN_ACTION_ID));
    }

    public void testApply_goalNoLongerWanted_dropsItFromTheDebugGoalsGroup()
    {
        GoalActions goalActions = GoalActions.getInstance();

        goalActions.apply(Map.of(GOAL_ID, INITIAL));
        goalActions.apply(Map.of());

        assertFalse(childIdsOf(GoalActions.DEBUG_GOALS_GROUP_ID).contains(DEBUG_ACTION_ID));
    }

    public void testApply_projectScopedGoal_showsTheScopeInTheActionText()
    {
        GoalActions.getInstance().apply(Map.of(GOAL_ID, INITIAL));

        assertTrue(runActionText().contains(GoalScope.PROJECT.getTitle()));
    }

    public void testApply_goalRenamedAfterRegistration_updatesTheActionText()
    {
        GoalActions goalActions = GoalActions.getInstance();

        goalActions.apply(Map.of(GOAL_ID, INITIAL));
        goalActions.apply(Map.of(GOAL_ID, RENAMED));

        assertEquals(RENAMED.getText(), runActionText());
    }

    public void testApply_goalRenamedAfterRegistration_keepsTheSameActionInstance()
    {
        GoalActions goalActions = GoalActions.getInstance();

        goalActions.apply(Map.of(GOAL_ID, INITIAL));

        Object registered = ActionManager.getInstance().getAction(RUN_ACTION_ID);

        goalActions.apply(Map.of(GOAL_ID, RENAMED));

        assertSame(registered, ActionManager.getInstance().getAction(RUN_ACTION_ID));
    }

    public void testApply_goalNoLongerWanted_unregistersItsAction()
    {
        GoalActions goalActions = GoalActions.getInstance();

        goalActions.apply(Map.of(GOAL_ID, INITIAL));
        goalActions.apply(Map.of());

        assertNull(ActionManager.getInstance().getAction(RUN_ACTION_ID));
    }

    public void testApply_goalNoLongerWanted_unregistersItsDebugAction()
    {
        GoalActions goalActions = GoalActions.getInstance();

        goalActions.apply(Map.of(GOAL_ID, INITIAL));
        goalActions.apply(Map.of());

        assertNull(ActionManager.getInstance().getAction(DEBUG_ACTION_ID));
    }

    private static String runActionText()
    {
        return ActionManager.getInstance().getAction(RUN_ACTION_ID).getTemplatePresentation().getText();
    }

    private static List<String> childIdsOf(String groupId)
    {
        ActionManager actions = ActionManager.getInstance();

        DefaultActionGroup group = (DefaultActionGroup) actions.getAction(groupId);

        List<String> ids = new ArrayList<>();

        for (AnAction child : group.getChildActionsOrStubs())
        {
            ids.add(actions.getId(child));
        }

        return ids;
    }
}
