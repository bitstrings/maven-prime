package org.bitstrings.idea.plugins.mavenprime.run;

import java.util.List;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class RunTestWithMavenPrimeActionPlatformTest
    extends BasePlatformTestCase
{
    private static final String RUN_ACTION = "MavenPrime.RunTestWithMavenPrime";

    private static final String DEBUG_ACTION = "MavenPrime.DebugTestWithMavenPrime";

    private static final String RUN_CONTEXT_GROUP = "RunContextGroupInner";

    private static final String PLATFORM_EXECUTORS = "RunContextExecutorsGroup";

    private static final String MORE_RUN_DEBUG = "RunContextGroupMore";

    private static final String GUTTER_EXTRAS = "RunLineMarkerExtraActions";

    public void testAction_theRunContextBlock_holdsBothEntriesBesideThePlatformsOwn()
    {
        List<AnAction> children = childrenOf(RUN_CONTEXT_GROUP);

        assertTrue(
            "outside the run block the entry lands wherever the menu ends, which is what buried it",
            children.contains(action(RUN_ACTION)));
        assertTrue(children.contains(action(DEBUG_ACTION)));
    }

    public void testAction_theRunContextBlock_ordersRunThenDebugAheadOfMoreRunDebug()
    {
        List<AnAction> children = childrenOf(RUN_CONTEXT_GROUP);

        int platform = children.indexOf(action(PLATFORM_EXECUTORS));
        int run = children.indexOf(action(RUN_ACTION));
        int debug = children.indexOf(action(DEBUG_ACTION));
        int more = children.indexOf(action(MORE_RUN_DEBUG));

        assertTrue("Run 'X' and Debug 'X' come first, ours read as the Maven Prime variant of them", platform < run);
        assertTrue("Debug sits under its own Run entry, the way the platform pairs them", run < debug);
        assertTrue("behind the overflow submenu is the burial the user reported", debug < more);
    }

    public void testAction_theGutterRunPopup_holdsBothEntries()
    {
        List<AnAction> children = childrenOf(GUTTER_EXTRAS);

        assertTrue(
            "the gutter icon is where one test actually gets run, and it reached Maven Prime only through "
                + "More Run/Debug",
            children.contains(action(RUN_ACTION)));
        assertTrue(children.contains(action(DEBUG_ACTION)));
    }

    public void testAction_theRunEntry_carriesTheIconTheIdeUsesForRunning()
    {
        assertEquals(
            "a Maven Prime logo beside Run 'X' does not read as an action that runs anything",
            iconOf("ChooseRunConfiguration"),
            iconOf(RUN_ACTION));
    }

    public void testAction_theDebugEntry_carriesTheIconTheIdeUsesForDebugging()
    {
        assertEquals(iconOf("ChooseDebugConfiguration"), iconOf(DEBUG_ACTION));
    }

    public void testAction_aContextWithNoTest_hidesTheRunEntry()
    {
        assertFalse(
            "an entry shown on every right-click would be noise in files that hold no tests",
            visibilityOf(RUN_ACTION));
    }

    public void testAction_aContextWithNoTest_hidesTheDebugEntry()
    {
        assertFalse(visibilityOf(DEBUG_ACTION));
    }

    private static boolean visibilityOf(String actionId)
    {
        AnActionEvent event =
            AnActionEvent.createEvent(
                DataContext.EMPTY_CONTEXT,
                new Presentation(),
                "MavenPrimeTest",
                ActionUiKind.NONE,
                null);

        action(actionId).update(event);

        return event.getPresentation().isVisible();
    }

    private static Object iconOf(String actionId)
    {
        return action(actionId).getTemplatePresentation().getIcon();
    }

    private static AnAction action(String actionId)
    {
        AnAction action = ActionManager.getInstance().getAction(actionId);

        assertNotNull(actionId + " is gone from the platform", action);

        return action;
    }

    private static List<AnAction> childrenOf(String groupId)
    {
        return List.of(((ActionGroup) action(groupId)).getChildren(null));
    }
}
