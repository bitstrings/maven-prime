package org.bitstrings.idea.plugins.mavenprime.build;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceLog;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionLog;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class SpySessionPlatformTest
    extends BasePlatformTestCase
{
    private static final String BUILD_NAME = "spy check";

    public void testAccept_aLossReportFromTheBuild_marksTheProfileDataIncomplete()
    {
        started().accept(SpyProtocol.encode(SpyProtocol.DROPPED, "42"));

        assertEquals(42L, profileCompletenessDrops());
    }

    public void testAccept_aLossReportFromTheBuild_marksTheResolutionDataIncomplete()
    {
        started().accept(SpyProtocol.encode(SpyProtocol.DROPPED, "42"));

        assertEquals(42L, ResolutionLog.getInstance(getProject()).getCompleteness().getDroppedEvents());
    }

    public void testAccept_aLossReportFromTheBuild_marksTheProvenanceDataIncomplete()
    {
        started().accept(SpyProtocol.encode(SpyProtocol.DROPPED, "42"));

        assertEquals(42L, ProvenanceLog.getInstance(getProject()).getCompleteness().getDroppedEvents());
    }

    public void testAccept_anOrdinaryBuildEvent_leavesTheDataTrustworthy()
    {
        started().accept(SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:mod", "Mod"));

        assertTrue(
            BuildProfileService.getInstance(getProject()).getCurrent().getCompleteness().isComplete());
    }

    public void testRecordDrainOutcome_aBuildWhoseEventsNeverFinishedArriving_marksTheDataIncomplete()
    {
        started().recordDrainOutcome(false);

        assertTrue(
            BuildProfileService.getInstance(getProject()).getCurrent().getCompleteness().isTruncated());
    }

    public void testRecordDrainOutcome_aBuildThatDeliveredEverything_leavesTheDataTrustworthy()
    {
        started().recordDrainOutcome(true);

        assertTrue(
            BuildProfileService.getInstance(getProject()).getCurrent().getCompleteness().isComplete());
    }

    private SpySession started()
    {
        return SpySession.timed(getProject(), BUILD_NAME, "", null);
    }

    private long profileCompletenessDrops()
    {
        return BuildProfileService.getInstance(getProject()).getCurrent().getCompleteness().getDroppedEvents();
    }
}
