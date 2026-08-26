package org.bitstrings.idea.plugins.mavenprime.profile;

public sealed interface ProfileEvent
{
    record ModuleTiming(String module, long startMillis, long durationMillis)
        implements ProfileEvent
    {
        public long endMillis()
        {
            return startMillis + durationMillis;
        }
    }

    record MojoTiming(
        String module,
        String goal,
        String executionId,
        long startMillis,
        long durationMillis)
        implements ProfileEvent
    {
        public long endMillis()
        {
            return startMillis + durationMillis;
        }
    }

    record ReactorEdge(String module, String upstream)
        implements ProfileEvent
    {
    }

    record ModuleFailed(String module)
        implements ProfileEvent
    {
    }
}
