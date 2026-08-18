package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.ui.DistributionItem;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class DaemonsPanelPlatformTest
    extends BasePlatformTestCase
{
    private static final String VERY_LONG_HOME =
        "/home/someone/opt/tools/apache/maven/daemon/maven-mvnd-1.0.3-linux-amd64-with-a-very-long-name";

    public void testGetMinimumSize_anInstallationWithALongPath_doesNotWidenThePanel()
    {
        DaemonsPanel panel = new DaemonsPanel(getProject());

        int before = panel.getMinimumSize().width;

        panel.installationCombo.addItem(
            new DistributionItem(DistributionSpec.daemonHome(VERY_LONG_HOME), VERY_LONG_HOME));

        assertEquals(
            "the daemon list must not size itself from the installation paths",
            before,
            panel.getMinimumSize().width);
    }
}
