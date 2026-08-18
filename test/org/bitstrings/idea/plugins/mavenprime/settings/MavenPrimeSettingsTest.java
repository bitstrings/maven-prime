package org.bitstrings.idea.plugins.mavenprime.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MavenPrimeSettingsTest
{
    @Test
    public void autoRefresh_settingsNeverConfigured_reloadsTheMavenProject()
    {
        assertTrue(new MavenPrimeSettings().autoRefresh);
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
