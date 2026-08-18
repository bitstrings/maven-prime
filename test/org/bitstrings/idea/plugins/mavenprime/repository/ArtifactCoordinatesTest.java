package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class ArtifactCoordinatesTest
{
    private static final ArtifactCoordinates WIDGET =
        new ArtifactCoordinates("org.example", "widget", "jar", StringUtils.EMPTY, "1.2.3");

    @Test
    public void fileName_explicitClassifierAndExtension_namesTheSidecarArtifact()
    {
        assertEquals("widget-1.2.3-sources.jar", WIDGET.fileName("sources", "jar"));
    }

    @Test
    public void fileName_blankClassifier_omitsTheClassifierSegment()
    {
        assertEquals("widget-1.2.3.pom", WIDGET.fileName(StringUtils.EMPTY, "pom"));
    }

    @Test
    public void remotePath_jarArtifact_addressesTheVersionDirectory()
    {
        assertEquals("org/example/widget/1.2.3/widget-1.2.3.jar", WIDGET.remotePath());
    }

    @Test
    public void metadataPath_anyVersion_addressesTheArtifactLevelMetadata()
    {
        assertEquals("org/example/widget/maven-metadata.xml", WIDGET.metadataPath());
    }

    @Test
    public void byArtifact_severalVersionsOfOneArtifact_ordersTheNewestFirst()
    {
        List<ArtifactCoordinates> versions =
            new ArrayList<>(List.of(version("1.9"), version("1.10"), version("1.2")));

        versions.sort(ArtifactCoordinates.BY_ARTIFACT);

        assertEquals(List.of("1.10", "1.9", "1.2"), versionsOf(versions));
    }

    @Test
    public void parse_shortForm_readsAllThreeComponents()
    {
        assertEquals(
            "org.example:widget:1.2.3", ArtifactCoordinates.parse("org.example:widget:1.2.3").get().shortForm());
    }

    @Test
    public void parse_metadataCoordinatesWithoutAVersion_stillParses()
    {
        assertEquals("widget", ArtifactCoordinates.parse("org.example:widget:").get().artifactId());
    }

    @Test
    public void parse_groupLevelMetadataCoordinates_stillParses()
    {
        assertEquals("org.example", ArtifactCoordinates.parse("org.example::").get().groupId());
    }

    @Test
    public void parse_textThatIsNotACoordinate_yieldsNothing()
    {
        assertTrue(ArtifactCoordinates.parse("org.example").isEmpty());
    }

    @Test
    public void baseVersion_timestampedSnapshot_reportsTheSnapshotBaseVersion()
    {
        assertEquals("1.0-SNAPSHOT", version("1.0-20240101.101010-3").baseVersion());
    }

    @Test
    public void baseVersion_releaseVersion_reportsTheVersionUnchanged()
    {
        assertEquals("1.2.3", WIDGET.baseVersion());
    }

    @Test
    public void directoryIn_timestampedSnapshot_addressesTheSnapshotDirectory()
    {
        assertEquals(
            Paths.get("/repo/org/example/widget/1.0-SNAPSHOT"),
            version("1.0-20240101.101010-3").directoryIn(Paths.get("/repo")));
    }

    @Test
    public void remotePath_timestampedSnapshot_addressesTheSnapshotDirectoryAndTheTimestampedFile()
    {
        assertEquals(
            "org/example/widget/1.0-SNAPSHOT/widget-1.0-20240101.101010-3.jar",
            version("1.0-20240101.101010-3").remotePath());
    }

    private static ArtifactCoordinates version(String version)
    {
        return new ArtifactCoordinates("org.example", "widget", "jar", StringUtils.EMPTY, version);
    }

    private static List<String> versionsOf(List<ArtifactCoordinates> coordinates)
    {
        List<String> versions = new ArrayList<>();

        for (ArtifactCoordinates candidate : coordinates)
        {
            versions.add(candidate.version());
        }

        return versions;
    }
}
