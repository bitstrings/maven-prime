package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactFailed;
import org.junit.Test;

public class ResolutionRecordedSessionTest
{
    private static final String OFFLINE_FAILURE =
        "ARTIFACT_FAILED\torg.apache.maven.plugins:maven-resources-plugin:pom:3.3.1\t\t"
            + "Cannot access main (https://repo.expretio.com/artifactory/m2-main) in offline mode and the"
            + " artifact org.apache.maven.plugins:maven-resources-plugin:pom:3.3.1 has not been downloaded"
            + " from it before.";

    @Test
    public void parse_recordedOfflineFailure_keepsMavensOwnExplanation()
    {
        ArtifactFailed failed = (ArtifactFailed) ResolutionEvents.parse(OFFLINE_FAILURE).orElseThrow();

        assertEquals("org.apache.maven.plugins:maven-resources-plugin:pom:3.3.1", failed.coordinates());
    }

    @Test
    public void parse_recordedFailureWithNoRepositoryOnTheEvent_leavesTheRepositoryBlank()
    {
        ArtifactFailed failed = (ArtifactFailed) ResolutionEvents.parse(OFFLINE_FAILURE).orElseThrow();

        assertEquals("", failed.repository());
    }

    @Test
    public void parse_recordedFailureCoordinates_locateTheArtifactInTheLocalRepository()
    {
        ArtifactCoordinates coordinates =
            ArtifactCoordinates
                .parse(ResolutionEvents.parse(OFFLINE_FAILURE).orElseThrow().coordinates())
                .orElseThrow();

        Path directory = coordinates.directoryIn(Paths.get("/repo"));

        assertEquals(
            Paths.get("/repo/org/apache/maven/plugins/maven-resources-plugin/3.3.1"), directory);
    }

    @Test
    public void parse_recordedFailureCoordinates_nameThePomFileMavenLooksFor()
    {
        ArtifactCoordinates coordinates =
            ArtifactCoordinates
                .parse(ResolutionEvents.parse(OFFLINE_FAILURE).orElseThrow().coordinates())
                .orElseThrow();

        assertEquals("maven-resources-plugin-3.3.1.pom", coordinates.fileName());
    }
}
