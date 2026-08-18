package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary.Concurrency;
import org.junit.Test;

public class BuildProfileRecordedSessionTest
{
    private static final List<String> PARALLEL_SESSION =
        List.of(
            "REACTOR_EDGE\torg.example.spycheck:core\torg.example.spycheck:reactor",
            "REACTOR_EDGE\torg.example.spycheck:app\torg.example.spycheck:reactor",
            "REACTOR_EDGE\torg.example.spycheck:app\torg.example.spycheck:core",
            "PROJECT_TIMING\torg.example.spycheck:reactor\t3\t90\tmvn-builder-reactor",
            "PROJECT_TIMING\torg.example.spycheck:core\t94\t438\tmvn-builder-core",
            "PROJECT_TIMING\torg.example.spycheck:app\t532\t45\tmvn-builder-app");

    @Test
    public void summarize_recordedParallelSession_findsTheChainAsTheCriticalPath()
    {
        assertEquals(
            List.of(
                "org.example.spycheck:reactor",
                "org.example.spycheck:core",
                "org.example.spycheck:app"),
            replay().criticalPath());
    }

    @Test
    public void summarize_recordedParallelSession_reportsNoParallelismDespiteThreeBuilderThreads()
    {
        assertEquals(Concurrency.SEQUENTIAL, replay().concurrency());
    }

    @Test
    public void summarize_recordedParallelSession_measuresTheCriticalPathAsNearlyTheWholeBuild()
    {
        BuildProfileSummary summary = replay();

        assertEquals(574L, summary.wallClockMillis());
        assertEquals(573L, summary.criticalPathMillis());
    }

    private static BuildProfileSummary replay()
    {
        BuildProfile profile = new BuildProfile();

        for (String line : PARALLEL_SESSION)
        {
            ProfileEvents.parse(line).ifPresent(profile::accept);
        }

        return profile.summarize();
    }
}
