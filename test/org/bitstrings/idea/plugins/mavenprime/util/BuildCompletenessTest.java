package org.bitstrings.idea.plugins.mavenprime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BuildCompletenessTest
{
    @Test
    public void isComplete_aBuildThatLostNothing_reportsTheDataAsTrustworthy()
    {
        assertTrue(new BuildCompleteness().isComplete());
    }

    @Test
    public void isComplete_aBuildThatDroppedEvents_refusesToCallTheDataComplete()
    {
        BuildCompleteness completeness = new BuildCompleteness();

        completeness.recordDropped(1L);

        assertFalse(completeness.isComplete());
    }

    @Test
    public void isComplete_aBuildThatEndedBeforeItsEventsArrived_refusesToCallTheDataComplete()
    {
        BuildCompleteness completeness = new BuildCompleteness();

        completeness.recordTruncated();

        assertFalse(completeness.isComplete());
    }

    @Test
    public void recordDropped_severalReportsFromOneBuild_addsThemUp()
    {
        BuildCompleteness completeness = new BuildCompleteness();

        completeness.recordDropped(3L);
        completeness.recordDropped(4L);

        assertEquals(7L, completeness.getDroppedEvents());
    }

    @Test
    public void recordDropped_aReportOfZero_leavesTheDataTrustworthy()
    {
        BuildCompleteness completeness = new BuildCompleteness();

        completeness.recordDropped(0L);

        assertTrue(completeness.isComplete());
    }
}
