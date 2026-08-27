package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;

public record WorkerLanes(Map<String, Integer> laneByModule, List<String> workers)
{
    private static final int NO_LANE = -1;

    public static WorkerLanes of(List<ModuleTiming> modules)
    {
        return allNamed(modules)
            ? byWorker(modules)
            : new WorkerLanes(ReactorLanes.assign(modules), List.of());
    }

    public boolean isByWorker()
    {
        return !workers.isEmpty();
    }

    public int count()
    {
        return Math.max(
            1, laneByModule.values().stream().mapToInt(Integer::intValue).max().orElse(NO_LANE) + 1);
    }

    public int laneOf(String module)
    {
        return laneByModule.getOrDefault(module, Integer.valueOf(NO_LANE)).intValue();
    }

    public int ordinalOf(String worker)
    {
        return workers.indexOf(worker);
    }

    private static boolean allNamed(List<ModuleTiming> modules)
    {
        return !modules.isEmpty() && modules.stream().noneMatch(module -> StringUtils.isBlank(module.worker()));
    }

    private static WorkerLanes byWorker(List<ModuleTiming> modules)
    {
        Map<String, Long> firstStart = new LinkedHashMap<>();

        for (ModuleTiming module : modules)
        {
            firstStart.merge(module.worker(), Long.valueOf(module.startMillis()), Math::min);
        }

        List<String> workers =
            firstStart
                .entrySet()
                .stream()
                .sorted(
                    Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();

        Map<String, Integer> laneByWorker = new LinkedHashMap<>();

        for (int lane = 0; lane < workers.size(); lane++)
        {
            laneByWorker.put(workers.get(lane), Integer.valueOf(lane));
        }

        Map<String, Integer> laneByModule = new LinkedHashMap<>();

        for (ModuleTiming module : modules)
        {
            laneByModule.put(module.module(), laneByWorker.get(module.worker()));
        }

        return new WorkerLanes(Map.copyOf(laneByModule), workers);
    }
}
