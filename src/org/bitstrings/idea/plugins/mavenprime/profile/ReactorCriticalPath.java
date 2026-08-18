package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class ReactorCriticalPath
{
    private ReactorCriticalPath()
    {
    }

    public static List<String> longest(Map<String, Long> durations, Map<String, Set<String>> upstreams)
    {
        Set<String> nodes = nodes(durations, upstreams);

        Map<String, Integer> remaining = new TreeMap<>();
        Map<String, List<String>> downstream = new TreeMap<>();

        for (String node : nodes)
        {
            remaining.put(node, Integer.valueOf(0));
        }

        for (String node : nodes)
        {
            for (String upstream : upstreamsOf(upstreams, node))
            {
                if (nodes.contains(upstream))
                {
                    downstream.computeIfAbsent(upstream, key -> new ArrayList<>()).add(node);
                    remaining.merge(node, Integer.valueOf(1), Integer::sum);
                }
            }
        }

        Deque<String> ready = new ArrayDeque<>();

        for (Map.Entry<String, Integer> entry : remaining.entrySet())
        {
            if (entry.getValue().intValue() == 0)
            {
                ready.add(entry.getKey());
            }
        }

        Map<String, Long> cost = new TreeMap<>();
        Map<String, String> predecessor = new TreeMap<>();

        String heaviest = null;

        while (!ready.isEmpty())
        {
            String node = ready.poll();

            String from = heaviestUpstream(upstreamsOf(upstreams, node), cost);

            long total = ((from == null) ? 0L : cost.get(from).longValue()) + duration(durations, node);

            cost.put(node, Long.valueOf(total));

            if (from != null)
            {
                predecessor.put(node, from);
            }

            if ((heaviest == null) || (total > cost.get(heaviest).longValue()))
            {
                heaviest = node;
            }

            for (String next : downstream.getOrDefault(node, List.of()))
            {
                if (remaining.merge(next, Integer.valueOf(-1), Integer::sum).intValue() == 0)
                {
                    ready.add(next);
                }
            }
        }

        return path(heaviest, predecessor);
    }

    public static long weight(List<String> path, Map<String, Long> durations)
    {
        long total = 0L;

        for (String node : path)
        {
            total += duration(durations, node);
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

    private static Set<String> nodes(Map<String, Long> durations, Map<String, Set<String>> upstreams)
    {
        Set<String> nodes = new TreeSet<>(durations.keySet());

        nodes.addAll(upstreams.keySet());

        for (Set<String> declared : upstreams.values())
        {
            nodes.addAll(declared);
        }

        return nodes;
    }

    private static Set<String> upstreamsOf(Map<String, Set<String>> upstreams, String node)
    {
        return upstreams.getOrDefault(node, Set.of());
    }

    private static long duration(Map<String, Long> durations, String node)
    {
        Long duration = durations.get(node);

        return (duration == null) ? 0L : duration.longValue();
    }
}
