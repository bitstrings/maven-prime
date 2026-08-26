package org.bitstrings.idea.plugins.mavenprime.profile;

public final class ProfileAdvice
{
    private static final double RECOVERABLE_SHARE = 0.20D;

    private static final long RECOVERABLE_MILLIS = 10_000L;

    private ProfileAdvice()
    {
    }

    public static boolean isWorthReporting(BuildProfileSummary summary)
    {
        long recoverable = summary.recoverableMillis();

        return (recoverable >= RECOVERABLE_MILLIS)
            && (recoverable >= (long) (summary.wallClockMillis() * RECOVERABLE_SHARE));
    }
}
