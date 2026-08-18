package org.bitstrings.idea.plugins.mavenprime.build;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleResult;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleStarted;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class SpyServerTest
    extends BasePlatformTestCase
{
    private static final int TIMEOUT_SECONDS = 30;

    private static final long TIMEOUT_MILLIS = TIMEOUT_SECONDS * 1000L;

    private static final String OTHER_TOKEN = "another-build-token";

    private static final String UNKNOWN_TOKEN = "never-registered-token";

    private final List<MavenLogEvent> received = new CopyOnWriteArrayList<>();

    private final List<MavenLogEvent> receivedByOther = new CopyOnWriteArrayList<>();

    private SpyServer server;

    private String token;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        server = SpyServer.getInstance(getProject());
        token = "build-token-" + getName();

        assertTrue(
            "the loopback listener is not bound, so every event test here would only time out",
            server.getPort() > 0);
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            server.unregister(token);
            server.unregister(OTHER_TOKEN);
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testGetPort_askedTwice_staysStableSoARerunStillReachesTheListener()
    {
        assertEquals(server.getPort(), server.getPort());
    }

    public void testGetPort_anyProject_isBound()
    {
        assertTrue(server.getPort() > 0);
    }

    public void testReceive_greetingCarryingARegisteredToken_routesToThatRegistration()
        throws Exception
    {
        CountDownLatch delivered = register(2);

        speak(
            token,
            SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:core", "Core"),
            SpyProtocol.encode(SpyProtocol.PROJECT_SUCCEEDED, "org.example:core"));

        assertTrue(delivered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertEquals(new ModuleStarted("org.example", "core"), received.get(0));
        assertEquals(new ModuleResult("org.example:core", true, false, ""), received.get(1));
    }

    public void testReceive_greetingCarryingAnotherToken_leavesThisRegistrationUntouched()
        throws Exception
    {
        register(token, received, 1);

        CountDownLatch elsewhere = register(OTHER_TOKEN, receivedByOther, 1);

        speak(OTHER_TOKEN, SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:core", "Core"));

        assertTrue(elsewhere.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("a foreign token must not feed this registration", received.isEmpty());
    }

    public void testReceive_lineThatMapsToNothing_deliversNoEvent()
        throws Exception
    {
        CountDownLatch delivered = register(1);

        speak(
            token,
            SpyProtocol.encode(SpyProtocol.SESSION_STARTED, "3"),
            "GARBAGE FROM A NEWER SPY",
            SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:core", "Core"));

        assertTrue(delivered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertEquals(1, received.size());
    }

    public void testAwaitCompletion_clientDisconnected_reportsTheReaderFinished()
        throws Exception
    {
        CountDownLatch delivered = register(1);

        speak(token, SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:core", "Core"));

        assertTrue(delivered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue(server.awaitCompletion(token, TIMEOUT_MILLIS));
    }

    public void testAwaitCompletion_tokenNeverRegistered_returnsImmediately()
    {
        assertTrue(server.awaitCompletion(UNKNOWN_TOKEN, 0L));
    }

    public void testReceive_handlerFailingOnOneEvent_stillDeliversTheFollowingEvents()
        throws Exception
    {
        CountDownLatch delivered = new CountDownLatch(2);

        server.register(
            token,
            line ->
            {
                delivered.countDown();

                throw new IllegalStateException("handler refused " + line);
            });

        speak(
            token,
            SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:core", "Core"),
            SpyProtocol.encode(SpyProtocol.PROJECT_SUCCEEDED, "org.example:core"));

        assertTrue(delivered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private CountDownLatch register(int expected)
    {
        return register(token, received, expected);
    }

    private CountDownLatch register(String key, List<MavenLogEvent> sink, int expected)
    {
        CountDownLatch delivered = new CountDownLatch(expected);

        server.register(
            key,
            line ->
                SpyEvents
                    .parse(line)
                    .ifPresent(
                        event ->
                        {
                            sink.add(event);
                            delivered.countDown();
                        }));

        return delivered;
    }

    private void speak(String greetingToken, String... lines)
        throws Exception
    {
        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getPort());
            Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))
        {
            out.write(SpyProtocol.encode(SpyProtocol.HANDSHAKE, greetingToken, "test build"));
            out.write('\n');

            for (String line : lines)
            {
                out.write(line);
                out.write('\n');
            }

            out.flush();
        }
    }
}
