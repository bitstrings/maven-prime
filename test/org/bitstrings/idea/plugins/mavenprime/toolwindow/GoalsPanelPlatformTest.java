package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class GoalsPanelPlatformTest
    extends BasePlatformTestCase
{
    private static final int WIDTH = 400;

    private static final int HEIGHT = 300;

    public void testDoLayout_aTrustedProjectHidingTheTrustNotice_leavesTheGoalsTheWholeHeight()
    {
        GoalsPanel panel = new GoalsPanel(getProject(), () -> null);

        panel.setSize(WIDTH, HEIGHT);
        panel.doLayout();

        assertEquals(
            "a hidden notice must take no height, or every goals tab loses a strip of the table",
            HEIGHT,
            ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER).getHeight());
    }
}
