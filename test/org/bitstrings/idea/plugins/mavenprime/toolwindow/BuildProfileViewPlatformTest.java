package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.List;

import javax.swing.SwingUtilities;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

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
                },
                () -> BuildProfileService.getInstance(getProject()).startBuild(getName()));
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
            .startBuild(getName())
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
}
