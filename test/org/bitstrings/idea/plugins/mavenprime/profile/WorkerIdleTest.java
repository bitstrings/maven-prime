package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.WorkerIdle.IdleGap;
import org.junit.Test;

public class WorkerIdleTest
{
    private static final Map<String, Set<String>> NO_EDGES = Map.of();

    @Test
    public void gaps_aWorkerThatWaitedBetweenTwoModules_measuresTheWait()
    {
        List<ModuleTiming> modules =
            List.of(module("g:a", 0L, 100L, "w-1"), module("g:b", 250L, 50L, "w-1"));

        assertEquals(150L, gapsOf(modules, NO_EDGES).get(0).durationMillis());
    }

    @Test
    public void gaps_modulesRunningBackToBack_reportNoWaitAtAll()
    {
        List<ModuleTiming> modules =
            List.of(module("g:a", 0L, 100L, "w-1"), module("g:b", 100L, 50L, "w-1"));

        assertTrue(gapsOf(modules, NO_EDGES).isEmpty());
    }

    @Test
    public void gaps_lanesPackedByOverlapRatherThanRead_reportNothing()
    {
        List<ModuleTiming> modules =
            List.of(new ModuleTiming("g:a", 0L, 100L), new ModuleTiming("g:b", 250L, 50L));

        assertTrue(
            "a lane the packer invented has no worker behind it, so a gap in it is an artefact of the "
                + "packing rather than a thread that had nothing to run",
            gapsOf(modules, NO_EDGES).isEmpty());
    }

    @Test
    public void gaps_theModuleAfterTheWait_namesTheUpstreamThatFinishedLast()
    {
        List<ModuleTiming> modules =
            List.of(
                module("g:a", 0L, 100L, "w-1"),
                module("g:slow", 0L, 240L, "w-2"),
                module("g:b", 250L, 50L, "w-1"));

        assertEquals(
            "g:slow",
            gapsOf(modules, Map.of("g:b", Set.of("g:a", "g:slow"))).get(0).lastUpstream());
    }

    @Test
    public void gaps_aWaitBeforeAModuleWithNoUpstreams_namesNothingRatherThanGuessing()
    {
        List<ModuleTiming> modules =
            List.of(module("g:a", 0L, 100L, "w-1"), module("g:b", 250L, 50L, "w-1"));

        assertEquals("", gapsOf(modules, NO_EDGES).get(0).lastUpstream());
    }

    @Test
    public void gaps_aGapOnTheSecondWorker_carriesTheLaneItBelongsTo()
    {
        List<ModuleTiming> modules =
            List.of(
                module("g:a", 0L, 300L, "w-1"),
                module("g:b", 0L, 100L, "w-2"),
                module("g:c", 250L, 50L, "w-2"));

        assertEquals(
            "the tooltip names the worker from this lane, so a gap filed under the wrong one blames "
                + "the wrong thread",
            1,
            gapsOf(modules, NO_EDGES).get(0).lane());
    }

    @Test
    public void totalMillis_severalWaits_addsThemUp()
    {
        List<ModuleTiming> modules =
            List.of(
                module("g:a", 0L, 100L, "w-1"),
                module("g:b", 150L, 50L, "w-1"),
                module("g:c", 300L, 50L, "w-1"));

        assertEquals(150L, WorkerIdle.totalMillis(gapsOf(modules, NO_EDGES)));
    }

    private static List<IdleGap> gapsOf(List<ModuleTiming> modules, Map<String, Set<String>> upstreams)
    {
        return WorkerIdle.gaps(modules, WorkerLanes.of(modules, List.of()), upstreams);
    }

    private static ModuleTiming module(String name, long start, long duration, String worker)
    {
        return new ModuleTiming(name, start, duration, worker);
    }
}
