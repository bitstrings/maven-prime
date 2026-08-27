package org.bitstrings.idea.plugins.mavenprime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;
import org.junit.Test;

public class BuildHistoryTest
{
    @Test
    public void start_secondBuild_selectsTheLatestByDefault()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("first", "", "one");
        history.start("second", "", "two");

        assertEquals("two", history.getSelectedData());
    }

    @Test
    public void start_secondBuild_keepsTheFirstBuildAvailable()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("first", "", "one");
        history.start("second", "", "two");

        assertEquals(2, history.getEntries().size());
    }

    @Test
    public void select_anEarlierBuild_showsThatBuildInstead()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("first", "", "one");
        history.start("second", "", "two");

        assertTrue(history.select(history.getEntries().get(0)));
        assertEquals("one", history.getSelectedData());
    }

    @Test
    public void select_theAlreadySelectedBuild_reportsNoChange()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("only", "", "one");

        assertFalse(history.select(history.getEntries().get(0)));
    }

    @Test
    public void getSelectedData_noBuildYet_isTheEmptyValue()
    {
        assertEquals("empty", new BuildHistory<>(() -> "empty").getSelectedData());
    }

    @Test
    public void start_aHistoryToldToKeepFewer_dropsDownToThatCountAsSoonAsTheNextBuildArrives()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty", () -> 2);

        for (int index = 0; index < 5; index++)
        {
            history.start("build" + index, "", "data" + index);
        }

        assertEquals(
            "lowering the setting has to take effect without a restart, or the reader keeps paying "
                + "memory for runs they asked to forget",
            2,
            history.getEntries().size());
    }

    @Test
    public void start_aRetentionOfZero_stillKeepsTheRunItJustRecorded()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty", () -> 0);

        history.start("only", "", "data");

        assertEquals(
            "trimming to nothing removes from an empty list and throws before the run is recorded, so "
                + "a hand-edited zero would take the history down with it",
            1,
            history.getEntries().size());
    }

    @Test
    public void start_beyondTheRetentionLimit_dropsTheOldest()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        for (int index = 0; index <= BuildHistory.DEFAULT_RETAINED; index++)
        {
            history.start("build" + index, "", "data" + index);
        }

        List<BuildEntry<String>> entries = history.getEntries();

        assertEquals(BuildHistory.DEFAULT_RETAINED, entries.size());
        assertEquals("build1", entries.get(0).name());
    }

    @Test
    public void isShowingLatest_afterSelectingAnEarlierBuild_isFalse()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("first", "", "one");
        history.start("second", "", "two");
        history.select(history.getEntries().get(0));

        assertFalse(history.isShowingLatest());
    }
}
