package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.DownloadTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleFailed;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ReactorEdge;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;
import org.junit.Test;

public class ProfileEventsTest
{
    @Test
    public void parse_projectTiming_readsOffsetAndDuration()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.PROJECT_TIMING, "org.example:core", "120", "450"))
                .orElseThrow();

        assertEquals(new ModuleTiming("org.example:core", 120L, 450L), event);
    }

    @Test
    public void parse_projectFailed_namesTheModuleThatStoppedTheReactor()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.PROJECT_FAILED, "org.example:core", "Unable to resolve artifact"))
                .orElseThrow();

        assertEquals(new ModuleFailed("org.example:core"), event);
    }

    @Test
    public void parse_mojoTiming_readsGoalExecutionIdAndTiming()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MOJO_TIMING,
                        "org.example:core",
                        "maven-compiler-plugin:compile",
                        "default-compile",
                        "10",
                        "200"))
                .orElseThrow();

        assertEquals(
            new MojoTiming(
                "org.example:core", "maven-compiler-plugin:compile", "default-compile", 10L, 200L),
            event);
    }

    @Test
    public void parse_mojoTiming_readsThePhaseAndTheWorkerBehindIt()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MOJO_TIMING,
                        "org.example:core",
                        "maven-surefire-plugin:test",
                        "default-test",
                        "10",
                        "200",
                        "test",
                        "mvn-builder-1"))
                .orElseThrow();

        assertEquals(
            new MojoTiming(
                "org.example:core",
                "maven-surefire-plugin:test",
                "default-test",
                10L,
                200L,
                "test",
                "mvn-builder-1"),
            event);
    }

    @Test
    public void parse_projectTiming_readsTheWorkerThatBuiltIt()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.PROJECT_TIMING, "org.example:core", "120", "450", "mvn-builder-2"))
                .orElseThrow();

        assertEquals(new ModuleTiming("org.example:core", 120L, 450L, "mvn-builder-2"), event);
    }

    @Test
    public void parse_artifactDownloaded_readsTheRepositoryTimingAndSize()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.ARTIFACT_DOWNLOADED,
                        "org.example:widget:jar:1.0",
                        "central",
                        "120",
                        "450",
                        "2048"))
                .orElseThrow();

        assertEquals(
            new DownloadTiming(
                DownloadKind.ARTIFACT, "org.example:widget:jar:1.0", "central", 120L, 450L, 2048L),
            event);
    }

    @Test
    public void parse_metadataDownloaded_isKeptApartFromAnArtifactDownload()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.METADATA_DOWNLOADED,
                        "org.example:widget:1.0",
                        "central",
                        "0",
                        "10",
                        "512"))
                .orElseThrow();

        assertEquals(DownloadKind.METADATA, ((DownloadTiming) event).kind());
    }

    @Test
    public void parse_aDownloadWhoseSizeWasNeverKnown_readsAsZeroRatherThanBeingDropped()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.ARTIFACT_DOWNLOADED, "org.example:widget:jar:1.0", "central", "0", "10"))
                .orElseThrow();

        assertEquals(0L, ((DownloadTiming) event).bytes());
    }

    @Test
    public void parse_aDownloadWithANonNumericDuration_isIgnored()
    {
        assertTrue(
            ProfileEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.ARTIFACT_DOWNLOADED,
                        "org.example:widget:jar:1.0",
                        "central",
                        "0",
                        "soon",
                        "10"))
                .isEmpty());
    }

    @Test
    public void parse_reactorEdge_readsBothCoordinates()
    {
        ProfileEvent event =
            ProfileEvents
                .parse(SpyProtocol.encode(SpyProtocol.REACTOR_EDGE, "org.example:app", "org.example:core"))
                .orElseThrow();

        assertEquals(new ReactorEdge("org.example:app", "org.example:core"), event);
    }

    @Test
    public void parse_reactorEdgePointingAtItself_isIgnored()
    {
        assertTrue(
            ProfileEvents
                .parse(SpyProtocol.encode(SpyProtocol.REACTOR_EDGE, "org.example:app", "org.example:app"))
                .isEmpty());
    }

    @Test
    public void parse_projectTimingWithANonNumericDuration_isIgnored()
    {
        assertTrue(
            ProfileEvents
                .parse(SpyProtocol.encode(SpyProtocol.PROJECT_TIMING, "org.example:core", "0", "soon", "main"))
                .isEmpty());
    }

    @Test
    public void parse_buildTreeEvent_carriesNoProfilingDataOfItsOwn()
    {
        assertTrue(
            ProfileEvents
                .parse(SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:core", "Core"))
                .isEmpty());
    }

    @Test
    public void parse_emptyLine_isIgnored()
    {
        assertTrue(ProfileEvents.parse("").isEmpty());
    }
}
