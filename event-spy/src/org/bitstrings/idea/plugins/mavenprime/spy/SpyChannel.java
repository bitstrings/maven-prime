package org.bitstrings.idea.plugins.mavenprime.spy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class SpyChannel
    implements LineSink
{
    private static final ChannelAction FLUSH = Writer::flush;

    private static final int DEFAULT_CAPACITY = 65536;

    private static final long POLL_INTERVAL_MILLIS = 50L;

    private static final long DRAIN_TIMEOUT_MILLIS = 2000L;

    private static final String SENDER_NAME = "maven-prime-event-spy";

    private final BlockingQueue<String> pending;

    private final AtomicBoolean open = new AtomicBoolean();

    private final AtomicLong dropped = new AtomicLong();

    private volatile Socket socket;

    private volatile Writer out;

    private volatile Thread sender;

    public SpyChannel()
    {
        this(DEFAULT_CAPACITY);
    }

    SpyChannel(int capacity)
    {
        this.pending = new ArrayBlockingQueue<String>(capacity);
    }

    public boolean connect(int port)
    {
        try
        {
            socket = new Socket(InetAddress.getLoopbackAddress(), port);
            out =
                new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));
        }
        catch (Exception unreachable)
        {
            closeQuietly();

            return false;
        }

        open.set(true);

        sender = new Thread(this::drain, SENDER_NAME);

        sender.setDaemon(true);
        sender.start();

        return true;
    }

    public boolean isOpen()
    {
        return open.get();
    }

    @Override
    public void send(String line)
    {
        if (open.get() && !pending.offer(line))
        {
            dropped.incrementAndGet();
        }
    }

    long getDropped()
    {
        return dropped.get();
    }

    public void close()
    {
        if (open.compareAndSet(true, false))
        {
            awaitSender();
        }

        closeQuietly();
    }

    private void awaitSender()
    {
        Thread worker = sender;

        if (worker == null)
        {
            return;
        }

        try
        {
            worker.join(DRAIN_TIMEOUT_MILLIS);
        }
        catch (InterruptedException interrupted)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void drain()
    {
        try
        {
            while (true)
            {
                String line = pending.poll(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

                if (line != null)
                {
                    writeLine(line);

                    if (pending.isEmpty())
                    {
                        onChannel(FLUSH);
                    }
                }
                else if (!open.get())
                {
                    reportDropped();

                    return;
                }
            }
        }
        catch (InterruptedException stopped)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void reportDropped()
    {
        long lost = dropped.get();

        if (lost > 0L)
        {
            writeLine(SpyProtocol.encode(SpyProtocol.DROPPED, String.valueOf(lost)));
        }

        onChannel(FLUSH);
    }

    private void writeLine(String line)
    {
        onChannel(
            channel ->
            {
                channel.write(line);
                channel.write('\n');
            });
    }

    private void onChannel(ChannelAction action)
    {
        Writer channel = out;

        if (channel == null)
        {
            return;
        }

        try
        {
            action.run(channel);
        }
        catch (Exception broken)
        {
            open.set(false);

            closeQuietly();
        }
    }

    private void closeQuietly()
    {
        Writer writer = out;
        Socket channel = socket;

        out = null;
        socket = null;

        try
        {
            if (writer != null)
            {
                writer.close();
            }
        }
        catch (Exception ignored)
        {
        }

        try
        {
            if (channel != null)
            {
                channel.close();
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private interface ChannelAction
    {
        void run(Writer channel)
            throws IOException;
    }
}
