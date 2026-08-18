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

        history.start("first", "one");
        history.start("second", "two");

        assertEquals("two", history.getSelectedData());
    }

    @Test
    public void start_secondBuild_keepsTheFirstBuildAvailable()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("first", "one");
        history.start("second", "two");

        assertEquals(2, history.getEntries().size());
    }

    @Test
    public void select_anEarlierBuild_showsThatBuildInstead()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("first", "one");
        history.start("second", "two");

        assertTrue(history.select(history.getEntries().get(0)));
        assertEquals("one", history.getSelectedData());
    }

    @Test
    public void select_theAlreadySelectedBuild_reportsNoChange()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("only", "one");

        assertFalse(history.select(history.getEntries().get(0)));
    }

    @Test
    public void getSelectedData_noBuildYet_isTheEmptyValue()
    {
        assertEquals("empty", new BuildHistory<>(() -> "empty").getSelectedData());
    }

    @Test
    public void start_beyondTheRetentionLimit_dropsTheOldest()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        for (int index = 0; index <= BuildHistory.MAX_RETAINED; index++)
        {
            history.start("build" + index, "data" + index);
        }

        List<BuildEntry<String>> entries = history.getEntries();

        assertEquals(BuildHistory.MAX_RETAINED, entries.size());
        assertEquals("build1", entries.get(0).name());
    }

    @Test
    public void isShowingLatest_afterSelectingAnEarlierBuild_isFalse()
    {
        BuildHistory<String> history = new BuildHistory<>(() -> "empty");

        history.start("first", "one");
        history.start("second", "two");
        history.select(history.getEntries().get(0));

        assertFalse(history.isShowingLatest());
    }
}
