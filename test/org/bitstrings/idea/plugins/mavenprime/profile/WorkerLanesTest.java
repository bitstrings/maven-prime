package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
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
            WorkerLanes
                .of(List.of(module("g:a", 0L, 100L, "w-1"), module("g:b", 100L, 100L, "w-2")))
                .count());
    }

    @Test
    public void of_theSameTwoModulesWithNoWorkerRecorded_packsThemIntoOneLane()
    {
        assertEquals(
            1,
            WorkerLanes
                .of(List.of(new ModuleTiming("g:a", 0L, 100L), new ModuleTiming("g:b", 100L, 100L)))
                .count());
    }

    @Test
    public void of_twoModulesOnOneWorker_putsThemInTheSameLane()
    {
        WorkerLanes lanes =
            WorkerLanes.of(List.of(module("g:a", 0L, 100L, "w-1"), module("g:b", 100L, 50L, "w-1")));

        assertEquals(lanes.laneOf("g:a"), lanes.laneOf("g:b"));
    }

    @Test
    public void of_workersThatStartedAtDifferentTimes_ordersTheLanesByThatFirstStart()
    {
        WorkerLanes lanes =
            WorkerLanes.of(List.of(module("g:late", 500L, 10L, "w-2"), module("g:early", 0L, 10L, "w-1")));

        assertEquals(0, lanes.laneOf("g:early"));
    }

    @Test
    public void of_oneModuleMissingItsWorker_fallsBackForTheWholeBuildRatherThanMixingTheTwo()
    {
        assertFalse(
            "half the lanes read from the build and half guessed would put two modules of one worker "
                + "on separate rows",
            WorkerLanes
                .of(List.of(module("g:a", 0L, 100L, "w-1"), new ModuleTiming("g:b", 0L, 100L)))
                .isByWorker());
    }

    @Test
    public void of_everyModuleNamingItsWorker_readsTheLanesFromTheBuild()
    {
        assertTrue(WorkerLanes.of(List.of(module("g:a", 0L, 100L, "w-1"))).isByWorker());
    }

    @Test
    public void ordinalOf_theWorkerThatStartedFirst_readsAsTheFirstLane()
    {
        assertEquals(
            0,
            WorkerLanes
                .of(List.of(module("g:late", 500L, 10L, "42"), module("g:early", 0L, 10L, "17")))
                .ordinalOf("17"));
    }

    @Test
    public void ordinalOf_aThreadThatBuiltNothing_hasNoLaneToNumber()
    {
        assertEquals(-1, WorkerLanes.of(List.of(module("g:a", 0L, 100L, "17"))).ordinalOf("99"));
    }

    @Test
    public void laneOf_aModuleThatIsNotInTheBuild_reportsNoLane()
    {
        assertEquals(-1, WorkerLanes.of(List.of(module("g:a", 0L, 100L, "w-1"))).laneOf("g:ghost"));
    }

    @Test
    public void count_aBuildWithNoModules_stillLeavesOneLaneToDrawInto()
    {
        assertEquals(1, WorkerLanes.of(List.of()).count());
    }

    private static ModuleTiming module(String name, long start, long duration, String worker)
    {
        return new ModuleTiming(name, start, duration, worker);
    }
}
