package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class IndexedArtifactsTest
{
    private static final ArtifactCoordinates LANG3_14 = version("org.apache.commons", "commons-lang3", "3.14.0");

    private static final ArtifactCoordinates LANG3_12 = version("org.apache.commons", "commons-lang3", "3.12.0");

    private static final ArtifactCoordinates GUAVA = version("com.google.guava", "guava", "33.0");

    private static final ArtifactCoordinates OTHER_LANG3 = version("org.other", "commons-lang3", "1.0");

    @Test
    public void versionsOf_artifactWithSeveralVersions_reportsThemAll()
    {
        assertEquals(
            List.of(LANG3_14, LANG3_12),
            indexOf(LANG3_14, LANG3_12, GUAVA).versionsOf(LANG3_14));
    }

    @Test
    public void versionsOf_artifactWithSeveralVersions_keepsTheScanOrderNewestFirst()
    {
        assertEquals("3.14.0", indexOf(LANG3_14, LANG3_12).versionsOf(LANG3_12).get(0).version());
    }

    @Test
    public void versionsOf_sameArtifactIdInAnotherGroup_doesNotLeakAcrossGroups()
    {
        assertEquals(List.of(OTHER_LANG3), indexOf(LANG3_14, OTHER_LANG3).versionsOf(OTHER_LANG3));
    }

    @Test
    public void versionsOf_artifactAbsentFromTheIndex_reportsNothing()
    {
        assertTrue(indexOf(LANG3_14).versionsOf(GUAVA).isEmpty());
    }

    @Test
    public void versionsOf_emptyIndex_reportsNothing()
    {
        assertTrue(IndexedArtifacts.EMPTY.versionsOf(LANG3_14).isEmpty());
    }

    @Test
    public void versionsOf_agreesWithAFullScanOfTheSameEntries()
    {
        List<ArtifactCoordinates> scanned = List.of(LANG3_14, GUAVA, LANG3_12, OTHER_LANG3);

        assertEquals(scannedVersionsOf(scanned, LANG3_14), IndexedArtifacts.of(scanned).versionsOf(LANG3_14));
    }

    @Test
    public void versionsOf_callerMutatingTheReturnedVersions_isRejected()
    {
        IndexedArtifacts indexed = indexOf(LANG3_14, LANG3_12);

        assertThrows(UnsupportedOperationException.class, () -> indexed.versionsOf(LANG3_14).clear());
    }

    @Test
    public void of_scannedListMutatedAfterIndexing_leavesTheIndexUnchanged()
    {
        List<ArtifactCoordinates> scanned = new ArrayList<>(List.of(LANG3_14, LANG3_12));
        IndexedArtifacts indexed = IndexedArtifacts.of(scanned);

        scanned.clear();

        assertEquals(2, indexed.all().size());
    }

    @Test
    public void all_indexedEntries_keepsEveryScannedArtifact()
    {
        assertEquals(3, indexOf(LANG3_14, LANG3_12, GUAVA).all().size());
    }

    private static IndexedArtifacts indexOf(ArtifactCoordinates... scanned)
    {
        return IndexedArtifacts.of(List.of(scanned));
    }

    private static List<ArtifactCoordinates> scannedVersionsOf(
        List<ArtifactCoordinates> scanned, ArtifactCoordinates wanted)
    {
        List<ArtifactCoordinates> versions = new ArrayList<>();

        for (ArtifactCoordinates candidate : scanned)
        {
            if (candidate.groupId().equals(wanted.groupId())
                && candidate.artifactId().equals(wanted.artifactId()))
            {
                versions.add(candidate);
            }
        }

        versions.sort(ArtifactCoordinates.BY_ARTIFACT);

        return List.copyOf(versions);
    }

    private static ArtifactCoordinates version(String groupId, String artifactId, String version)
    {
        return new ArtifactCoordinates(groupId, artifactId, "jar", StringUtils.EMPTY, version);
    }
}
