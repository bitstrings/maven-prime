package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactDownloading;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.junit.Test;

public class ArtifactCandidatesTest
{
    @Test
    public void ofArtifacts_dependenciesInDeclarationOrder_ordersThemByArtifactId()
    {
        List<ArtifactCoordinates> coordinates =
            ArtifactCandidates.ofArtifacts(
                List.of(artifact("org.example", "zebra"), artifact("org.example", "ant")));

        assertEquals(List.of("ant", "zebra"), artifactIdsIn(coordinates));
    }

    @Test
    public void ofPlugins_aPluginWithNoVersion_isLeftOutBecauseItCannotBeLocated()
    {
        assertTrue(ArtifactCandidates.ofPlugins(List.of(plugin("org.example", "widget", null))).isEmpty());
    }

    @Test
    public void ofPlugins_aPluginWithABlankVersion_isLeftOutBecauseItCannotBeLocated()
    {
        assertTrue(ArtifactCandidates.ofPlugins(List.of(plugin("org.example", "widget", "  "))).isEmpty());
    }

    @Test
    public void ofPlugins_aVersionedPlugin_isKept()
    {
        assertEquals(
            List.of("widget"),
            artifactIdsIn(ArtifactCandidates.ofPlugins(List.of(plugin("org.example", "widget", "1.0")))));
    }

    @Test
    public void ofResolutionEvents_theSameArtifactDownloadedTwice_isListedOnce()
    {
        assertEquals(
            1,
            ArtifactCandidates
                .ofResolutionEvents(
                    List.of(downloading("org.example:widget:jar:1.0"), downloading("org.example:widget:jar:1.0")))
                .size());
    }

    @Test
    public void ofResolutionEvents_anEventWhoseCoordinatesCannotBeParsed_isSkippedRatherThanThrowing()
    {
        assertTrue(ArtifactCandidates.ofResolutionEvents(List.of(downloading(""))).isEmpty());
    }

    @Test
    public void ofResolutionEvents_severalArtifacts_keepsTheOrderTheBuildResolvedThem()
    {
        assertEquals(
            List.of("second", "first"),
            artifactIdsIn(
                ArtifactCandidates.ofResolutionEvents(
                    List.of(downloading("org.example:second:jar:1.0"), downloading("org.example:first:jar:1.0")))));
    }

    @Test
    public void matching_aBlankNeedle_returnsTheCandidatesUntouched()
    {
        List<ArtifactCoordinates> candidates = ArtifactCandidates.ofArtifacts(List.of(artifact("g", "a")));

        assertSame(candidates, ArtifactCandidates.matching(candidates, "   "));
    }

    @Test
    public void matching_aNeedleInADifferentCase_stillMatches()
    {
        assertEquals(
            1,
            ArtifactCandidates
                .matching(ArtifactCandidates.ofArtifacts(List.of(artifact("org.example", "Widget"))), "widget")
                .size());
    }

    @Test
    public void matching_aNeedleNothingCarries_returnsNothing()
    {
        assertTrue(
            ArtifactCandidates
                .matching(ArtifactCandidates.ofArtifacts(List.of(artifact("org.example", "widget"))), "absent")
                .isEmpty());
    }

    private static List<String> artifactIdsIn(List<ArtifactCoordinates> coordinates)
    {
        return coordinates.stream().map(ArtifactCoordinates::artifactId).toList();
    }

    private static MavenArtifact artifact(String groupId, String artifactId)
    {
        return new MavenArtifact(
            groupId, artifactId, "1.0", "1.0", "jar", null, "compile", false, "jar", null, null, false, false);
    }

    private static MavenPlugin plugin(String groupId, String artifactId, String version)
    {
        return new MavenPlugin(groupId, artifactId, version, false, false, null, List.of(), List.of());
    }

    private static ResolutionEvent downloading(String coordinates)
    {
        return new ArtifactDownloading(coordinates, "central", "https://repo.example");
    }
}
