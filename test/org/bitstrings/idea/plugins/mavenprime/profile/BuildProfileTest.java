package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary.Concurrency;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ReactorEdge;
import org.junit.Test;

public class BuildProfileTest
{
    @Test
    public void summarize_noTimings_reportsAnEmptySummary()
    {
        assertEquals(BuildProfileSummary.empty(), new BuildProfile().summarize());
    }

    @Test
    public void summarize_modulesThatNeverOverlap_reportsTheBuildAsSequential()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 100L, 100L));

        assertEquals(Concurrency.SEQUENTIAL, profile.summarize().concurrency());
    }

    @Test
    public void summarize_wallClockMatchingTheCriticalPath_reportsDependencyBound()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 100L, 200L));
        profile.accept(new ModuleTiming("g:c", 100L, 50L));
        profile.accept(new ReactorEdge("g:b", "g:a"));
        profile.accept(new ReactorEdge("g:c", "g:a"));

        assertEquals(Concurrency.DEPENDENCY_BOUND, profile.summarize().concurrency());
    }

    @Test
    public void summarize_wallClockFarAboveTheCriticalPath_reportsThreadStarvation()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 0L, 100L));
        profile.accept(new ModuleTiming("g:c", 100L, 100L));

        assertEquals(Concurrency.THREAD_STARVED, profile.summarize().concurrency());
    }

    @Test
    public void summarize_perModuleThreadNames_reportsConcurrencyFromOverlapNotThreadNames()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:reactor", 0L, 90L));
        profile.accept(new ModuleTiming("g:core", 94L, 438L));
        profile.accept(new ModuleTiming("g:app", 532L, 45L));

        assertEquals(1, profile.summarize().laneCount());
    }

    @Test
    public void summarize_twoModulesInParallel_measuresWallClockNotTheSumOfWork()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 0L, 100L));

        BuildProfileSummary summary = profile.summarize();

        assertEquals(100L, summary.wallClockMillis());
        assertEquals(200L, summary.totalWorkMillis());
    }

    @Test
    public void summarize_dependentChain_reportsTheChainAsTheCriticalPath()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 100L, 200L));
        profile.accept(new ModuleTiming("g:idle", 0L, 10L));
        profile.accept(new ReactorEdge("g:b", "g:a"));

        BuildProfileSummary summary = profile.summarize();

        assertEquals(List.of("g:a", "g:b"), summary.criticalPath());
        assertEquals(300L, summary.criticalPathMillis());
    }

    @Test
    public void summarize_strictChain_reportsThatOneThreadIsAllTheGraphCanUse()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 100L, 100L));
        profile.accept(new ReactorEdge("g:b", "g:a"));

        assertEquals(1, profile.summarize().usefulThreads());
    }

    @Test
    public void summarize_fourIndependentModules_reportsThatFourThreadsCouldBeUsed()
    {
        BuildProfile profile = new BuildProfile();

        for (String module : List.of("g:a", "g:b", "g:c", "g:d"))
        {
            profile.accept(new ModuleTiming(module, 0L, 100L));
        }

        assertEquals(4, profile.summarize().usefulThreads());
    }

    @Test
    public void summarize_scheduleAtTheGrahamBound_reportsNoSchedulingLoss()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 0L, 100L));

        assertEquals(0L, profile.summarize().schedulingLossMillis());
    }

    @Test
    public void summarize_idleGapBetweenIndependentModules_isReportedAsSchedulingLoss()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 0L, 100L));
        profile.accept(new ModuleTiming("g:c", 300L, 100L));

        assertEquals(250L, profile.summarize().schedulingLossMillis());
    }

    @Test
    public void summarize_criticalPathLongerThanTheWorkShare_isDependencyBoundWhateverTheRatio()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 1000L));
        profile.accept(new ModuleTiming("g:b", 1000L, 1000L));
        profile.accept(new ModuleTiming("g:c", 1000L, 10L));
        profile.accept(new ReactorEdge("g:b", "g:a"));
        profile.accept(new ReactorEdge("g:c", "g:a"));

        assertEquals(Concurrency.DEPENDENCY_BOUND, profile.summarize().concurrency());
    }

    @Test
    public void getMojos_moduleWithRecordedMojos_returnsOnlyThatModulesMojos()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ProfileEvent.MojoTiming("g:a", "compiler:compile", "default", 0L, 40L));
        profile.accept(new ProfileEvent.MojoTiming("g:b", "surefire:test", "default", 0L, 90L));

        assertEquals(1, profile.getMojos("g:a").size());
    }

    @Test
    public void isEmpty_onlyReactorEdgesRecorded_staysEmpty()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ReactorEdge("g:b", "g:a"));

        assertTrue(profile.isEmpty());
    }
}
