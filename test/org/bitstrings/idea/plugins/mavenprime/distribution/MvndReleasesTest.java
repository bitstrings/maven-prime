package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class MvndReleasesTest
{
    private static final String INDEX =
        "<html><head><title>Index of /maven/mvnd</title></head><body>"
            + "<a href=\"../\">../</a>"
            + "<a href=\"0.9.0/\">0.9.0/</a>"
            + "<a href=\"1.0.2/\">1.0.2/</a>"
            + "<a href=\"1.0.10/\">1.0.10/</a>"
            + "<a href=\"KEYS\">KEYS</a>"
            + "</body></html>";

    @Test
    public void parse_apacheDirectoryIndex_keepsOnlyVersionDirectories()
    {
        assertEquals(List.of("1.0.10", "1.0.2", "0.9.0"), MvndReleases.parse(INDEX));
    }

    @Test
    public void parse_versionsThatSortWrongLexicographically_ordersByVersionNewestFirst()
    {
        assertEquals("1.0.10", MvndReleases.parse(INDEX).get(0));
    }

    @Test
    public void parse_parentLink_isNotOfferedAsAVersion()
    {
        assertTrue(MvndReleases.parse(INDEX).stream().noneMatch(version -> version.contains("..")));
    }

    @Test
    public void parse_nonVersionFileEntry_isIgnored()
    {
        assertTrue(MvndReleases.parse(INDEX).stream().noneMatch("KEYS"::equals));
    }

    @Test
    public void parse_duplicateEntriesInTheIndex_areListedOnce()
    {
        assertEquals(
            List.of("1.0.6"),
            MvndReleases.parse("<a href=\"1.0.6/\">1.0.6/</a><a href=\"1.0.6/\">1.0.6/</a>"));
    }

    @Test
    public void parse_unreachableIndex_yieldsNothingSoTheCallerCanFallBack()
    {
        assertEquals(List.of(), MvndReleases.parse(null));
    }

    @Test
    public void parse_indexWithoutAnyVersionDirectory_yieldsNothing()
    {
        assertEquals(List.of(), MvndReleases.parse("<html><body>no releases here</body></html>"));
    }

    @Test
    public void newestStable_theNewestReleaseIsACandidate_offersTheNewestFinalReleaseInstead()
    {
        assertEquals(
            "a release candidate must never be the default download",
            "1.0.2",
            MvndReleases.newestStable(List.of("2.0.0-rc-3", "1.0.2", "1.0.1")));
    }

    @Test
    public void newestStable_everyReleaseIsFinal_offersTheNewestOne()
    {
        assertEquals("1.0.2", MvndReleases.newestStable(List.of("1.0.2", "1.0.1")));
    }

    @Test
    public void newestStable_nothingButPrereleases_stillOffersSomethingToDownload()
    {
        assertEquals("2.0.0-rc-3", MvndReleases.newestStable(List.of("2.0.0-rc-3")));
    }

    @Test
    public void isStable_aMilestoneOrSnapshotBuild_isNotOfferedAsTheDefault()
    {
        assertFalse(MvndReleases.isStable("2.0.0-m1"));
        assertFalse(MvndReleases.isStable("1.9.0-SNAPSHOT"));
        assertFalse(MvndReleases.isStable("1.0.0-beta-2"));
    }
}
