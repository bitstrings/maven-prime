package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.List;

import javax.swing.SwingUtilities;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.profile.DownloadKind;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.DownloadTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

import com.intellij.openapi.util.text.Formats;
import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BuildProfileViewPlatformTest
    extends BasePlatformTestCase
{
    private static final List<String> PARTS =
        List.of(BuildProfilePanel.SUMMARY_KEY, BuildProfilePanel.TIMELINE_KEY, BuildProfilePanel.DETAILS_KEY);

    private static final String MODULE = "org.example:core";

    private static final String GOAL = "compiler:compile";

    private static final String EXECUTION = "default-compile";

    private static final String GOALS = "clean install -DskipTests";

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            RunAll.runAll(
                () ->
                {
                    BuildProfilePanel panel = new BuildProfilePanel(getProject());

                    PARTS.forEach(part -> panel.show(part, true));

                    panel.show(BuildProfilePanel.DOWNLOADS_KEY, false);
                },
                () -> BuildProfileService.getInstance(getProject()).startBuild(getName(), ""));
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testIsShown_aProfilerOpenedForTheFirstTime_showsEveryPart()
    {
        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        PARTS.forEach(part -> assertTrue(part, panel.isShown(part)));
    }

    public void testShow_detailsTurnedOff_takesTheDetailsPaneOutOfTheTab()
    {
        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.show(BuildProfilePanel.DETAILS_KEY, false);

        assertNull("a hidden part must leave the splitter, not sit there empty", panel.details.getParent());
    }

    public void testShow_timelineTurnedOff_takesTheChartOutOfTheTab()
    {
        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.show(BuildProfilePanel.TIMELINE_KEY, false);

        assertNull(panel.chart.getParent());
    }

    public void testShow_everyPartTurnedOff_keepsTheToolbarThatTurnsThemBackOn()
    {
        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        PARTS.forEach(part -> panel.show(part, false));

        assertTrue(
            "hiding every part must leave the toggles reachable, or there is no way back",
            SwingUtilities.isDescendingFrom(panel.toolbar, panel));
    }

    public void testShow_summaryTurnedOff_hidesTheHeader()
    {
        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.show(BuildProfilePanel.SUMMARY_KEY, false);

        assertFalse(panel.header.isVisible());
    }

    public void testShow_aPartTurnedOff_staysOffForTheNextProfilerOpenedInThisProject()
    {
        new BuildProfilePanel(getProject()).show(BuildProfilePanel.DETAILS_KEY, false);

        assertFalse(
            "the choice must survive reopening the tool window",
            new BuildProfilePanel(getProject()).isShown(BuildProfilePanel.DETAILS_KEY));
    }

    public void testRefresh_aBuildThatReportedNoModuleTimings_drawsNoVerdict()
    {
        BuildProfileService
            .getInstance(getProject())
            .startBuild(getName(), "")
            .accept(new MojoTiming(MODULE, GOAL, EXECUTION, 0L, 100L));

        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.refresh();

        assertFalse(
            "a build that reported no module timings has nothing to conclude from, so the summary"
                + " must not state a verdict computed from zeros",
            panel.metrics
                .getCharSequence(false)
                .toString()
                .contains(MavenPrimeBundle.message("mavenprime.profile.badge.sequential")));
    }

    public void testShow_aPartTurnedOffThenOn_comesBack()
    {
        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.show(BuildProfilePanel.DETAILS_KEY, false);
        panel.show(BuildProfilePanel.DETAILS_KEY, true);

        assertNotNull(panel.details.getParent());
    }

    public void testIsShown_aProfilerOpenedForTheFirstTime_leavesTheDownloadsPaneOut()
    {
        assertFalse(
            "most builds resolve everything locally, so opening on an empty downloads table would "
                + "cost every user room for a table that says nothing",
            new BuildProfilePanel(getProject()).isShown(BuildProfilePanel.DOWNLOADS_KEY));
    }

    public void testShow_downloadsTurnedOn_putsThatPaneInTheTab()
    {
        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.show(BuildProfilePanel.DOWNLOADS_KEY, true);

        assertNotNull(panel.downloads.getParent());
    }

    public void testShow_detailsTurnedOff_takesTheGroupingBarWithIt()
    {
        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.show(BuildProfilePanel.DETAILS_KEY, false);

        assertFalse(
            "grouping, sorting and filtering act on the details table alone, so the bar is dead "
                + "controls once that table is gone",
            panel.filters.isVisible());
    }

    public void testRefresh_aBuildThatDownloadedNothing_saysNothingAboutDownloadingInTheHeader()
    {
        BuildProfileService
            .getInstance(getProject())
            .startBuild(getName(), "")
            .accept(new ModuleTiming(MODULE, 0L, 100L));

        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.refresh();

        assertFalse(headerOf(panel).contains(downloadingFor(0L)));
    }

    public void testRefresh_aBuildThatDownloaded_reportsTheStretchItSpentOnTheWireInTheHeader()
    {
        BuildProfile profile = BuildProfileService.getInstance(getProject()).startBuild(getName(), "");

        profile.accept(new ModuleTiming(MODULE, 0L, 1000L));
        profile.accept(
            new DownloadTiming(DownloadKind.ARTIFACT, "g:a:jar:1", "central", 0L, 250L, 2048L));

        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.refresh();

        assertTrue(
            "the time a build spent downloading is invisible in Maven's own output, and the header is "
                + "the one line every user reads",
            headerOf(panel).contains(downloadingFor(250L)));
    }

    public void testRefresh_aBuildRecordedWithItsGoals_reachesThemFromTheHeader()
    {
        BuildProfileService
            .getInstance(getProject())
            .startBuild(getName(), GOALS)
            .accept(new ModuleTiming(MODULE, 0L, 100L));

        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.refresh();

        assertEquals(
            "the goal line answers what produced these numbers, and the header is the one part always "
                + "on screen",
            MavenPrimeBundle.message("mavenprime.build.goals", GOALS),
            panel.metrics.getToolTipText());
    }

    public void testRefresh_aBuildWhoseGoalsWereNeverRecorded_leavesTheHeaderWithoutATooltip()
    {
        BuildProfileService
            .getInstance(getProject())
            .startBuild(getName(), "")
            .accept(new ModuleTiming(MODULE, 0L, 100L));

        BuildProfilePanel panel = new BuildProfilePanel(getProject());

        panel.refresh();

        assertNull(panel.metrics.getToolTipText());
    }

    private static String downloadingFor(long millis)
    {
        return MavenPrimeBundle.message(
            "mavenprime.profile.downloading", Formats.formatDuration(millis));
    }

    private static String headerOf(BuildProfilePanel panel)
    {
        return panel.metrics.getCharSequence(false).toString();
    }
}
