package org.bitstrings.idea.plugins.mavenprime.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory;
import org.junit.Test;

public class MavenPrimeSettingsTest
{
    @Test
    public void autoRefresh_settingsNeverConfigured_reloadsTheMavenProject()
    {
        assertTrue(new MavenPrimeSettings().autoRefresh);
    }

    @Test
    public void retainedBuilds_settingsNeverConfigured_keepsTwentyRuns()
    {
        assertEquals(BuildHistory.DEFAULT_RETAINED, new MavenPrimeSettings().retainedBuilds);
    }

    @Test
    public void loadState_aLoweredRetention_keepsTheLoweredCount()
    {
        MavenPrimeSettings stored = new MavenPrimeSettings();

        stored.retainedBuilds = 5;

        MavenPrimeSettings loaded = new MavenPrimeSettings();

        loaded.loadState(stored);

        assertEquals(
            "a retention the reader lowered has to survive reopening the project",
            5,
            loaded.retainedBuilds);
    }

    @Test
    public void loadState_autoRefreshTurnedOff_keepsItOff()
    {
        MavenPrimeSettings stored = new MavenPrimeSettings();

        stored.autoRefresh = false;

        MavenPrimeSettings loaded = new MavenPrimeSettings();

        loaded.loadState(stored);

        assertFalse(loaded.autoRefresh);
    }
}
