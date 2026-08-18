package org.bitstrings.idea.plugins.mavenprime.goals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GoalStoreTest
{
    @Test
    public void getState_nothingEverStored_leavesTheStateAtItsDefaultSoNoFileIsWritten()
    {
        assertNull(new TestGoalStore().getState().appliedDefaults);
    }

    @Test
    public void getState_everyGoalDeletedAfterSeeding_stillDiffersFromAFreshState()
    {
        TestGoalStore store = new TestGoalStore();

        store.applyDefaults();
        store.setGoals(List.of());

        GoalList persisted = store.getState();

        assertTrue(persisted.goals.isEmpty());
        assertFalse(persisted.appliedDefaults.isEmpty());
    }

    @Test
    public void seedDefaults_aFreshStore_reportsEveryDefaultGoal()
    {
        TestGoalStore store = new TestGoalStore();

        store.applyDefaults();

        assertEquals(ApplicationGoalStore.defaults().size(), store.getState().goals.size());
    }

    @Test
    public void mergeDefaults_aDefaultAddedSinceTheStateWasWritten_isAppended()
    {
        TestGoalStore store = new TestGoalStore();

        store.loadState(stateWith(List.of("install"), GoalDefinition.of("install")));
        store.topUp();

        assertTrue(
            "a default introduced by a newer build has to reach an existing installation",
            idsOf(store).contains("dependency-resolve-updatesnapshots"));
    }

    @Test
    public void mergeDefaults_aDefaultTheUserDeleted_isNotResurrected()
    {
        TestGoalStore store = new TestGoalStore();

        store.applyDefaults();
        store.setGoals(withoutId(store.getGoals(), "test"));
        store.topUp();

        assertFalse(
            "a default already offered once must stay deleted",
            idsOf(store).contains("test"));
    }

    @Test
    public void mergeDefaults_stateWrittenBeforeIdsWereDerived_doesNotDuplicateWhatItAlreadyHas()
    {
        TestGoalStore store = new TestGoalStore();

        GoalDefinition stored = GoalDefinition.of("install");

        stored.id = "8f14e45f-ceea-467a-9575-4b1c04b3e2f5";

        store.loadState(stateWith(null, stored));
        store.topUp();

        assertEquals(
            "an installation upgrading from random identifiers must not gain a second copy",
            1,
            countOfGoalLine(store, "install"));
    }

    @Test
    public void setGoals_aGoalCarryingAName_takesItsIdFromThatName()
    {
        TestGoalStore store = new TestGoalStore();

        store.setGoals(List.of(GoalDefinition.of("Clean Install (skip tests)", "clean install")));

        assertEquals("clean-install-skip-tests", store.getGoals().get(0).id);
    }

    @Test
    public void setGoals_twoGoalsSharingADisplayName_areGivenDistinctIds()
    {
        TestGoalStore store = new TestGoalStore();

        store.setGoals(List.of(GoalDefinition.of("verify"), GoalDefinition.of("verify")));

        assertEquals(List.of("verify", "verify-2"), idsOf(store));
    }

    @Test
    public void setGoals_aGoalRenamedAfterItWasStored_keepsTheIdItsShortcutIsBoundTo()
    {
        TestGoalStore store = new TestGoalStore();

        store.setGoals(List.of(GoalDefinition.of("verify")));

        GoalDefinition renamed = store.getGoals().get(0);

        renamed.name = "Nightly verification";

        store.setGoals(List.of(renamed));

        assertEquals("verify", store.getGoals().get(0).id);
    }

    @Test
    public void mergeDefaults_theGoalListAnEarlierBuildWrote_gainsExactlyTheDefaultsItLacks()
    {
        TestGoalStore store = new TestGoalStore();

        store.loadState(
            stateWith(
                null,
                GoalDefinition.of("clean install"),
                GoalDefinition.of("clean install (skip tests)", "clean install"),
                GoalDefinition.of("clean package"),
                GoalDefinition.of("clean verify"),
                GoalDefinition.of("test"),
                GoalDefinition.of("dependency:tree"),
                GoalDefinition.of("help:effective-pom"),
                GoalDefinition.of("versions:display-dependency-updates")));

        List<String> before = idsOf(store);

        store.topUp();

        assertEquals(
            List.of(
                "install",
                "package",
                "clean-install-skiptests",
                "verify",
                "test-compile",
                "clean",
                "dependency-analyze",
                "dependency-resolve-updatesnapshots"),
            idsOf(store).stream().filter(id -> !before.contains(id)).toList());
    }

    @Test
    public void mergeDefaults_aDefaultWhoseLabelChanged_isRecognizedByItsUnchangedKey()
    {
        TestGoalStore store = new TestGoalStore();

        GoalDefinition applied = GoalDefinition.of("dependency:resolve -U", "dependency:resolve");

        applied.id = "dependency-resolve-updatesnapshots";

        store.loadState(stateWith(List.of(applied.id), applied));
        store.topUp();

        assertEquals(
            "a default's identity is its key, so relabeling it must not produce a second copy",
            1L,
            countOfGoalLine(store, "dependency:resolve"));
    }

    @Test
    public void defaults_aNamedDefault_carriesAKeyThatItsLabelCannotChange()
    {
        assertEquals(
            List.of("clean-install-skiptests", "dependency-resolve-updatesnapshots"),
            ApplicationGoalStore
                .defaults()
                .stream()
                .filter(goal -> !goal.template.flags.isEmpty())
                .map(goal -> goal.id)
                .toList());
    }

    private static GoalList stateWith(List<String> appliedDefaults, GoalDefinition... goals)
    {
        GoalList state = new GoalList();

        state.appliedDefaults = (appliedDefaults == null) ? null : new ArrayList<>(appliedDefaults);
        state.goals.addAll(List.of(goals));

        return state;
    }

    private static List<GoalDefinition> withoutId(List<GoalDefinition> goals, String id)
    {
        return goals.stream().filter(goal -> !id.equals(goal.id)).toList();
    }

    private static List<String> idsOf(TestGoalStore store)
    {
        return store.getGoals().stream().map(goal -> goal.id).toList();
    }

    private static long countOfGoalLine(TestGoalStore store, String goalLine)
    {
        return store.getGoals().stream().filter(goal -> goalLine.equals(goal.getGoalLine())).count();
    }

    private static final class TestGoalStore
        extends GoalStore
    {
        void applyDefaults()
        {
            seedDefaults(ApplicationGoalStore.defaults());
        }

        void topUp()
        {
            mergeDefaults(ApplicationGoalStore.defaults());
        }
    }
}
