package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class ArtifactRankingTest
{
    private static final ArtifactCoordinates LANG3 =
        new ArtifactCoordinates("org.apache.commons", "commons-lang3", "jar", StringUtils.EMPTY, "3.14.0");

    private static final ArtifactCoordinates LANG3_OLDER =
        new ArtifactCoordinates("org.apache.commons", "commons-lang3", "jar", StringUtils.EMPTY, "3.12.0");

    private static final ArtifactCoordinates COLLECTIONS =
        new ArtifactCoordinates("org.apache.commons", "commons-collections4", "jar", StringUtils.EMPTY, "4.4");

    @Test
    public void rankOf_exactArtifactId_ranksAheadOfEveryOtherMatch()
    {
        assertEquals(0, ArtifactRanking.rankOf(LANG3, "commons-lang3"));
    }

    @Test
    public void rankOf_artifactIdPrefix_ranksAheadOfAnInnerSubstring()
    {
        assertTrue(ArtifactRanking.rankOf(LANG3, "commons-l") < ArtifactRanking.rankOf(LANG3, "lang3"));
    }

    @Test
    public void rankOf_differingCase_stillMatchesTheArtifactId()
    {
        assertEquals(0, ArtifactRanking.rankOf(LANG3, "COMMONS-LANG3"));
    }

    @Test
    public void rankOf_groupAndArtifactWithoutVersion_matchesTheShortForm()
    {
        assertTrue(
            ArtifactRanking.rankOf(LANG3, "org.apache.commons:commons-lang3") != ArtifactRanking.NO_MATCH);
    }

    @Test
    public void rankOf_artifactAndVersionWithoutGroup_matchesTheShortForm()
    {
        assertTrue(ArtifactRanking.rankOf(LANG3, "commons-lang3:3.14.0") != ArtifactRanking.NO_MATCH);
    }

    @Test
    public void rankOf_fullShortFormIncludingNoExtension_matches()
    {
        assertTrue(
            ArtifactRanking.rankOf(LANG3, "org.apache.commons:commons-lang3:3.14.0")
                != ArtifactRanking.NO_MATCH);
    }

    @Test
    public void rankOf_textPresentInNoField_doesNotMatch()
    {
        assertEquals(ArtifactRanking.NO_MATCH, ArtifactRanking.rankOf(LANG3, "guava"));
    }

    @Test
    public void bestMatches_candidatesOfDifferingRank_putsTheExactArtifactIdFirst()
    {
        assertEquals(
            LANG3,
            ArtifactRanking.bestMatches(List.of(COLLECTIONS, LANG3), "commons-lang3", 10).get(0));
    }

    @Test
    public void bestMatches_moreMatchesThanTheLimit_returnsOnlyTheLimit()
    {
        assertEquals(
            1, ArtifactRanking.bestMatches(List.of(LANG3, LANG3_OLDER, COLLECTIONS), "commons", 1).size());
    }

    @Test
    public void bestMatches_equalRank_ordersByTheArtifactComparator()
    {
        assertEquals(
            List.of(COLLECTIONS, LANG3),
            ArtifactRanking.bestMatches(List.of(LANG3, COLLECTIONS), "org.apache.commons", 10));
    }

    @Test
    public void bestMatches_limitSmallerThanTheMatches_keepsTheBestRatherThanTheFirstSeen()
    {
        assertEquals(
            List.of(LANG3),
            ArtifactRanking.bestMatches(List.of(COLLECTIONS, LANG3_OLDER, LANG3), "commons-lang3", 1));
    }

    @Test
    public void bestMatches_limitSmallerThanTheMatches_selectsTheSameHeadAsRankingEverything()
    {
        List<ArtifactCoordinates> candidates = List.of(COLLECTIONS, LANG3_OLDER, LANG3);

        assertEquals(
            ArtifactRanking.bestMatches(candidates, "commons", 10).subList(0, 2),
            ArtifactRanking.bestMatches(candidates, "commons", 2));
    }

    @Test
    public void bestMatches_zeroLimit_returnsNothing()
    {
        assertTrue(ArtifactRanking.bestMatches(List.of(LANG3), "commons-lang3", 0).isEmpty());
    }

    @Test
    public void bestMatches_noCandidateMatches_returnsNothing()
    {
        assertTrue(ArtifactRanking.bestMatches(List.of(LANG3, COLLECTIONS), "guava", 10).isEmpty());
    }
}
