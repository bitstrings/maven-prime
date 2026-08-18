package org.bitstrings.idea.plugins.mavenprime.goals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.junit.Test;

public class ApplicationGoalStoreTest
{
    @Test
    public void defaults_theSkipTestsVariant_carriesTheSkipTestsFlag()
    {
        assertTrue(skipTestsVariant().template.flags.contains(MavenFlag.SKIP_TESTS));
    }

    @Test
    public void defaults_theSkipTestsVariant_keepsItsGoalLineFreeOfRawOptionTokens()
    {
        assertEquals(
            "the IDE Maven runner reads skipTests from the flags, never from the goal list",
            "clean install",
            skipTestsVariant().getGoalLine());
    }

    @Test
    public void defaults_theSkipTestsVariant_readsDifferentlyFromPlainCleanInstall()
    {
        assertNotEquals("clean install", skipTestsVariant().getDisplayName());
    }

    @Test
    public void defaults_theDependencyResolveVariant_carriesTheUpdateSnapshotsFlag()
    {
        assertTrue(updateSnapshotsVariant().template.flags.contains(MavenFlag.UPDATE_SNAPSHOTS));
    }

    @Test
    public void defaults_theDependencyResolveVariant_keepsItsGoalLineFreeOfRawOptionTokens()
    {
        assertEquals(
            "a -U parked in the goal line is executed as a goal, not as an option",
            "dependency:resolve",
            updateSnapshotsVariant().getGoalLine());
    }

    @Test
    public void defaults_theDependencyResolveVariant_namesTheOptionItCarries()
    {
        assertEquals("dependency:resolve (update)", updateSnapshotsVariant().getDisplayName());
    }

    private static GoalDefinition skipTestsVariant()
    {
        return variantCarrying(MavenFlag.SKIP_TESTS);
    }

    private static GoalDefinition updateSnapshotsVariant()
    {
        return variantCarrying(MavenFlag.UPDATE_SNAPSHOTS);
    }

    private static GoalDefinition variantCarrying(MavenFlag flag)
    {
        return ApplicationGoalStore
            .defaults()
            .stream()
            .filter(definition -> definition.template.flags.contains(flag))
            .findFirst()
            .orElseThrow();
    }
}
