package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ReactorCriticalPath
{
    private ReactorCriticalPath()
    {
    }

    public static List<String> longest(Map<String, Long> durations, Map<String, Set<String>> upstreams)
    {
        ReactorGraph graph = ReactorGraph.of(durations, upstreams);

        Map<String, Long> cost = new TreeMap<>();
        Map<String, String> predecessor = new TreeMap<>();

        String heaviest = null;

        for (String node : graph.getOrdered())
        {
            String from = heaviestUpstream(graph.upstreamsOf(node), cost);

            long total = ((from == null) ? 0L : cost.get(from).longValue()) + graph.durationOf(node);

            cost.put(node, Long.valueOf(total));

            if (from != null)
            {
                predecessor.put(node, from);
            }

            if ((heaviest == null) || (total > cost.get(heaviest).longValue()))
            {
                heaviest = node;
            }
        }

        return path(heaviest, predecessor);
    }

    public static long weight(List<String> path, Map<String, Long> durations)
    {
        long total = 0L;

        for (String node : path)
        {
            Long duration = durations.get(node);

            total += (duration == null) ? 0L : duration.longValue();
        }

        return total;
    }

    private static String heaviestUpstream(Set<String> candidates, Map<String, Long> cost)
    {
        String heaviest = null;

        for (String candidate : candidates)
        {
            Long candidateCost = cost.get(candidate);

            if ((candidateCost != null)
                && ((heaviest == null) || (candidateCost.longValue() > cost.get(heaviest).longValue())))
            {
                heaviest = candidate;
            }
        }

        return heaviest;
    }

    private static List<String> path(String last, Map<String, String> predecessor)
    {
        if (last == null)
        {
            return List.of();
        }

        List<String> path = new ArrayList<>();

        for (String node = last; node != null; node = predecessor.get(node))
        {
            path.add(node);
        }

        Collections.reverse(path);

        return List.copyOf(path);
    }
}
