package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class RemoteArtifactIndexTest
{
    @Test
    public void newestFirst_versionsOutOfOrder_putsTheNewestFirst()
    {
        assertEquals(
            List.of("1.10", "1.9", "1.2"),
            RemoteArtifactIndex.newestFirst(Set.of("1.9", "1.2", "1.10")));
    }

    @Test
    public void newestFirst_noVersions_returnsNothing()
    {
        assertTrue(RemoteArtifactIndex.newestFirst(Set.of()).isEmpty());
    }

    @Test
    public void newestFirst_nullFromAnUnavailableIndex_returnsNothing()
    {
        assertTrue(RemoteArtifactIndex.newestFirst(null).isEmpty());
    }

    @Test
    public void newestNewerThan_aNewerVersionExists_reportsTheNewestOne()
    {
        assertEquals(
            "3.0", RemoteArtifactIndex.newestNewerThan(List.of("3.0", "2.5", "2.0"), "2.0"));
    }

    @Test
    public void newestNewerThan_currentIsAlreadyTheNewest_reportsNothing()
    {
        assertNull(RemoteArtifactIndex.newestNewerThan(List.of("3.0", "2.5"), "3.0"));
    }

    @Test
    public void newestNewerThan_currentIsNewerThanAnythingIndexed_reportsNothing()
    {
        assertNull(RemoteArtifactIndex.newestNewerThan(List.of("3.0", "2.5"), "4.0"));
    }

    @Test
    public void newestNewerThan_blankCurrentVersion_reportsNothing()
    {
        assertNull(RemoteArtifactIndex.newestNewerThan(List.of("3.0"), StringUtils.EMPTY));
    }

    @Test
    public void matches_needleInsideTheArtifactId_matches()
    {
        assertTrue(RemoteArtifactIndex.matches("org.apache.commons", "commons-lang3", "lang"));
    }

    @Test
    public void matches_differingCase_stillMatches()
    {
        assertTrue(RemoteArtifactIndex.matches("org.apache.commons", "commons-lang3", "LANG3"));
    }

    @Test
    public void matches_groupAndArtifactForm_matches()
    {
        assertTrue(
            RemoteArtifactIndex.matches(
                "org.apache.commons", "commons-lang3", "org.apache.commons:commons-lang3"));
    }

    @Test
    public void matches_groupIdAloneWithoutTheArtifact_doesNotMatch()
    {
        assertFalse(RemoteArtifactIndex.matches("org.apache.commons", "commons-lang3", "guava"));
    }
}
