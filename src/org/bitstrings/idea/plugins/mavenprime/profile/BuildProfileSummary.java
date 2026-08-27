package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.List;

public record BuildProfileSummary(
    long wallClockMillis,
    long totalWorkMillis,
    long criticalPathMillis,
    List<String> criticalPath,
    int laneCount,
    Concurrency concurrency)
{
    public enum Concurrency
    {
        SEQUENTIAL,
        DEPENDENCY_BOUND,
        THREAD_STARVED
    }

    public static BuildProfileSummary empty()
    {
        return new BuildProfileSummary(0L, 0L, 0L, List.of(), 0, Concurrency.SEQUENTIAL);
    }

    public double achievedParallelism()
    {
        return (wallClockMillis == 0L) ? 0.0D : ((double) totalWorkMillis / wallClockMillis);
    }

    public double availableParallelism()
    {
        return (criticalPathMillis == 0L)
            ? achievedParallelism()
            : ((double) totalWorkMillis / criticalPathMillis);
    }

    public int usefulThreads()
    {
        return (criticalPathMillis == 0L)
            ? Math.max(1, laneCount)
            : (int) Math.max(1L, divideCeil(totalWorkMillis, criticalPathMillis));
    }

    public long workBoundMillis()
    {
        return (laneCount <= 0) ? totalWorkMillis : divideCeil(totalWorkMillis, laneCount);
    }

    public long lowerBoundMillis()
    {
        return Math.max(criticalPathMillis, workBoundMillis());
    }

    public long recoverableMillis()
    {
        return Math.max(0L, wallClockMillis - criticalPathMillis);
    }

    public long schedulingLossMillis()
    {
        return Math.max(0L, wallClockMillis - lowerBoundMillis());
    }

    static long divideCeil(long value, long divisor)
    {
        return (divisor <= 0L) ? value : Math.ceilDiv(value, divisor);
    }
}
