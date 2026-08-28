package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

public record WorkerLanes(Map<String, Integer> laneByModule, List<String> workers)
{
    private static final int NO_LANE = -1;

    public static WorkerLanes of(List<ModuleTiming> modules, List<MojoTiming> mojos)
    {
        List<String> workers = rosterOf(modules, mojos);

        return (workers.isEmpty() || !allNamed(modules))
            ? new WorkerLanes(ReactorLanes.assign(modules), List.of())
            : new WorkerLanes(lanesOf(modules, workers), workers);
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
        return modules.stream().noneMatch(module -> StringUtils.isBlank(module.worker()));
    }

    // A cancelled build reports the goals it finished and no module timings, so a roster read from
    // modules alone leaves ordinalOf nothing to number and the raw thread id reaches the reader.
    // ProfileTableGroupingPlatformTest pins the label.
    private static List<String> rosterOf(List<ModuleTiming> modules, List<MojoTiming> mojos)
    {
        Map<String, Long> firstStart = new LinkedHashMap<>();

        for (ModuleTiming module : modules)
        {
            claim(firstStart, module.worker(), module.startMillis());
        }

        for (MojoTiming mojo : mojos)
        {
            claim(firstStart, mojo.worker(), mojo.startMillis());
        }

        return firstStart
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
            .map(Map.Entry::getKey)
            .toList();
    }

    private static void claim(Map<String, Long> firstStart, String worker, long startMillis)
    {
        if (StringUtils.isNotBlank(worker))
        {
            firstStart.merge(worker, Long.valueOf(startMillis), Math::min);
        }
    }

    private static Map<String, Integer> lanesOf(List<ModuleTiming> modules, List<String> workers)
    {
        Map<String, Integer> laneByModule = new LinkedHashMap<>();

        for (ModuleTiming module : modules)
        {
            laneByModule.put(module.module(), Integer.valueOf(workers.indexOf(module.worker())));
        }

        return Map.copyOf(laneByModule);
    }
}
