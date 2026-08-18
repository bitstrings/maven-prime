package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class DaemonInstallationsTest
{
    @Test
    public void adopt_catalogStillEmpty_takesTheMigratedProjectEntries()
    {
        DaemonInstallations installations = new DaemonInstallations();

        installations.adopt(List.of("/opt/mvnd-1.0.6"));

        assertEquals(List.of("/opt/mvnd-1.0.6"), installations.getHomes());
    }

    @Test
    public void adopt_catalogPopulated_keepsWhatTheUserAlreadyHas()
    {
        DaemonInstallations installations = new DaemonInstallations();

        installations.setHomes(List.of("/opt/mvnd-1.0.6"));
        installations.adopt(List.of("/other/project/mvnd-0.9.0"));

        assertEquals(List.of("/opt/mvnd-1.0.6"), installations.getHomes());
    }

    @Test
    public void adopt_nothingToMigrate_leavesTheCatalogEmpty()
    {
        DaemonInstallations installations = new DaemonInstallations();

        installations.adopt(List.of());

        assertEquals(List.of(), installations.getHomes());
    }

    @Test
    public void setHomes_callerMutatesItsListAfterwards_doesNotAffectTheCatalog()
    {
        DaemonInstallations installations = new DaemonInstallations();

        List<String> caller = new ArrayList<>(List.of("/opt/mvnd-1.0.6"));

        installations.setHomes(caller);
        caller.clear();

        assertEquals(1, installations.getHomes().size());
    }
}
