package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary.Concurrency;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.DownloadTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleFailed;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ReactorEdge;
import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;

public final class BuildProfile
{
    private final Object lock = new Object();

    private final List<ModuleTiming> modules = new ArrayList<>();

    private final List<MojoTiming> mojos = new ArrayList<>();

    private final List<DownloadTiming> downloads = new ArrayList<>();

    private final Map<String, Set<String>> upstreams = new TreeMap<>();

    private final Set<String> failed = new TreeSet<>();

    private final BuildCompleteness completeness = new BuildCompleteness();

    public BuildCompleteness getCompleteness()
    {
        return completeness;
    }

    public boolean hasFailures()
    {
        synchronized (lock)
        {
            return !failed.isEmpty();
        }
    }

    public void accept(ProfileEvent event)
    {
        synchronized (lock)
        {
            switch (event)
            {
                case ModuleTiming module -> modules.add(module);
                case MojoTiming mojo -> mojos.add(mojo);
                case DownloadTiming download -> downloads.add(download);
                case ReactorEdge edge ->
                    upstreams.computeIfAbsent(edge.module(), key -> new TreeSet<>()).add(edge.upstream());
                case ModuleFailed failure -> failed.add(failure.module());
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

    public List<MojoTiming> getMojos()
    {
        synchronized (lock)
        {
            return List.copyOf(mojos);
        }
    }

    public List<DownloadTiming> getDownloads()
    {
        synchronized (lock)
        {
            return List.copyOf(downloads);
        }
    }

    public DownloadSummary summarizeDownloads()
    {
        return DownloadSummary.of(getDownloads());
    }

    public Map<String, Long> getPayoff()
    {
        synchronized (lock)
        {
            return modules.isEmpty() ? Map.of() : ReactorPayoff.of(durationsOf(), upstreams);
        }
    }

    public WorkerLanes getWorkerLanes()
    {
        synchronized (lock)
        {
            return WorkerLanes.of(modules);
        }
    }

    public List<WorkerIdle.IdleGap> getIdleGaps()
    {
        synchronized (lock)
        {
            return WorkerIdle.gaps(List.copyOf(modules), WorkerLanes.of(modules), upstreams);
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

            Map<String, Long> durations = durationsOf();

            long totalWork = 0L;
            long earliest = Long.MAX_VALUE;
            long latest = 0L;

            for (ModuleTiming module : modules)
            {
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

    private Map<String, Long> durationsOf()
    {
        Map<String, Long> durations = new TreeMap<>();

        for (ModuleTiming module : modules)
        {
            durations.merge(module.module(), Long.valueOf(module.durationMillis()), Long::sum);
        }

        return durations;
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

}
