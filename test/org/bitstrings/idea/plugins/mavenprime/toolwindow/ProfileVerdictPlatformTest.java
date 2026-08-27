package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ReactorEdge;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ProfileVerdictPlatformTest
    extends BasePlatformTestCase
{
    private static final String THREADS = "-T";

    public void testSentenceOf_aBuildOfOneModule_doesNotAdviseAThreadCount()
    {
        assertFalse(
            "the sequential advice renders as -T 1 bringing the wall clock down to the wall clock, "
                + "which tells the reader to change nothing",
            ProfileVerdict.sentenceOf(oneModule()).contains(THREADS));
    }

    public void testSentenceOf_aBuildOfOneModule_saysWhyThereIsNothingToCompare()
    {
        assertEquals(
            MavenPrimeBundle.message("mavenprime.profile.verdict.oneModule"),
            ProfileVerdict.sentenceOf(oneModule()));
    }

    public void testSentenceOf_aChainRunOneModuleAtATime_namesTheChainRatherThanAThreadCount()
    {
        assertEquals(
            MavenPrimeBundle.message("mavenprime.profile.verdict.chain", Integer.valueOf(3)),
            ProfileVerdict.sentenceOf(chainOfThree()));
    }

    public void testSentenceOf_aReactorThatCouldOverlapButRanOnOneThread_stillAdvisesTheThreadCount()
    {
        assertTrue(
            "this is the one case the advice is for, and suppressing it would cost the whole verdict",
            ProfileVerdict.sentenceOf(parallelisableButSequential()).contains(THREADS));
    }

    public void testBadgeOf_aReactorThatCannotOverlap_saysThereIsNothingToRunInParallel()
    {
        assertEquals(
            MavenPrimeBundle.message("mavenprime.profile.badge.noParallelism"),
            ProfileVerdict.badgeOf(oneModule()));
    }

    public void testBadgeOf_aReactorThatCouldOverlap_stillReportsThatNothingDid()
    {
        assertEquals(
            MavenPrimeBundle.message("mavenprime.profile.badge.sequential"),
            ProfileVerdict.badgeOf(parallelisableButSequential()));
    }

    private static BuildProfileSummary oneModule()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("org.example:only", 0L, 6029L));

        return profile.summarize();
    }

    private static BuildProfileSummary chainOfThree()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 100L, 100L));
        profile.accept(new ModuleTiming("g:c", 200L, 100L));
        profile.accept(new ReactorEdge("g:b", "g:a"));
        profile.accept(new ReactorEdge("g:c", "g:b"));

        return profile.summarize();
    }

    // Three modules that depend on nothing, run back to back, so the graph allows three at once.
    private static BuildProfileSummary parallelisableButSequential()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:a", 0L, 100L));
        profile.accept(new ModuleTiming("g:b", 100L, 100L));
        profile.accept(new ModuleTiming("g:c", 200L, 100L));

        return profile.summarize();
    }
}
