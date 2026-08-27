package org.bitstrings.idea.plugins.mavenprime.profile;

import org.apache.commons.lang3.StringUtils;

public sealed interface ProfileEvent
{
    record ModuleTiming(String module, long startMillis, long durationMillis, String worker)
        implements ProfileEvent
    {
        public ModuleTiming(String module, long startMillis, long durationMillis)
        {
            this(module, startMillis, durationMillis, StringUtils.EMPTY);
        }

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
        long durationMillis,
        String phase,
        String worker)
        implements ProfileEvent
    {
        public MojoTiming(
            String module, String goal, String executionId, long startMillis, long durationMillis)
        {
            this(module, goal, executionId, startMillis, durationMillis, StringUtils.EMPTY, StringUtils.EMPTY);
        }

        public long endMillis()
        {
            return startMillis + durationMillis;
        }
    }

    record DownloadTiming(
        DownloadKind kind,
        String coordinates,
        String repository,
        long startMillis,
        long durationMillis,
        long bytes)
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
