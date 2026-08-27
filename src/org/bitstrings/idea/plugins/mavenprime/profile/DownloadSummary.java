package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.DownloadTiming;

public record DownloadSummary(
    List<RepositoryTotal> repositories, long wallClockMillis, long bytes, int count)
{
    public static DownloadSummary empty()
    {
        return new DownloadSummary(List.of(), 0L, 0L, 0);
    }

    public static DownloadSummary of(List<DownloadTiming> downloads)
    {
        if (downloads.isEmpty())
        {
            return empty();
        }

        return new DownloadSummary(
            totalsOf(downloads),
            unionMillis(downloads),
            downloads.stream().mapToLong(DownloadTiming::bytes).sum(),
            downloads.size());
    }

    public boolean isEmpty()
    {
        return count == 0;
    }

    private static List<RepositoryTotal> totalsOf(List<DownloadTiming> downloads)
    {
        Map<String, List<DownloadTiming>> byRepository = new LinkedHashMap<>();

        for (DownloadTiming download : downloads)
        {
            byRepository.computeIfAbsent(download.repository(), key -> new ArrayList<>()).add(download);
        }

        return byRepository
            .entrySet()
            .stream()
            .map(entry -> totalOf(entry.getKey(), entry.getValue()))
            .sorted(
                Comparator
                    .comparingLong(RepositoryTotal::transferMillis)
                    .reversed()
                    .thenComparing(RepositoryTotal::repository))
            .toList();
    }

    private static RepositoryTotal totalOf(String repository, List<DownloadTiming> downloads)
    {
        return new RepositoryTotal(
            repository,
            downloads.size(),
            (int) downloads.stream().filter(download -> download.kind() == DownloadKind.METADATA).count(),
            downloads.stream().mapToLong(DownloadTiming::durationMillis).sum(),
            downloads.stream().mapToLong(DownloadTiming::bytes).sum());
    }

    // Transfers overlap, so their durations add up past the wall clock. The union of the intervals is
    // the part of the build during which something was on the wire.
    private static long unionMillis(List<DownloadTiming> downloads)
    {
        return intervalsOf(downloads).stream().mapToLong(Interval::durationMillis).sum();
    }

    public static List<Interval> intervalsOf(List<DownloadTiming> downloads)
    {
        List<Interval> merged = new ArrayList<>();

        for (DownloadTiming download : byStart(downloads))
        {
            Interval last = merged.isEmpty() ? null : merged.get(merged.size() - 1);

            if ((last == null) || (download.startMillis() > last.endMillis()))
            {
                merged.add(
                    new Interval(download.startMillis(), download.endMillis(), 1, download.bytes()));
            }
            else
            {
                merged.set(
                    merged.size() - 1,
                    new Interval(
                        last.startMillis(),
                        Math.max(last.endMillis(), download.endMillis()),
                        last.count() + 1,
                        last.bytes() + download.bytes()));
            }
        }

        return List.copyOf(merged);
    }

    private static List<DownloadTiming> byStart(List<DownloadTiming> downloads)
    {
        return downloads.stream().sorted(Comparator.comparingLong(DownloadTiming::startMillis)).toList();
    }

    public record RepositoryTotal(
        String repository, int count, int metadataCount, long transferMillis, long bytes)
    {
    }

    public record Interval(long startMillis, long endMillis, int count, long bytes)
    {
        public long durationMillis()
        {
            return endMillis - startMillis;
        }
    }
}
