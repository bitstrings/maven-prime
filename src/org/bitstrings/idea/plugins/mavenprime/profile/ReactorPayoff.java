package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ReactorPayoff
{
    private ReactorPayoff()
    {
    }

    public static Map<String, Long> of(Map<String, Long> durations, Map<String, Set<String>> upstreams)
    {
        ReactorGraph graph = ReactorGraph.of(durations, upstreams);

        Map<String, Long> earliest = finishTimes(graph, null);
        Map<String, Long> remainder = longestFrom(graph);

        long makespan = maxOf(earliest);

        Map<String, Long> payoff = new TreeMap<>();

        for (String node : graph.getOrdered())
        {
            long through = valueOf(earliest, node) + valueOf(remainder, node) - graph.durationOf(node);

            payoff.put(
                node,
                Long.valueOf(
                    (through < makespan) ? 0L : (makespan - maxOf(finishTimes(graph, node)))));
        }

        return Map.copyOf(payoff);
    }

    private static Map<String, Long> finishTimes(ReactorGraph graph, String freed)
    {
        Map<String, Long> finish = new HashMap<>();

        for (String node : graph.getOrdered())
        {
            long ready = 0L;

            for (String upstream : graph.upstreamsOf(node))
            {
                ready = Math.max(ready, valueOf(finish, upstream));
            }

            finish.put(node, Long.valueOf(ready + (node.equals(freed) ? 0L : graph.durationOf(node))));
        }

        return finish;
    }

    private static Map<String, Long> longestFrom(ReactorGraph graph)
    {
        List<String> ordered = graph.getOrdered();

        Map<String, Long> weight = new HashMap<>();

        for (int index = ordered.size() - 1; index >= 0; index--)
        {
            String node = ordered.get(index);

            long after = 0L;

            for (String next : graph.downstreamOf(node))
            {
                after = Math.max(after, valueOf(weight, next));
            }

            weight.put(node, Long.valueOf(after + graph.durationOf(node)));
        }

        return weight;
    }

    private static long maxOf(Map<String, Long> values)
    {
        return values.values().stream().mapToLong(Long::longValue).max().orElse(0L);
    }

    private static long valueOf(Map<String, Long> values, String node)
    {
        Long value = values.get(node);

        return (value == null) ? 0L : value.longValue();
    }
}
