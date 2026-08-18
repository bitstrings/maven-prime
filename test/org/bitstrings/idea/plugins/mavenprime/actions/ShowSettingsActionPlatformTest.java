package org.bitstrings.idea.plugins.mavenprime.actions;

import java.util.List;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ShowSettingsActionPlatformTest
    extends BasePlatformTestCase
{
    private static final String MAVEN_GEAR_GROUP = "Maven.ShowSettingsGroup";

    private static final String MAVEN_SETTINGS = "Maven.ShowSettings";

    private static final String OWN_SETTINGS = "MavenPrime.ShowSettings";

    public void testShowSettings_theMavenGearMenu_carriesTheMavenPrimeEntryAfterTheMavenOne()
    {
        List<AnAction> gear = List.of(gearGroup().getChildren(null));

        AnAction mavenSettings = ActionManager.getInstance().getAction(MAVEN_SETTINGS);
        AnAction ownSettings = ActionManager.getInstance().getAction(OWN_SETTINGS);

        assertTrue("the Maven gear menu no longer offers Maven's own settings", gear.contains(mavenSettings));
        assertEquals(
            "the wheel is where a user goes for settings, so the Maven Prime page has to sit beside Maven's",
            gear.indexOf(mavenSettings) + 1,
            gear.indexOf(ownSettings));
    }

    private static ActionGroup gearGroup()
    {
        AnAction group = ActionManager.getInstance().getAction(MAVEN_GEAR_GROUP);

        assertNotNull("the Maven plugin no longer registers " + MAVEN_GEAR_GROUP, group);

        return (ActionGroup) group;
    }
}
