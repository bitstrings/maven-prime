package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperties;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.ui.UIUtil;

public class ToolWindowTabStripPlatformTest
    extends BasePlatformTestCase
{
    private static final String DAEMON_HOME = "/opt/maven-mvnd";

    private static final int PANEL_WIDTH = 800;

    private static final int PANEL_HEIGHT = 600;

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            RunAll.runAll(
                () -> BuildContext.getInstance(getProject()).loadState(new BuildContextProperties()));
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testTabs_theBuildSwitchingToTheDaemon_showsTheDaemonTab()
    {
        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            useDaemon(panel, false);

            useDaemon(panel, true);

            assertFalse(
                "the Daemons tab stayed hidden after the build switched to mvnd",
                daemonTabOf(panel).isHidden());
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    public void testTabs_theBuildSwitchingOffTheDaemon_hidesTheDaemonTab()
    {
        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            useDaemon(panel, true);

            useDaemon(panel, false);

            assertTrue(
                "the Daemons tab is offered for a build that cannot use a daemon",
                daemonTabOf(panel).isHidden());
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    public void testTabs_anyDistribution_keepsEveryTabInTheStrip()
    {
        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            useDaemon(panel, false);

            assertEquals(
                "a tab dropped from the strip is rebuilt on every daemon toggle",
                ToolWindowTab.values().length,
                panel.tabs.getTabCount());
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    public void testTabs_theDaemonTabComingBack_goesBackToItsOwnPosition()
    {
        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            useDaemon(panel, false);

            useDaemon(panel, true);

            assertEquals(
                "the tab was rebuilt at the end of the strip instead of returning to its own slot",
                ToolWindowTab.DAEMONS.ordinal(),
                panel.tabs.getIndexOf(daemonTabOf(panel)));
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    private static TabInfo daemonTabOf(MavenPrimeToolWindowPanel panel)
    {
        return panel.tabs
            .getTabs()
            .stream()
            .filter(tab -> tab.getObject() == ToolWindowTab.DAEMONS)
            .findFirst()
            .orElseThrow(() -> new AssertionError("the Daemons tab was never added to the strip"));
    }

    private void useDaemon(MavenPrimeToolWindowPanel panel, boolean daemon)
    {
        BuildContext context = BuildContext.getInstance(getProject());

        BuildContextEnvironment environment = context.getEnvironment();

        environment.distribution =
            daemon ? DistributionSpec.daemonHome(DAEMON_HOME) : DistributionSpec.ide();

        context.setEnvironment(environment);

        UIUtil.dispatchAllInvocationEvents();

        panel.setSize(PANEL_WIDTH, PANEL_HEIGHT);
        panel.doLayout();
    }
}
