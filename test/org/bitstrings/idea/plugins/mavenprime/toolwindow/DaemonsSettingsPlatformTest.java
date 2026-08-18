package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperties;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.ui.DistributionItem;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class DaemonsSettingsPlatformTest
    extends BasePlatformTestCase
{
    private static final String MVND_HOME = "/opt/mvnd-for-test";

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            BuildContext.getInstance(getProject()).loadState(new BuildContextProperties());
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testUseForBuilds_tickedButNotApplied_leavesTheBuildContextAlone()
    {
        DaemonsPanel panel = panelWithADaemon();

        panel.useForBuildsBox.doClick();

        assertFalse(
            "ticking a settings checkbox must not change the build until Apply",
            BuildContext.getInstance(getProject()).getDistribution().isDaemon());
    }

    public void testUseForBuilds_tickedButNotApplied_reportsTheSettingsPageAsModified()
    {
        DaemonsPanel panel = panelWithADaemon();

        panel.useForBuildsBox.doClick();

        assertTrue("Apply must become available", panel.isModified());
    }

    public void testApply_aTickedDaemon_becomesTheBuildDistribution()
    {
        DaemonsPanel panel = panelWithADaemon();

        panel.useForBuildsBox.doClick();
        panel.apply();

        assertEquals(
            DistributionSpec.daemonHome(MVND_HOME),
            BuildContext.getInstance(getProject()).getDistribution());
    }

    public void testReset_aTickedButUnappliedDaemon_isDiscarded()
    {
        DaemonsPanel panel = panelWithADaemon();

        panel.useForBuildsBox.doClick();
        panel.reset();

        assertFalse("Reset must discard the pending choice", panel.isModified());
    }

    private DaemonsPanel panelWithADaemon()
    {
        DaemonsPanel panel = new DaemonsPanel(getProject());

        panel.installationCombo.addItem(
            new DistributionItem(DistributionSpec.daemonHome(MVND_HOME), "mvnd (test)"));

        return panel;
    }
}
