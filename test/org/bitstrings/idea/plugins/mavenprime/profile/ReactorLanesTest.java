package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.junit.Test;

public class ReactorLanesTest
{
    @Test
    public void count_modulesThatNeverOverlap_packsThemIntoOneLane()
    {
        List<ModuleTiming> modules =
            List.of(
                new ModuleTiming("g:a", 0L, 100L),
                new ModuleTiming("g:b", 100L, 100L),
                new ModuleTiming("g:c", 200L, 100L));

        assertEquals(1, ReactorLanes.count(modules));
    }

    @Test
    public void count_threeModulesRunningTogether_reportsThreeLanes()
    {
        List<ModuleTiming> modules =
            List.of(
                new ModuleTiming("g:a", 0L, 100L),
                new ModuleTiming("g:b", 10L, 100L),
                new ModuleTiming("g:c", 20L, 100L));

        assertEquals(3, ReactorLanes.count(modules));
    }

    @Test
    public void count_perModuleThreadNamesWithoutOverlap_doesNotInflateTheLaneCount()
    {
        List<ModuleTiming> modules =
            List.of(
                new ModuleTiming("g:reactor", 0L, 90L),
                new ModuleTiming("g:core", 94L, 438L),
                new ModuleTiming("g:app", 532L, 45L));

        assertEquals(1, ReactorLanes.count(modules));
    }

    @Test
    public void assign_moduleFreeingItsLane_reusesThatLane()
    {
        Map<String, Integer> lanes =
            ReactorLanes.assign(
                List.of(
                    new ModuleTiming("g:a", 0L, 100L),
                    new ModuleTiming("g:b", 0L, 300L),
                    new ModuleTiming("g:c", 100L, 100L)));

        assertEquals(Integer.valueOf(0), lanes.get("g:a"));
        assertEquals(Integer.valueOf(1), lanes.get("g:b"));
        assertEquals(Integer.valueOf(0), lanes.get("g:c"));
    }

    @Test
    public void count_noModules_reportsNoLanes()
    {
        assertEquals(0, ReactorLanes.count(List.of()));
    }
}
