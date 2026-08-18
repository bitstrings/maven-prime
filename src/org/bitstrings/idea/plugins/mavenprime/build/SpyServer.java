package org.bitstrings.idea.plugins.mavenprime.build;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class SpyServer
    implements Disposable
{
    private static final Logger LOG = Logger.getInstance(SpyServer.class);

    private static final int BACKLOG = 16;

    private final Project project;

    private final Object lock = new Object();

    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;

    private volatile boolean closed;

    public SpyServer(Project project)
    {
        this.project = project;
    }

    public static SpyServer getInstance(Project project)
    {
        return project.getService(SpyServer.class);
    }

    public int getPort()
    {
        synchronized (lock)
        {
            if (closed)
            {
                return 0;
            }

            if (serverSocket == null)
            {
                serverSocket = open();

                if (serverSocket != null)
                {
                    ApplicationManager.getApplication().executeOnPooledThread(this::acceptLoop);
                }
            }

            return (serverSocket == null) ? 0 : serverSocket.getLocalPort();
        }
    }

    public void register(String token, Consumer<String> handler)
    {
        registrations.put(token, new Registration(handler));
    }

    public void unregister(String token)
    {
        registrations.remove(token);
    }

    public boolean awaitCompletion(String token, long millis)
    {
        Registration registration = registrations.get(token);

        if (registration == null)
        {
            return true;
        }

        try
        {
            return registration.completed.await(millis, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException interrupted)
        {
            Thread.currentThread().interrupt();

            return false;
        }
    }

    private ServerSocket open()
    {
        try
        {
            return new ServerSocket(0, BACKLOG, InetAddress.getLoopbackAddress());
        }
        catch (IOException unavailable)
        {
            LOG.warn("Maven Prime build events are unavailable: the local listener could not be opened", unavailable);

            return null;
        }
    }

    private void acceptLoop()
    {
        while (!closed)
        {
            try
            {
                Socket accepted = serverSocket.accept();

                ApplicationManager.getApplication().executeOnPooledThread(() -> receive(accepted));
            }
            catch (IOException stopped)
            {
                if (!closed)
                {
                    LOG.debug("Maven Prime build event listener stopped", stopped);
                }

                return;
            }
        }
    }

    private void receive(Socket accepted)
    {
        String token = StringUtils.EMPTY;

        SpySession adopted = null;

        try (Socket channel = accepted;
            BufferedReader lines =
                new BufferedReader(new InputStreamReader(channel.getInputStream(), StandardCharsets.UTF_8)))
        {
            String greeting = lines.readLine();

            if ((greeting == null) || !greeting.startsWith(SpyProtocol.HANDSHAKE))
            {
                return;
            }

            token = tokenOf(greeting);

            Registration claimed = registrations.get(token);

            if (claimed == null)
            {
                if (project.isDisposed())
                {
                    return;
                }

                adopted = SpySession.adopt(project, buildNameOf(greeting));
                token = adopted.getToken();
                claimed = registrations.get(token);
            }

            if (claimed == null)
            {
                return;
            }

            Consumer<String> handler = claimed.handler;

            for (String line = lines.readLine(); line != null; line = lines.readLine())
            {
                deliver(handler, line);
            }
        }
        catch (IOException disconnected)
        {
            if (!closed)
            {
                LOG.debug("Maven Prime build event channel closed", disconnected);
            }
        }
        finally
        {
            complete(token);

            if (adopted != null)
            {
                adopted.finish();
            }
        }
    }

    private void complete(String token)
    {
        Registration registration = registrations.get(token);

        if (registration != null)
        {
            registration.completed.countDown();
        }
    }

    private static String tokenOf(String greeting)
    {
        return fieldOf(greeting, 1);
    }

    private static String buildNameOf(String greeting)
    {
        return fieldOf(greeting, 2);
    }

    private static String fieldOf(String greeting, int index)
    {
        if ((greeting == null) || !greeting.startsWith(SpyProtocol.HANDSHAKE))
        {
            return StringUtils.EMPTY;
        }

        String[] fields = greeting.split(SpyProtocol.SEPARATOR, -1);

        return (index < fields.length) ? fields[index] : StringUtils.EMPTY;
    }

    private static void deliver(Consumer<String> handler, String line)
    {
        try
        {
            handler.accept(line);
        }
        catch (ProcessCanceledException canceled)
        {
            throw canceled;
        }
        catch (RuntimeException failure)
        {
            LOG.warn("Maven Prime could not handle a build event", failure);
        }
    }

    @Override
    public void dispose()
    {
        synchronized (lock)
        {
            closed = true;

            closeQuietly(serverSocket);
        }

        registrations.values().forEach(registration -> registration.completed.countDown());
        registrations.clear();
    }

    private static void closeQuietly(Closeable target)
    {
        if (target == null)
        {
            return;
        }

        try
        {
            target.close();
        }
        catch (IOException ignored)
        {
        }
    }

    private static final class Registration
    {
        private final Consumer<String> handler;

        private final CountDownLatch completed = new CountDownLatch(1);

        Registration(Consumer<String> handler)
        {
            this.handler = handler;
        }
    }
}
