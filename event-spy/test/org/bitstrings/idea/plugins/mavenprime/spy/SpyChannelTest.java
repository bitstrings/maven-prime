package org.bitstrings.idea.plugins.mavenprime.spy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SpyChannelTest
{
    private static final int BLOCKED_READER_CAPACITY = 4;

    private static final int LINES_PAST_CAPACITY = 500;

    private static final int SMALL_RECEIVE_BUFFER = 1024;

    private static final int UNREAD_LINE_LENGTH = 4096;

    private static final int UNREAD_LINES = 2048;

    private static final long DEADLOCK_TIMEOUT_MILLIS = 20_000L;

    private ServerSocket listener;

    private SpyChannel channel;

    @Before
    public void openListener()
        throws IOException
    {
        listener = new ServerSocket();

        listener.setReceiveBufferSize(SMALL_RECEIVE_BUFFER);
        listener.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 1);
    }

    @After
    public void closeEverything()
        throws IOException
    {
        if (channel != null)
        {
            channel.close();
        }

        listener.close();
    }

    @Test
    public void send_severalLines_deliversThemInTheOrderTheBuildProducedThem()
        throws IOException
    {
        channel = connected();

        Socket accepted = listener.accept();

        channel.send("first");
        channel.send("second");
        channel.send("third");
        channel.close();

        assertEquals(Arrays.asList("first", "second", "third"), linesOf(accepted));
    }

    @Test
    public void close_linesStillWaitingToBeSent_deliversThemBeforeTheSocketGoes()
        throws IOException
    {
        channel = connected();

        Socket accepted = listener.accept();

        for (int line = 0; line < LINES_PAST_CAPACITY; line++)
        {
            channel.send(String.valueOf(line));
        }

        channel.close();

        assertEquals(LINES_PAST_CAPACITY, linesOf(accepted).size());
    }

    @Test
    public void send_nobodyDrainingTheSocket_returnsInsteadOfStallingTheBuild()
        throws IOException, InterruptedException
    {
        channel = connected(BLOCKED_READER_CAPACITY);

        listener.accept();

        CountDownLatch returned = sendingOnAnotherThread();

        assertTrue(
            "send must never wait on the IDE to read",
            returned.await(DEADLOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }

    private CountDownLatch sendingOnAnotherThread()
    {
        CountDownLatch returned = new CountDownLatch(1);

        String event = unreadEvent();

        Thread caller =
            new Thread(
                () ->
                {
                    for (int line = 0; line < UNREAD_LINES; line++)
                    {
                        channel.send(event);
                    }

                    returned.countDown();
                });

        caller.setDaemon(true);
        caller.start();

        return returned;
    }

    private static String unreadEvent()
    {
        StringBuilder event = new StringBuilder(UNREAD_LINE_LENGTH);

        while (event.length() < UNREAD_LINE_LENGTH)
        {
            event.append('x');
        }

        return event.toString();
    }

    @Test
    public void send_moreLinesThanTheQueueHolds_countsEveryLineItCouldNotDeliver()
        throws IOException
    {
        channel = connected(BLOCKED_READER_CAPACITY);

        listener.accept();

        String event = unreadEvent();

        for (int line = 0; line < UNREAD_LINES; line++)
        {
            channel.send(event);
        }

        assertTrue(
            "a line the queue refused is data the IDE will never see, so it has to be counted",
            channel.getDropped() > 0L);
    }

    @Test
    public void close_linesLostToABackedUpQueue_reportsTheLossAsTheLastThingItSends()
        throws Exception
    {
        channel = connected(BLOCKED_READER_CAPACITY);

        Socket accepted = listener.accept();

        String event = unreadEvent();

        for (int line = 0; line < UNREAD_LINES; line++)
        {
            channel.send(event);
        }

        List<String> received = closeWhileReading(accepted);

        assertEquals(
            SpyProtocol.encode(SpyProtocol.DROPPED, String.valueOf(channel.getDropped())),
            received.get(received.size() - 1));
    }

    @Test
    public void close_nothingLost_reportsNoLossAtAll()
        throws Exception
    {
        channel = connected();

        Socket accepted = listener.accept();

        channel.send("only line");

        List<String> received = closeWhileReading(accepted);

        assertEquals(Arrays.asList("only line"), received);
    }

    private List<String> closeWhileReading(Socket accepted)
        throws Exception
    {
        FutureTask<List<String>> reader = new FutureTask<>(() -> linesOf(accepted));

        Thread readerThread = new Thread(reader);

        readerThread.setDaemon(true);
        readerThread.start();

        channel.close();

        return reader.get(DEADLOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void send_afterTheChannelClosed_isIgnored()
        throws IOException
    {
        channel = connected();

        Socket accepted = listener.accept();

        channel.close();

        channel.send("too late");

        assertEquals(0, linesOf(accepted).size());
    }

    @Test
    public void connect_nothingListeningOnThatPort_reportsFailureRatherThanThrowing()
        throws IOException
    {
        int unusedPort = listener.getLocalPort();

        listener.close();

        channel = new SpyChannel();

        assertFalse(channel.connect(unusedPort));
    }

    @Test
    public void isOpen_beforeConnecting_isFalseSoEventsAreNotCollected()
    {
        assertFalse(new SpyChannel().isOpen());
    }

    private SpyChannel connected()
    {
        return connected(0);
    }

    private SpyChannel connected(int capacity)
    {
        SpyChannel connecting = (capacity > 0) ? new SpyChannel(capacity) : new SpyChannel();

        assertTrue(connecting.connect(listener.getLocalPort()));

        return connecting;
    }

    private static List<String> linesOf(Socket accepted)
        throws IOException
    {
        List<String> lines = new ArrayList<String>();

        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(accepted.getInputStream(), Charset.forName("UTF-8")));

        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            lines.add(line);
        }

        return lines;
    }
}
