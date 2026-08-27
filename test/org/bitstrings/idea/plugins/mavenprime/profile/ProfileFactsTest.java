package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileFacts.Measure;
import org.junit.Test;

public class ProfileFactsTest
{
    private static final double SHARE_TOLERANCE = 0.001D;

    @Test
    public void of_anyBuild_leadsWithTheWallClockBecauseThatIsTheNumberOnTheHeader()
    {
        assertEquals(Measure.WALL_CLOCK, factsOf(sequential()).get(0).measure());
    }

    @Test
    public void of_aBuildNoThreadCountCanImprove_leavesOutTheTimeThreadsWouldRecover()
    {
        assertFalse(
            "offering recoverable time on a dependency-bound build tells the reader to chase nothing",
            measuresOf(dependencyBound()).contains(Measure.RECOVERABLE));
    }

    @Test
    public void of_aBuildNoThreadCountCanImprove_stillReportsWhatWentOnSchedulingIt()
    {
        assertTrue(
            "the gap above the critical path is real, and dropping both rows hides it entirely",
            measuresOf(dependencyBound()).contains(Measure.SCHEDULING_LOSS));
    }

    @Test
    public void of_aBuildStarvedOfThreads_keepsTheTimeThreadsWouldRecover()
    {
        assertTrue(
            "raising the thread count is the whole fix for a starved build, so the row it acts on "
                + "must survive",
            measuresOf(threadStarved()).contains(Measure.RECOVERABLE));
    }

    @Test
    public void of_aBuildOfOneModule_leavesOutTheWorkRowThatWouldRepeatTheWallClock()
    {
        assertFalse(
            "a row carrying the wall clock under a second name reads as a measurement that failed, "
                + "and a one-module build produced that for every row",
            measuresOf(sequential()).contains(Measure.WORK));
    }

    @Test
    public void of_aBuildOfOneModule_leavesOutTheCriticalPathRowThatWouldRepeatItToo()
    {
        assertFalse(measuresOf(sequential()).contains(Measure.CRITICAL_PATH));
    }

    @Test
    public void of_aBuildWhoseModulesOverlapped_keepsTheWorkRowBecauseItDiffers()
    {
        assertTrue(measuresOf(threadStarved()).contains(Measure.WORK));
    }

    @Test
    public void of_aBuildWithNoSchedulingOverhead_leavesThatRowOut()
    {
        assertFalse(measuresOf(sequential()).contains(Measure.SCHEDULING_LOSS));
    }

    @Test
    public void criticalPath_aModuleOnThePath_carriesItsOwnDurationAndShare()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:slow", 0L, 80L));
        profile.accept(new ModuleTiming("g:fast", 80L, 20L));
        profile.accept(new ProfileEvent.ReactorEdge("g:fast", "g:slow"));

        BuildProfileSummary summary = profile.summarize();

        List<ProfileFacts.Step> path = ProfileFacts.criticalPath(profile, summary);

        assertTrue("the fixture has to produce a critical path", path.size() > 1);
        assertEquals(80L, path.get(0).durationMillis());
        assertEquals(80.0D, path.get(0).sharePercent(), SHARE_TOLERANCE);
    }

    @Test
    public void criticalPath_aModuleWithNoRecordedTiming_readsAsZeroRatherThanFailing()
    {
        BuildProfileSummary summary =
            new BuildProfileSummary(
                100L, 100L, 100L, List.of("g:ghost"), 1, BuildProfileSummary.Concurrency.SEQUENTIAL);

        assertEquals(0L, ProfileFacts.criticalPath(new BuildProfile(), summary).get(0).durationMillis());
    }

    private static List<Measure> measuresOf(BuildProfile profile)
    {
        return factsOf(profile).stream().map(ProfileFacts.Fact::measure).toList();
    }

    private static List<ProfileFacts.Fact> factsOf(BuildProfile profile)
    {
        return ProfileFacts.of(profile, profile.summarize());
    }

    private static BuildProfile sequential()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));

        return profile;
    }

    // A chain longer than the work spread over the lanes it ran in, started late enough to leave a gap.
    private static BuildProfile dependencyBound()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 120L, 100L));
        profile.accept(new ModuleTiming("g:c", 0L, 10L));
        profile.accept(new ProfileEvent.ReactorEdge("g:b", "g:a"));

        return profile;
    }

    // Four independent modules that only ever filled two lanes.
    private static BuildProfile threadStarved()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 0L, 100L));
        profile.accept(new ModuleTiming("g:c", 100L, 100L));
        profile.accept(new ModuleTiming("g:d", 100L, 100L));

        return profile;
    }
}
