package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;

public final class ReactorLanes
{
    private ReactorLanes()
    {
    }

    public static Map<String, Integer> assign(List<ModuleTiming> modules)
    {
        List<Long> laneEnds = new ArrayList<>();
        Map<String, Integer> lanes = new LinkedHashMap<>();

        for (ModuleTiming module : byStart(modules))
        {
            int lane = firstFree(laneEnds, module.startMillis());

            if (lane == laneEnds.size())
            {
                laneEnds.add(Long.valueOf(module.endMillis()));
            }
            else
            {
                laneEnds.set(lane, Long.valueOf(module.endMillis()));
            }

            lanes.put(module.module(), Integer.valueOf(lane));
        }

        return lanes;
    }

    public static int count(List<ModuleTiming> modules)
    {
        return assign(modules).values().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
    }

    private static List<ModuleTiming> byStart(List<ModuleTiming> modules)
    {
        return modules
            .stream()
            .sorted(
                Comparator
                    .comparingLong(ModuleTiming::startMillis)
                    .thenComparing(ModuleTiming::module))
            .toList();
    }

    private static int firstFree(List<Long> laneEnds, long start)
    {
        for (int lane = 0; lane < laneEnds.size(); lane++)
        {
            if (laneEnds.get(lane).longValue() <= start)
            {
                return lane;
            }
        }

        return laneEnds.size();
    }
}
