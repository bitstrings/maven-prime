package org.bitstrings.idea.plugins.mavenprime.execution;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public final class RequestSummary
{
    private static final String SEPARATOR = " · ";

    private RequestSummary()
    {
    }

    public static String of(MavenPrimeRequest request)
    {
        List<String> parts = new ArrayList<>();

        add(parts, String.join(StringUtils.SPACE, CommandLineRenderer.renderOptions(request, request.flags)));

        if ((request.distribution != null) && !request.distribution.isContext())
        {
            add(parts, request.distribution.kind.getTitle());
        }

        add(parts, request.jreName);
        add(parts, request.vmOptions);

        return String.join(SEPARATOR, parts);
    }

    private static void add(List<String> parts, String part)
    {
        if (StringUtils.isNotBlank(part))
        {
            parts.add(part.trim());
        }
    }
}
