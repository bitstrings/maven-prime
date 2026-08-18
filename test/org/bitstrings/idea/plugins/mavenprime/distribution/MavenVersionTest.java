package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MavenVersionTest
{
    @Test
    public void getMajor_threeSegments_readsTheLeadingSegment()
    {
        assertEquals(3, MavenVersion.parse("3.9.12").getMajor());
    }

    @Test
    public void getMajor_qualifiedVersion_readsTheLeadingSegment()
    {
        assertEquals(4, MavenVersion.parse("4.0.0-rc-3").getMajor());
    }

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
    public void isAtLeast_maven4AgainstMajor4_isTrue()
    {
        assertTrue(MavenVersion.parse("4.0.0").isAtLeast(4));
    }

    @Test
    public void isAtLeast_maven3AgainstMajor4_isFalse()
    {
        assertFalse(MavenVersion.parse("3.9.12").isAtLeast(4));
    }

    @Test
    public void isAtLeast_unknownVersion_isFalse()
    {
        assertFalse(MavenVersion.UNKNOWN.isAtLeast(3));
    }

    @Test
    public void isAtLeast_releaseCandidateOfTheRequiredMajor_isTrue()
    {
        assertTrue(MavenVersion.parse("4.0.0-rc-5").isAtLeast(4));
    }

    @Test
    public void isAtLeast_higherMinorThanRequired_isTrue()
    {
        assertTrue(MavenVersion.parse("3.9.12").isAtLeast(3, 9));
    }

    @Test
    public void isAtLeast_lowerMinorThanRequired_isFalse()
    {
        assertFalse(MavenVersion.parse("3.8.8").isAtLeast(3, 9));
    }

    @Test
    public void isAtLeast_preReleaseOfTheRequiredMinor_isTrue()
    {
        assertTrue(MavenVersion.parse("3.9.0-alpha-1").isAtLeast(3, 9));
    }

    @Test
    public void getMinor_qualifiedMinorSegment_readsTheLeadingDigits()
    {
        assertEquals(9, MavenVersion.parse("3.9-SNAPSHOT").getMinor());
    }

    @Test
    public void getMinor_versionWithoutAMinorSegment_isZero()
    {
        assertEquals(0, MavenVersion.parse("4").getMinor());
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
