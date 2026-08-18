package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class EmbeddedIdeActionsPlatformTest
    extends BasePlatformTestCase
{
    public void testToolbar_everyEmbeddedIdeActionId_resolvesInThisIde()
    {
        List<String> unresolved = new ArrayList<>();

        for (String actionId : embeddedActionIds())
        {
            if (ActionManager.getInstance().getAction(actionId) == null)
            {
                unresolved.add(actionId);
            }
        }

        assertEquals(
            "an action id the toolbar cannot resolve loses its button silently",
            List.of(),
            unresolved);
    }

    public void testToolbar_theMavenBuildMenuGroup_resolvesInThisIde()
    {
        assertNotNull(
            "MavenPrime.RunSelectedGoals is added to Maven.BuildMenu",
            ActionManager.getInstance().getAction("Maven.BuildMenu"));
    }

    private static List<String> embeddedActionIds()
    {
        List<String> ids = new ArrayList<>();

        ids.addAll(MavenPrimeToolWindowPanel.IDE_TOGGLE_ACTIONS);
        ids.addAll(MavenPrimeToolWindowPanel.IDE_PROJECT_ACTIONS);
        ids.addAll(MavenPrimeToolWindowPanel.IDE_SETTINGS_ACTIONS);
        ids.addAll(MavenPrimeToolWindowPanel.OWN_TOOLBAR_ACTIONS);

        return ids;
    }
}
