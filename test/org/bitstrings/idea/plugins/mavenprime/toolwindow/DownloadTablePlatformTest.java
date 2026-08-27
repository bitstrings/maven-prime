package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.DownloadKind;
import org.bitstrings.idea.plugins.mavenprime.profile.DownloadSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.DownloadTiming;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class DownloadTablePlatformTest
    extends BasePlatformTestCase
{
    private static final String CENTRAL = "central";

    private static final String NEXUS = "nexus";

    private static final long WALL_CLOCK_MILLIS = 1000L;

    private static final int REPOSITORY_COLUMN = 0;

    private static final int FILES_COLUMN = 1;

    private static final int SHARE_COLUMN = 4;

    public void testSetSummary_repositoriesOfDifferentCost_leadsWithTheHeaviestOne()
    {
        assertEquals(NEXUS, valueAt(tableShowing(twoRepositories()), 0, REPOSITORY_COLUMN));
    }

    public void testSetSummary_aRepositoryThatCostAQuarterOfTheBuild_saysSoAgainstTheWallClock()
    {
        assertEquals(
            MavenPrimeBundle.message("mavenprime.profile.share", Double.valueOf(25.0D)),
            valueAt(tableShowing(twoRepositories()), 0, SHARE_COLUMN));
    }

    public void testSetSummary_aRepositoryThatServedMetadataToo_breaksThatOutOfTheFileCount()
    {
        DownloadSummary summary =
            DownloadSummary.of(
                List.of(download(CENTRAL, 0L, 10L), metadata(CENTRAL, 20L), metadata(CENTRAL, 40L)));

        assertEquals(
            MavenPrimeBundle.message(
                "mavenprime.profile.downloads.files", Integer.valueOf(3), Integer.valueOf(2)),
            valueAt(tableShowing(summary), 0, FILES_COLUMN));
    }

    public void testSetSummary_aRepositoryThatServedNoMetadata_showsThePlainCount()
    {
        DownloadSummary summary = DownloadSummary.of(List.of(download(CENTRAL, 0L, 10L)));

        assertEquals("1", valueAt(tableShowing(summary), 0, FILES_COLUMN));
    }

    public void testSetSummary_aBuildThatDownloadedNothing_showsNoRows()
    {
        assertEquals(0, tableShowing(DownloadSummary.empty()).getRowCount());
    }

    public void testSetSummary_asecondBuildWithFewerRepositories_dropsTheRowsOfTheFirst()
    {
        DownloadTable table = tableShowing(twoRepositories());

        table.setSummary(DownloadSummary.of(List.of(download(CENTRAL, 0L, 10L))), WALL_CLOCK_MILLIS);

        assertEquals(1, table.getRowCount());
    }

    private static DownloadTable tableShowing(DownloadSummary summary)
    {
        DownloadTable table = new DownloadTable();

        table.setSummary(summary, WALL_CLOCK_MILLIS);

        return table;
    }

    private static String valueAt(DownloadTable table, int row, int column)
    {
        return String.valueOf(table.getValueAt(row, column));
    }

    private static DownloadSummary twoRepositories()
    {
        return DownloadSummary.of(List.of(download(CENTRAL, 0L, 10L), download(NEXUS, 20L, 250L)));
    }

    private static DownloadTiming download(String repository, long start, long duration)
    {
        return new DownloadTiming(
            DownloadKind.ARTIFACT, "org.example:widget:jar:1.0", repository, start, duration, 1024L);
    }

    private static DownloadTiming metadata(String repository, long start)
    {
        return new DownloadTiming(
            DownloadKind.METADATA, "org.example:widget:1.0", repository, start, 5L, 512L);
    }
}
