package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class ReactorGraph
{
    private final Map<String, Long> durations;

    private final Map<String, Set<String>> upstreams;

    private final Map<String, List<String>> downstream;

    private final List<String> ordered;

    private ReactorGraph(
        Map<String, Long> durations,
        Map<String, Set<String>> upstreams,
        Map<String, List<String>> downstream,
        List<String> ordered)
    {
        this.durations = durations;
        this.upstreams = upstreams;
        this.downstream = downstream;
        this.ordered = ordered;
    }

    public static ReactorGraph of(Map<String, Long> durations, Map<String, Set<String>> upstreams)
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
            for (String upstream : upstreams.getOrDefault(node, Set.of()))
            {
                if (nodes.contains(upstream))
                {
                    downstream.computeIfAbsent(upstream, key -> new ArrayList<>()).add(node);
                    remaining.merge(node, Integer.valueOf(1), Integer::sum);
                }
            }
        }

        return new ReactorGraph(durations, upstreams, downstream, topological(remaining, downstream));
    }

    private static List<String> topological(
        Map<String, Integer> remaining, Map<String, List<String>> downstream)
    {
        Deque<String> ready = new ArrayDeque<>();

        for (Map.Entry<String, Integer> entry : remaining.entrySet())
        {
            if (entry.getValue().intValue() == 0)
            {
                ready.add(entry.getKey());
            }
        }

        List<String> ordered = new ArrayList<>(remaining.size());

        while (!ready.isEmpty())
        {
            String node = ready.poll();

            ordered.add(node);

            for (String next : downstream.getOrDefault(node, List.of()))
            {
                if (remaining.merge(next, Integer.valueOf(-1), Integer::sum).intValue() == 0)
                {
                    ready.add(next);
                }
            }
        }

        return List.copyOf(ordered);
    }

    public List<String> getOrdered()
    {
        return ordered;
    }

    public Set<String> upstreamsOf(String node)
    {
        return upstreams.getOrDefault(node, Set.of());
    }

    public List<String> downstreamOf(String node)
    {
        return downstream.getOrDefault(node, List.of());
    }

    public long durationOf(String node)
    {
        Long duration = durations.get(node);

        return (duration == null) ? 0L : duration.longValue();
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
}
