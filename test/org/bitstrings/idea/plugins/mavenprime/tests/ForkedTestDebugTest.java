package org.bitstrings.idea.plugins.mavenprime.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.junit.Test;

public class ForkedTestDebugTest
{
    private static final String HOST = "127.0.0.1";

    private static final int PORT = 41234;

    private static final String AGENT =
        "-agentlib:jdwp=transport=dt_socket,suspend=y,server=n,address=127.0.0.1:41234";

    @Test
    public void agentOptions_theAddressTheIdeListensOn_makesTheForkDialOutAndWait()
    {
        assertEquals(
            "the IDE listens and the fork connects to it, so server=n and suspend=y are what make a "
                + "breakpoint reachable at all",
            AGENT,
            ForkedTestDebug.agentOptions(HOST, PORT));
    }

    @Test
    public void attach_aRunSelectingUnitTests_pointsSurefireAtTheDebugger()
    {
        MavenPrimeRequest request = testRequest(MavenPrimeRequest.TEST_PROPERTY);

        assertTrue(ForkedTestDebug.attach(request, HOST, PORT));
        assertEquals(AGENT, request.properties.get(ForkedTestDebug.SUREFIRE_DEBUG_PROPERTY));
    }

    @Test
    public void attach_aRunSelectingUnitTests_leavesFailsafeAlone()
    {
        MavenPrimeRequest request = testRequest(MavenPrimeRequest.TEST_PROPERTY);

        ForkedTestDebug.attach(request, HOST, PORT);

        assertNull(
            "a second plugin forking a second JVM would dial a listener that is already taken, and that "
                + "fork dies on startup",
            request.properties.get(ForkedTestDebug.FAILSAFE_DEBUG_PROPERTY));
    }

    @Test
    public void attach_aRunSelectingIntegrationTests_pointsFailsafeAtTheDebugger()
    {
        MavenPrimeRequest request = testRequest(MavenPrimeRequest.INTEGRATION_TEST_PROPERTY);

        assertTrue(ForkedTestDebug.attach(request, HOST, PORT));
        assertEquals(AGENT, request.properties.get(ForkedTestDebug.FAILSAFE_DEBUG_PROPERTY));
    }

    @Test
    public void attach_aRunSelectingTests_pinsTheBuildToOneReusedFork()
    {
        MavenPrimeRequest request = testRequest(MavenPrimeRequest.TEST_PROPERTY);

        ForkedTestDebug.attach(request, HOST, PORT);

        assertEquals(
            "a debugger listens for one connection, so a second concurrent fork cannot attach",
            "1",
            request.properties.get(ForkedTestDebug.FORK_COUNT_PROPERTY));
        assertEquals(
            "reuseForks=false starts a fresh JVM per test class, and every one after the first finds "
                + "nothing listening",
            "true",
            request.properties.get(ForkedTestDebug.REUSE_FORKS_PROPERTY));
    }

    @Test
    public void attach_aRunSelectingNoTests_changesNothing()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/repo", List.of("package"));

        assertFalse(
            "with no test selection nothing forks, so the debugger would listen for a connection that "
                + "never comes",
            ForkedTestDebug.attach(request, HOST, PORT));
        assertTrue(request.properties.isEmpty());
    }

    private static MavenPrimeRequest testRequest(String selector)
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/repo", List.of("test"));

        request.properties.put(selector, "CartTest");

        return request;
    }
}
