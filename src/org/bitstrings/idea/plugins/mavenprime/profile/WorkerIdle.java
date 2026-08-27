package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;

public final class WorkerIdle
{
    private WorkerIdle()
    {
    }

    public static List<IdleGap> gaps(
        List<ModuleTiming> modules, WorkerLanes lanes, Map<String, Set<String>> upstreams)
    {
        if (!lanes.isByWorker())
        {
            return List.of();
        }

        Map<Integer, List<ModuleTiming>> byLane = new TreeMap<>();

        for (ModuleTiming module : modules)
        {
            byLane
                .computeIfAbsent(Integer.valueOf(lanes.laneOf(module.module())), lane -> new ArrayList<>())
                .add(module);
        }

        Map<String, Long> ends = endsOf(modules);

        List<IdleGap> gaps = new ArrayList<>();

        for (Map.Entry<Integer, List<ModuleTiming>> lane : byLane.entrySet())
        {
            addGaps(gaps, lane.getKey().intValue(), lane.getValue(), upstreams, ends);
        }

        return List.copyOf(gaps);
    }

    public static long totalMillis(List<IdleGap> gaps)
    {
        return gaps.stream().mapToLong(IdleGap::durationMillis).sum();
    }

    private static void addGaps(
        List<IdleGap> gaps,
        int lane,
        List<ModuleTiming> modules,
        Map<String, Set<String>> upstreams,
        Map<String, Long> ends)
    {
        modules.sort(Comparator.comparingLong(ModuleTiming::startMillis).thenComparing(ModuleTiming::module));

        long busyUntil = modules.get(0).endMillis();

        for (int index = 1; index < modules.size(); index++)
        {
            ModuleTiming module = modules.get(index);

            if (module.startMillis() > busyUntil)
            {
                gaps.add(
                    new IdleGap(
                        lane,
                        busyUntil,
                        module.startMillis(),
                        module.module(),
                        lastUpstreamOf(module.module(), upstreams, ends)));
            }

            busyUntil = Math.max(busyUntil, module.endMillis());
        }
    }

    private static String lastUpstreamOf(
        String module, Map<String, Set<String>> upstreams, Map<String, Long> ends)
    {
        String latest = StringUtils.EMPTY;
        long latestEnd = Long.MIN_VALUE;

        for (String upstream : upstreams.getOrDefault(module, Set.of()))
        {
            Long end = ends.get(upstream);

            if ((end != null) && (end.longValue() > latestEnd))
            {
                latest = upstream;
                latestEnd = end.longValue();
            }
        }

        return latest;
    }

    private static Map<String, Long> endsOf(List<ModuleTiming> modules)
    {
        Map<String, Long> ends = new HashMap<>();

        for (ModuleTiming module : modules)
        {
            ends.merge(module.module(), Long.valueOf(module.endMillis()), Math::max);
        }

        return ends;
    }

    public record IdleGap(
        int lane,
        long startMillis,
        long endMillis,
        String nextModule,
        String lastUpstream)
    {
        public long durationMillis()
        {
            return endMillis - startMillis;
        }
    }
}
