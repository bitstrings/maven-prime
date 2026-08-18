package org.bitstrings.idea.plugins.mavenprime.context;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BuildContextPropertyTest
{
    @Test
    public void appliesToImport_aFreshProperty_isOffSoAddDoesNotTouchTheIdeImport()
    {
        assertFalse(
            "Add seeds its dialog with a fresh property, so a default of on silently opts every new "
                + "property into re-importing the Maven project",
            new BuildContextProperty().appliesToImport);
    }

    @Test
    public void appliesToImport_aPropertyTheTeamDeclares_keepsWhateverTheCallerAsked()
    {
        assertTrue(
            "the shared layer passes the flag explicitly, so the field default must not override it",
            BuildContextProperty.of("skipTests", "true", true).appliesToImport);
    }
}
