package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;

import com.intellij.openapi.util.text.Formats;

public final class ProfileVerdict
{
    private ProfileVerdict()
    {
    }

    public static String badgeOf(BuildProfileSummary summary)
    {
        return MavenPrimeBundle.message(
            switch (summary.concurrency())
            {
                case SEQUENTIAL -> "mavenprime.profile.badge.sequential";
                case DEPENDENCY_BOUND -> "mavenprime.profile.badge.dependencyBound";
                case THREAD_STARVED -> "mavenprime.profile.badge.threadStarved";
            });
    }

    public static String sentenceOf(BuildProfileSummary summary)
    {
        String verdict = messageOf(summary);

        return (summary.schedulingLossMillis() <= 0L)
            ? verdict
            : verdict + ' ' + MavenPrimeBundle.message(
                "mavenprime.profile.schedulingLoss",
                Formats.formatDuration(summary.schedulingLossMillis()),
                Integer.valueOf(summary.laneCount()),
                Formats.formatDuration(summary.lowerBoundMillis()));
    }

    private static String messageOf(BuildProfileSummary summary)
    {
        return switch (summary.concurrency())
        {
            case SEQUENTIAL ->
                MavenPrimeBundle.message(
                    "mavenprime.profile.verdict.sequential",
                    Double.valueOf(summary.availableParallelism()),
                    Integer.valueOf(summary.usefulThreads()),
                    Formats.formatDuration(summary.wallClockMillis()),
                    Formats.formatDuration(summary.criticalPathMillis()));
            case DEPENDENCY_BOUND ->
                MavenPrimeBundle.message(
                    "mavenprime.profile.verdict.dependencyBound",
                    Formats.formatDuration(summary.criticalPathMillis()),
                    Formats.formatDuration(summary.wallClockMillis()),
                    Integer.valueOf(summary.laneCount()),
                    Integer.valueOf(summary.usefulThreads()));
            case THREAD_STARVED ->
                MavenPrimeBundle.message(
                    "mavenprime.profile.verdict.threadStarved",
                    Integer.valueOf(summary.laneCount()),
                    Integer.valueOf(summary.usefulThreads()),
                    Formats.formatDuration(summary.recoverableMillis()),
                    Formats.formatDuration(summary.criticalPathMillis()));
        };
    }
}
