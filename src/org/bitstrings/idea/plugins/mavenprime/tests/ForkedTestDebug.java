package org.bitstrings.idea.plugins.mavenprime.tests;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;

public final class ForkedTestDebug
{
    public static final String SUREFIRE_DEBUG_PROPERTY = "maven.surefire.debug";

    public static final String FAILSAFE_DEBUG_PROPERTY = "maven.failsafe.debug";

    public static final String FORK_COUNT_PROPERTY = "forkCount";

    public static final String REUSE_FORKS_PROPERTY = "reuseForks";

    private static final String SINGLE_FORK = "1";

    private static final String REUSED_FORK = "true";

    private ForkedTestDebug()
    {
    }

    public static boolean attach(MavenPrimeRequest request, String host, int port)
    {
        String options = agentOptions(host, port);

        boolean attached = false;

        if (request.properties.containsKey(MavenPrimeRequest.TEST_PROPERTY))
        {
            request.properties.put(SUREFIRE_DEBUG_PROPERTY, options);

            attached = true;
        }

        if (request.properties.containsKey(MavenPrimeRequest.INTEGRATION_TEST_PROPERTY))
        {
            request.properties.put(FAILSAFE_DEBUG_PROPERTY, options);

            attached = true;
        }

        if (!attached)
        {
            return false;
        }

        request.properties.put(FORK_COUNT_PROPERTY, SINGLE_FORK);
        request.properties.put(REUSE_FORKS_PROPERTY, REUSED_FORK);

        return true;
    }

    static String agentOptions(String host, int port)
    {
        return "-agentlib:jdwp=transport=dt_socket,suspend=y,server=n,address=" + host + ':' + port;
    }
}
