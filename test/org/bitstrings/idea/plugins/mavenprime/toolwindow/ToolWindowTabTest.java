package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ToolWindowTabTest
{
    @Test
    public void visible_whenBuildsRunOnTheDaemon_containsEveryTab()
    {
        assertEquals(Arrays.asList(ToolWindowTab.values()), ToolWindowTab.visible(true));
    }

    @Test
    public void visible_whenBuildsDoNotRunOnTheDaemon_omitsOnlyTheDaemonsTab()
    {
        List<ToolWindowTab> expected = new ArrayList<>(Arrays.asList(ToolWindowTab.values()));

        expected.remove(ToolWindowTab.DAEMONS);

        assertEquals(expected, ToolWindowTab.visible(false));
    }

    @Test
    public void visible_whenBuildsDoNotRunOnTheDaemon_stillPositionsTabsBeyondItsOrdinal()
    {
        List<ToolWindowTab> visible = ToolWindowTab.visible(false);

        assertTrue(visible.contains(ToolWindowTab.REPOSITORY));
        assertFalse(visible.contains(ToolWindowTab.DAEMONS));
        assertEquals(ToolWindowTab.REPOSITORY, visible.get(visible.indexOf(ToolWindowTab.REPOSITORY)));
    }
}
