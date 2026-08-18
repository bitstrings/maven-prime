package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactDownloading;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactFailed;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;
import org.junit.Test;

public class ResolutionEventsTest
{
    @Test
    public void parse_artifactDownloading_readsRepositoryAndUrl()
    {
        ResolutionEvent event =
            ResolutionEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.ARTIFACT_DOWNLOADING,
                        "org.example:widget:jar:1.2.3",
                        "central",
                        "https://repo.maven.apache.org/maven2"))
                .orElseThrow();

        assertEquals(
            new ArtifactDownloading(
                "org.example:widget:jar:1.2.3", "central", "https://repo.maven.apache.org/maven2"),
            event);
    }

    @Test
    public void parse_artifactFailed_readsTheFailureMessage()
    {
        ResolutionEvent event =
            ResolutionEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.ARTIFACT_FAILED,
                        "org.example:missing:jar:9.9.9",
                        "central",
                        "Could not find artifact"))
                .orElseThrow();

        assertEquals(
            new ArtifactFailed("org.example:missing:jar:9.9.9", "central", "Could not find artifact"),
            event);
    }

    @Test
    public void parse_eventWithoutCoordinates_isIgnored()
    {
        assertTrue(
            ResolutionEvents
                .parse(SpyProtocol.encode(SpyProtocol.ARTIFACT_FAILED, "", "central", "boom"))
                .isEmpty());
    }

    @Test
    public void parse_buildTreeEvent_carriesNoResolutionDataOfItsOwn()
    {
        assertTrue(
            ResolutionEvents
                .parse(SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:core", "Core"))
                .isEmpty());
    }

    @Test
    public void parse_emptyLine_isIgnored()
    {
        assertTrue(ResolutionEvents.parse("").isEmpty());
    }
}
