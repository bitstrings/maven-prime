package org.bitstrings.idea.plugins.mavenprime.actions;

import java.util.List;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CreateSharedConfigActionPlatformTest
    extends BasePlatformTestCase
{
    private static final String MAVEN_GEAR_GROUP = "Maven.ShowSettingsGroup";

    private static final String OWN_SETTINGS = "MavenPrime.ShowSettings";

    private static final String CREATE_SHARED_CONFIG = "MavenPrime.CreateSharedConfig";

    public void testCreateSharedConfig_theMavenGearMenu_carriesItBeforeTheMavenPrimeSettingsEntry()
    {
        List<AnAction> gear = List.of(gearGroup().getChildren(null));

        AnAction ownSettings = ActionManager.getInstance().getAction(OWN_SETTINGS);
        AnAction createConfig = ActionManager.getInstance().getAction(CREATE_SHARED_CONFIG);

        assertNotNull("the create action is no longer registered under " + CREATE_SHARED_CONFIG, createConfig);
        assertEquals(
            "creating the shared file is what a user reaches for before opening the page that edits it",
            gear.indexOf(createConfig) + 1,
            gear.indexOf(ownSettings));
    }

    private static ActionGroup gearGroup()
    {
        AnAction group = ActionManager.getInstance().getAction(MAVEN_GEAR_GROUP);

        assertNotNull("the Maven plugin no longer registers " + MAVEN_GEAR_GROUP, group);

        return (ActionGroup) group;
    }
}
