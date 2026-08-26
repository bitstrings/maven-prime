package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.junit.Test;

public class ProfileAdviceTest
{
    @Test
    public void isWorthReporting_aLongBuildStarvedOfThreads_saysSoBecauseRaisingTheThreadCountFixesIt()
    {
        assertTrue(ProfileAdvice.isWorthReporting(threadStarved(60_000L)));
    }

    @Test
    public void isWorthReporting_aShortBuildStarvedOfThreads_staysQuietRatherThanInterruptOverSeconds()
    {
        assertFalse(
            "half of a five-second build is two and a half seconds, and nobody retunes -T for that",
            ProfileAdvice.isWorthReporting(threadStarved(5_000L)));
    }

    @Test
    public void isWorthReporting_aLongBuildLosingAThinSliceOfItsWallClock_staysQuiet()
    {
        assertFalse(
            "twelve seconds off three and a half minutes is noise, and reporting it teaches the reader "
                + "to dismiss the notification unread",
            ProfileAdvice.isWorthReporting(dependencyBound(12_000L)));
    }

    @Test
    public void isWorthReporting_aDependencyBoundBuildLosingTimeToScheduling_saysSoThoughThreadsCannotHelp()
    {
        assertTrue(
            "the gap above the lower bound is not the graph, so it survives a verdict that says no "
                + "thread count beats the critical path",
            ProfileAdvice.isWorthReporting(dependencyBound(60_000L)));
    }

    @Test
    public void isWorthReporting_aDependencyBoundBuildAlreadyAtItsFloor_staysQuiet()
    {
        assertFalse(ProfileAdvice.isWorthReporting(dependencyBound(0L)));
    }

    @Test
    public void usefulThreads_aWideReactorRunOnMoreLanesThanItsAverageNeeds_fallsBelowThoseLanes()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:head", 0L, 1_000L));

        for (int module = 0; module < 20; module++)
        {
            profile.accept(new ModuleTiming("g:leaf" + module, 1_000L, 100L));
            profile.accept(new ProfileEvent.ReactorEdge("g:leaf" + module, "g:head"));
        }

        BuildProfileSummary summary = profile.summarize();

        assertTrue(
            "a build can peak above the thread count that would still reach its critical path, so "
                + "text calling that count a limit on what ran at once states the opposite of the truth",
            summary.usefulThreads() < summary.laneCount());
    }

    @Test
    public void isWorthReporting_aSequentialBuildMoreThreadsWouldShorten_saysSo()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100_000L));
        profile.accept(new ModuleTiming("g:b", 100_000L, 100_000L));

        assertTrue(
            "Maven runs single-threaded unless -T is passed, so a reactor nobody has parallelised yet "
                + "is the most common build there is and the one with the most to gain",
            ProfileAdvice.isWorthReporting(profile.summarize()));
    }

    @Test
    public void isWorthReporting_aBuildOfOneModule_staysQuietBecauseNothingCanOverlapWithIt()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:only", 0L, 600_000L));

        BuildProfileSummary summary = profile.summarize();

        assertEquals("a lone module is its own critical path", 0L, summary.recoverableMillis());
        assertFalse(ProfileAdvice.isWorthReporting(summary));
    }

    // Two lanes filled by four equal modules that nothing links, so twice the threads halve the build.
    private static BuildProfileSummary threadStarved(long wallClockMillis)
    {
        BuildProfile profile = new BuildProfile();

        long half = wallClockMillis / 2L;

        profile.accept(new ModuleTiming("g:a", 0L, half));
        profile.accept(new ModuleTiming("g:b", 0L, half));
        profile.accept(new ModuleTiming("g:c", half, half));
        profile.accept(new ModuleTiming("g:d", half, half));

        return profile.summarize();
    }

    // A chain of two long modules with a short one beside it, held apart by a gap the graph does not explain.
    private static BuildProfileSummary dependencyBound(long gapMillis)
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100_000L));
        profile.accept(new ModuleTiming("g:b", 100_000L + gapMillis, 100_000L));
        profile.accept(new ModuleTiming("g:c", 0L, 10_000L));
        profile.accept(new ProfileEvent.ReactorEdge("g:b", "g:a"));

        return profile.summarize();
    }
}
