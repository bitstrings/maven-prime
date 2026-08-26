package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
