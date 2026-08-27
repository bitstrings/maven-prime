package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;

public final class ProfileFacts
{
    private static final double PERCENT = 100.0D;

    private ProfileFacts()
    {
    }

    public static List<Fact> of(BuildProfile profile, BuildProfileSummary summary)
    {
        List<Fact> facts = new ArrayList<>();

        facts.add(new Fact(Measure.WALL_CLOCK, summary.wallClockMillis()));

        // A row repeating the wall clock under a second name teaches the reader nothing and reads as a
        // measurement that failed, which is what a single-module build produced for every row.
        if (summary.totalWorkMillis() != summary.wallClockMillis())
        {
            facts.add(new Fact(Measure.WORK, summary.totalWorkMillis()));
        }

        if (summary.criticalPathMillis() != summary.wallClockMillis())
        {
            facts.add(new Fact(Measure.CRITICAL_PATH, summary.criticalPathMillis()));
        }

        long idle = WorkerIdle.totalMillis(profile.getIdleGaps());

        if (idle > 0L)
        {
            facts.add(new Fact(Measure.IDLE, idle));
        }

        // Below the scheduling overhead the gap is the graph, not the thread count, and the two rows
        // would carry one number under labels that contradict each other.
        if (summary.recoverableMillis() > summary.schedulingLossMillis())
        {
            facts.add(new Fact(Measure.RECOVERABLE, summary.recoverableMillis()));
        }

        if (summary.schedulingLossMillis() > 0L)
        {
            facts.add(new Fact(Measure.SCHEDULING_LOSS, summary.schedulingLossMillis()));
        }

        return facts;
    }

    public static List<Step> criticalPath(BuildProfile profile, BuildProfileSummary summary)
    {
        Map<String, Long> durations = durationsOf(profile);

        List<Step> steps = new ArrayList<>(summary.criticalPath().size());

        for (String module : summary.criticalPath())
        {
            long duration = durations.getOrDefault(module, Long.valueOf(0L)).longValue();

            steps.add(new Step(module, duration, shareOf(duration, summary.wallClockMillis())));
        }

        return steps;
    }

    public static List<Gain> gains(BuildProfile profile, BuildProfileSummary summary, int limit)
    {
        return profile
            .getPayoff()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().longValue() > 0L)
            .sorted(
                Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .limit(limit)
            .map(entry -> gainOf(entry.getKey(), entry.getValue().longValue(), summary))
            .toList();
    }

    private static Gain gainOf(String module, long millis, BuildProfileSummary summary)
    {
        return new Gain(module, millis, shareOf(millis, summary.wallClockMillis()));
    }

    private static Map<String, Long> durationsOf(BuildProfile profile)
    {
        Map<String, Long> durations = new LinkedHashMap<>();

        for (ModuleTiming module : profile.getModules())
        {
            durations.put(module.module(), Long.valueOf(module.durationMillis()));
        }

        return durations;
    }

    private static double shareOf(long duration, long wallClockMillis)
    {
        return (wallClockMillis == 0L) ? 0.0D : ((duration * PERCENT) / wallClockMillis);
    }

    public enum Measure
    {
        WALL_CLOCK("wallClock"),
        WORK("work"),
        CRITICAL_PATH("criticalPath"),
        IDLE("idle"),
        RECOVERABLE("recoverable"),
        SCHEDULING_LOSS("schedulingLoss");

        private final String bundleKey;

        Measure(String bundleKey)
        {
            this.bundleKey = bundleKey;
        }

        public String getBundleKey()
        {
            return "mavenprime.profile.measure." + bundleKey;
        }

        public String getHelpKey()
        {
            return "mavenprime.profile.help." + bundleKey;
        }
    }

    public record Fact(Measure measure, long millis)
    {
    }

    public record Step(String module, long durationMillis, double sharePercent)
    {
    }

    public record Gain(String module, long millis, double sharePercent)
    {
    }
}
