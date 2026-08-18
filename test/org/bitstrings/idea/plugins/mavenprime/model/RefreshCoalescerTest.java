package org.bitstrings.idea.plugins.mavenprime.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RefreshCoalescerTest
{
    @Test
    public void begin_theFirstRequest_startsAPass()
    {
        assertTrue("nothing was running, so the caller has to be told to go", new RefreshCoalescer().begin());
    }

    @Test
    public void begin_aRequestWhileAPassIsRunning_doesNotStartASecondPass()
    {
        RefreshCoalescer coalescer = new RefreshCoalescer();

        coalescer.begin();

        assertFalse(
            "two Maven runs against the same reactor race each other for the model",
            coalescer.begin());
    }

    @Test
    public void isRunning_whileAPassIsInFlight_reportsBusy()
    {
        RefreshCoalescer coalescer = new RefreshCoalescer();

        coalescer.begin();

        assertTrue("the toolbar reads this to show the refresh is under way", coalescer.isRunning());
    }

    @Test
    public void end_aPassNobodyInterrupted_owesNothingFurther()
    {
        RefreshCoalescer coalescer = new RefreshCoalescer();

        coalescer.begin();

        assertFalse("re-running with nothing changed spends a Maven run for nothing", coalescer.end());
    }

    @Test
    public void end_aRequestThatArrivedDuringThePass_owesAnotherPass()
    {
        RefreshCoalescer coalescer = new RefreshCoalescer();

        coalescer.begin();
        coalescer.begin();

        assertTrue(
            "dropping it leaves the model describing the context as it was before the change",
            coalescer.end());
    }

    @Test
    public void end_severalRequestsDuringOnePass_owesOnlyOnePassForAllOfThem()
    {
        RefreshCoalescer coalescer = new RefreshCoalescer();

        coalescer.begin();
        coalescer.begin();
        coalescer.begin();
        coalescer.begin();

        coalescer.end();
        coalescer.begin();

        assertFalse(
            "queuing one pass per request makes a burst of clicks cost a Maven run each",
            coalescer.end());
    }

    @Test
    public void begin_afterThePreviousPassEnded_startsAgain()
    {
        RefreshCoalescer coalescer = new RefreshCoalescer();

        coalescer.begin();
        coalescer.end();

        assertTrue("a finished pass must not latch the gate shut for the rest of the session",
            coalescer.begin());
    }
}
