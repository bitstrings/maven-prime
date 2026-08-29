package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MavenVersionTest
{
    @Test
    public void parse_blankText_yieldsUnknown()
    {
        assertFalse(MavenVersion.parse("  ").isKnown());
    }

    @Test
    public void parse_nonNumericText_yieldsUnknown()
    {
        assertFalse(MavenVersion.parse("not-a-version").isKnown());
    }

    @Test
    public void parse_qualifiedReleaseCandidate_isKnown()
    {
        assertTrue(MavenVersion.parse("4.0.0-rc-6").isKnown());
    }

    @Test
    public void toString_knownVersionWithQualifier_rendersTheFullVersion()
    {
        assertEquals("4.0.0-rc-3", MavenVersion.parse("4.0.0-rc-3").toString());
    }

    @Test
    public void toString_unknownVersion_rendersEmpty()
    {
        assertEquals("", MavenVersion.UNKNOWN.toString());
    }

    @Test
    public void equals_sameText_isEqual()
    {
        assertEquals(MavenVersion.parse("3.9.12"), MavenVersion.parse("3.9.12"));
        assertEquals(MavenVersion.parse("3.9.12").hashCode(), MavenVersion.parse("3.9.12").hashCode());
    }
}
