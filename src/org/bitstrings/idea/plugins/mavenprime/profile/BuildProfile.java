package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary.Concurrency;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ReactorEdge;
import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;

public final class BuildProfile
{
    private final Object lock = new Object();

    private final List<ModuleTiming> modules = new ArrayList<>();

    private final List<MojoTiming> mojos = new ArrayList<>();

    private final Map<String, Set<String>> upstreams = new TreeMap<>();

    private final BuildCompleteness completeness = new BuildCompleteness();

    public BuildCompleteness getCompleteness()
    {
        return completeness;
    }

    public void accept(ProfileEvent event)
    {
        synchronized (lock)
        {
            switch (event)
            {
                case ModuleTiming module -> modules.add(module);
                case MojoTiming mojo -> mojos.add(mojo);
                case ReactorEdge edge ->
                    upstreams.computeIfAbsent(edge.module(), key -> new TreeSet<>()).add(edge.upstream());
            }
        }
    }

    public boolean isEmpty()
    {
        synchronized (lock)
        {
            return modules.isEmpty() && mojos.isEmpty();
        }
    }

    public boolean hasModuleTimings()
    {
        synchronized (lock)
        {
            return !modules.isEmpty();
        }
    }

    public List<ModuleTiming> getModules()
    {
        synchronized (lock)
        {
            return List.copyOf(modules);
        }
    }

    public List<MojoTiming> getMojos(String module)
    {
        synchronized (lock)
        {
            return mojos.stream().filter(mojo -> mojo.module().equals(module)).toList();
        }
    }

    public Map<String, Integer> getLaneAssignment()
    {
        synchronized (lock)
        {
            return ReactorLanes.assign(modules);
        }
    }

    public BuildProfileSummary summarize()
    {
        synchronized (lock)
        {
            if (modules.isEmpty())
            {
                return BuildProfileSummary.empty();
            }

            Map<String, Long> durations = new TreeMap<>();

            long totalWork = 0L;
            long earliest = Long.MAX_VALUE;
            long latest = 0L;

            for (ModuleTiming module : modules)
            {
                durations.merge(module.module(), Long.valueOf(module.durationMillis()), Long::sum);

                totalWork += module.durationMillis();
                earliest = Math.min(earliest, module.startMillis());
                latest = Math.max(latest, module.endMillis());
            }

            List<String> criticalPath = ReactorCriticalPath.longest(durations, upstreams);

            long wallClock = latest - earliest;
            long criticalPathMillis = ReactorCriticalPath.weight(criticalPath, durations);

            int lanes = ReactorLanes.count(modules);

            return new BuildProfileSummary(
                wallClock,
                totalWork,
                criticalPathMillis,
                criticalPath,
                lanes,
                concurrency(lanes, totalWork, criticalPathMillis));
        }
    }

    private static Concurrency concurrency(int lanes, long totalWork, long criticalPath)
    {
        if (lanes <= 1)
        {
            return Concurrency.SEQUENTIAL;
        }

        return (criticalPath >= BuildProfileSummary.divideCeil(totalWork, lanes))
            ? Concurrency.DEPENDENCY_BOUND
            : Concurrency.THREAD_STARVED;
    }

    public List<ModuleTiming> getModulesByDuration()
    {
        synchronized (lock)
        {
            return modules
                .stream()
                .sorted(Comparator.comparingLong(ModuleTiming::durationMillis).reversed())
                .toList();
        }
    }
}
