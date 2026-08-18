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
        assertEquals(Measure.WALL_CLOCK, ProfileFacts.of(summarize()).get(0).measure());
    }

    @Test
    public void of_aBuildNoThreadCountCanImprove_leavesOutTheTimeThreadsWouldRecover()
    {
        assertFalse(
            "offering recoverable time on a dependency-bound build tells the reader to chase nothing",
            measuresOf(summarize()).contains(Measure.RECOVERABLE));
    }

    @Test
    public void of_aBuildWithNoSchedulingOverhead_leavesThatRowOut()
    {
        assertFalse(measuresOf(summarize()).contains(Measure.SCHEDULING_LOSS));
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

    private static List<Measure> measuresOf(BuildProfileSummary summary)
    {
        return ProfileFacts.of(summary).stream().map(ProfileFacts.Fact::measure).toList();
    }

    private static BuildProfileSummary summarize()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));

        return profile.summarize();
    }
}
