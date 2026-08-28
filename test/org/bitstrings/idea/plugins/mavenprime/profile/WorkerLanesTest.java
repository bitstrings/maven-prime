package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.junit.Test;

public class WorkerLanesTest
{
    @Test
    public void of_twoModulesThatNeverOverlappedButRanOnDifferentWorkers_keepsThemApart()
    {
        assertEquals(
            "packing by overlap folds back-to-back modules into one lane, which is exactly where a "
                + "worker sitting idle stops being visible",
            2,
            lanesOf(module("g:a", 0L, 100L, "w-1"), module("g:b", 100L, 100L, "w-2")).count());
    }

    @Test
    public void of_theSameTwoModulesWithNoWorkerRecorded_packsThemIntoOneLane()
    {
        assertEquals(
            1,
            lanesOf(new ModuleTiming("g:a", 0L, 100L), new ModuleTiming("g:b", 100L, 100L)).count());
    }

    @Test
    public void of_twoModulesOnOneWorker_putsThemInTheSameLane()
    {
        WorkerLanes lanes = lanesOf(module("g:a", 0L, 100L, "w-1"), module("g:b", 100L, 50L, "w-1"));

        assertEquals(lanes.laneOf("g:a"), lanes.laneOf("g:b"));
    }

    @Test
    public void of_workersThatStartedAtDifferentTimes_ordersTheLanesByThatFirstStart()
    {
        WorkerLanes lanes = lanesOf(module("g:late", 500L, 10L, "w-2"), module("g:early", 0L, 10L, "w-1"));

        assertEquals(0, lanes.laneOf("g:early"));
    }

    @Test
    public void of_oneModuleMissingItsWorker_fallsBackForTheWholeBuildRatherThanMixingTheTwo()
    {
        assertFalse(
            "half the lanes read from the build and half guessed would put two modules of one worker "
                + "on separate rows",
            lanesOf(module("g:a", 0L, 100L, "w-1"), new ModuleTiming("g:b", 0L, 100L)).isByWorker());
    }

    @Test
    public void of_everyModuleNamingItsWorker_readsTheLanesFromTheBuild()
    {
        assertTrue(lanesOf(module("g:a", 0L, 100L, "w-1")).isByWorker());
    }

    @Test
    public void ordinalOf_theWorkerThatStartedFirst_readsAsTheFirstLane()
    {
        assertEquals(0, lanesOf(module("g:late", 500L, 10L, "42"), module("g:early", 0L, 10L, "17")).ordinalOf("17"));
    }

    @Test
    public void ordinalOf_aThreadThatBuiltNothing_hasNoLaneToNumber()
    {
        assertEquals(-1, lanesOf(module("g:a", 0L, 100L, "17")).ordinalOf("99"));
    }

    @Test
    public void laneOf_aModuleThatIsNotInTheBuild_reportsNoLane()
    {
        assertEquals(-1, lanesOf(module("g:a", 0L, 100L, "w-1")).laneOf("g:ghost"));
    }

    @Test
    public void count_aBuildWithNoModules_stillLeavesOneLaneToDrawInto()
    {
        assertEquals(1, lanesOf().count());
    }

    @Test
    public void ordinalOf_aWorkerNamedOnlyByAGoal_isStillNumbered()
    {
        assertEquals(
            "a cancelled build reports the goals it finished and no module timings, and a roster read "
                + "from modules alone leaves the raw thread id as the only thing left to show",
            0,
            WorkerLanes.of(List.of(), List.of(mojo("g:a", 0L, "31"))).ordinalOf("31"));
    }

    @Test
    public void of_workersNamedOnlyByGoals_readsThoseWorkersRatherThanFallingBack()
    {
        assertTrue(WorkerLanes.of(List.of(), List.of(mojo("g:a", 0L, "31"))).isByWorker());
    }

    @Test
    public void ordinalOf_aGoalOnAWorkerThatAlsoRanAModule_numbersItOnceAgainstTheModuleLane()
    {
        WorkerLanes lanes =
            WorkerLanes.of(
                List.of(module("g:a", 0L, 100L, "w-2"), module("g:b", 0L, 100L, "w-1")),
                List.of(mojo("g:a", 0L, "w-2")));

        assertEquals(
            "the timeline numbers a bar by its lane and the table numbers a row by this ordinal, so "
                + "the two must name the same thread",
            lanes.laneOf("g:a"),
            lanes.ordinalOf("w-2"));
    }

    @Test
    public void of_goalsNamingNoWorkerAtAll_leavesTheRosterEmpty()
    {
        assertFalse(
            WorkerLanes.of(List.of(), List.of(new MojoTiming("g:a", "p:goal", "d", 0L, 10L))).isByWorker());
    }

    private static WorkerLanes lanesOf(ModuleTiming... modules)
    {
        return WorkerLanes.of(List.of(modules), List.of());
    }

    private static ModuleTiming module(String name, long start, long duration, String worker)
    {
        return new ModuleTiming(name, start, duration, worker);
    }

    private static MojoTiming mojo(String module, long start, String worker)
    {
        return new MojoTiming(module, "p:goal", "default", start, 10L, "compile", worker);
    }
}
