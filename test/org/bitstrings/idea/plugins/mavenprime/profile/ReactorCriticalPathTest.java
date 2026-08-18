package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ReactorCriticalPathTest
{
    @Test
    public void longest_linearChain_returnsEveryModuleInBuildOrder()
    {
        List<String> path =
            ReactorCriticalPath.longest(
                Map.of("a", 10L, "b", 20L, "c", 30L),
                Map.of("b", Set.of("a"), "c", Set.of("b")));

        assertEquals(List.of("a", "b", "c"), path);
    }

    @Test
    public void longest_diamondWithOneHeavyBranch_followsTheHeavyBranch()
    {
        List<String> path =
            ReactorCriticalPath.longest(
                Map.of("a", 10L, "b", 5L, "c", 20L, "d", 10L),
                Map.of("b", Set.of("a"), "c", Set.of("a"), "d", Set.of("b", "c")));

        assertEquals(List.of("a", "c", "d"), path);
    }

    @Test
    public void longest_independentModules_returnsOnlyTheSlowestOne()
    {
        List<String> path =
            ReactorCriticalPath.longest(Map.of("fast", 5L, "slow", 50L, "medium", 20L), Map.of());

        assertEquals(List.of("slow"), path);
    }

    @Test
    public void longest_noModules_returnsAnEmptyPath()
    {
        assertTrue(ReactorCriticalPath.longest(Map.of(), Map.of()).isEmpty());
    }

    @Test
    public void longest_upstreamWithNoRecordedDuration_stillContributesToTheChain()
    {
        List<String> path =
            ReactorCriticalPath.longest(Map.of("child", 30L), Map.of("child", Set.of("parent")));

        assertEquals(List.of("parent", "child"), path);
    }

    @Test
    public void longest_cyclicGraph_returnsWithoutHanging()
    {
        List<String> path =
            ReactorCriticalPath.longest(
                Map.of("a", 10L, "b", 10L),
                Map.of("a", Set.of("b"), "b", Set.of("a")));

        assertTrue(path.isEmpty());
    }

    @Test
    public void weight_diamondCriticalPath_sumsOnlyThatPath()
    {
        Map<String, Long> durations = Map.of("a", 10L, "b", 5L, "c", 20L, "d", 10L);

        long weight = ReactorCriticalPath.weight(List.of("a", "c", "d"), durations);

        assertEquals(40L, weight);
    }
}
