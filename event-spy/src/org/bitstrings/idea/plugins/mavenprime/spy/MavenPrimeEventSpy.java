package org.bitstrings.idea.plugins.mavenprime.spy;

import java.util.Map;
import java.util.Properties;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.eclipse.aether.RepositoryEvent;

@Named
@Singleton
public class MavenPrimeEventSpy
    extends AbstractEventSpy
{
    private static final String USER_PROPERTIES = "userProperties";

    private final SpyChannel channel = new SpyChannel();

    private final SpyReporter reporter = new SpyReporter(channel);

    @Override
    public void init(Context context)
    {
        int port = port(context);

        if (port <= 0)
        {
            return;
        }

        if (!channel.connect(port))
        {
            return;
        }

        channel.send(
            SpyProtocol.encode(
                SpyProtocol.HANDSHAKE,
                property(context, SpyProtocol.TOKEN_PROPERTY),
                property(context, SpyProtocol.BUILD_PROPERTY)));
    }

    @Override
    public void onEvent(Object event)
    {
        if (!channel.isOpen())
        {
            return;
        }

        if (event instanceof ExecutionEvent)
        {
            reporter.report((ExecutionEvent) event);
        }
        else if (event instanceof RepositoryEvent)
        {
            reporter.report((RepositoryEvent) event);
        }
    }

    @Override
    public void close()
    {
        channel.close();
    }

    private static String property(Context context, String name)
    {
        Map<String, Object> data = context.getData();

        Object properties = (data == null) ? null : data.get(USER_PROPERTIES);

        String value =
            (properties instanceof Properties) ? ((Properties) properties).getProperty(name) : null;

        if ((value == null) || (value.trim().length() == 0))
        {
            value = System.getProperty(name);
        }

        return (value == null) ? "" : value.trim();
    }

    private static int port(Context context)
    {
        String port = property(context, SpyProtocol.PORT_PROPERTY);

        if (port.length() == 0)
        {
            return 0;
        }

        try
        {
            return Integer.parseInt(port.trim());
        }
        catch (NumberFormatException notAPort)
        {
            return 0;
        }
    }
}
