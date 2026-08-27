package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.profile.DownloadSummary.RepositoryTotal;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.DownloadTiming;
import org.junit.Test;

public class DownloadSummaryTest
{
    private static final String CENTRAL = "central";

    private static final String NEXUS = "nexus";

    @Test
    public void of_transfersRunningAtTheSameTime_countsThatStretchOfTheBuildOnce()
    {
        DownloadSummary summary =
            DownloadSummary.of(
                List.of(download(CENTRAL, 0L, 100L, 10L), download(CENTRAL, 50L, 100L, 10L)));

        assertEquals(
            "adding the durations of parallel transfers reports more downloading than the build lasted",
            150L,
            summary.wallClockMillis());
    }

    @Test
    public void of_transfersRunningAtTheSameTime_stillAddsTheirTransferTimes()
    {
        DownloadSummary summary =
            DownloadSummary.of(
                List.of(download(CENTRAL, 0L, 100L, 10L), download(CENTRAL, 50L, 100L, 10L)));

        assertEquals(200L, summary.repositories().get(0).transferMillis());
    }

    @Test
    public void of_transfersFromTwoRepositories_leadsWithTheOneThatCostTheMost()
    {
        DownloadSummary summary =
            DownloadSummary.of(
                List.of(download(CENTRAL, 0L, 10L, 1L), download(NEXUS, 0L, 500L, 1L)));

        assertEquals(NEXUS, summary.repositories().get(0).repository());
    }

    @Test
    public void of_metadataAlongsideArtifacts_countsTheMetadataApart()
    {
        DownloadSummary summary =
            DownloadSummary.of(
                List.of(
                    download(CENTRAL, 0L, 10L, 1L),
                    metadata(CENTRAL, 20L, 10L),
                    metadata(CENTRAL, 40L, 10L)));

        RepositoryTotal total = summary.repositories().get(0);

        assertEquals(2, total.metadataCount());
    }

    @Test
    public void of_metadataAlongsideArtifacts_countsEverythingItFetchedInTheFileCount()
    {
        DownloadSummary summary =
            DownloadSummary.of(List.of(download(CENTRAL, 0L, 10L, 1L), metadata(CENTRAL, 20L, 10L)));

        assertEquals(2, summary.repositories().get(0).count());
    }

    @Test
    public void of_severalTransfers_addsUpWhatTheyPulledOverTheWire()
    {
        DownloadSummary summary =
            DownloadSummary.of(
                List.of(download(CENTRAL, 0L, 10L, 1024L), download(NEXUS, 20L, 10L, 2048L)));

        assertEquals(3072L, summary.bytes());
    }

    @Test
    public void of_noTransfersAtAll_isEmpty()
    {
        assertTrue(DownloadSummary.of(List.of()).isEmpty());
    }

    @Test
    public void intervalsOf_transfersThatTouchWithoutOverlapping_mergeIntoOneStretch()
    {
        assertEquals(
            1,
            DownloadSummary
                .intervalsOf(List.of(download(CENTRAL, 0L, 100L, 1L), download(CENTRAL, 100L, 50L, 1L)))
                .size());
    }

    @Test
    public void intervalsOf_transfersSeparatedByRealWork_stayApart()
    {
        assertEquals(
            2,
            DownloadSummary
                .intervalsOf(List.of(download(CENTRAL, 0L, 10L, 1L), download(CENTRAL, 500L, 10L, 1L)))
                .size());
    }

    @Test
    public void intervalsOf_aMergedStretch_carriesEverythingItPulled()
    {
        assertEquals(
            3072L,
            DownloadSummary
                .intervalsOf(
                    List.of(download(CENTRAL, 0L, 100L, 1024L), download(CENTRAL, 50L, 50L, 2048L)))
                .get(0)
                .bytes());
    }

    private static DownloadTiming download(String repository, long start, long duration, long bytes)
    {
        return new DownloadTiming(
            DownloadKind.ARTIFACT, "org.example:widget:jar:1.0", repository, start, duration, bytes);
    }

    private static DownloadTiming metadata(String repository, long start, long duration)
    {
        return new DownloadTiming(
            DownloadKind.METADATA, "org.example:widget:1.0", repository, start, duration, 512L);
    }
}
