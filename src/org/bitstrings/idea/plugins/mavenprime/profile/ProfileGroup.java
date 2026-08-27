package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

public record ProfileGroup(
    String name, long startMillis, long durationMillis, long payoffMillis, List<MojoTiming> mojos)
{
    public static final long NO_PAYOFF = -1L;

    public boolean hasPayoff()
    {
        return payoffMillis >= 0L;
    }
}
