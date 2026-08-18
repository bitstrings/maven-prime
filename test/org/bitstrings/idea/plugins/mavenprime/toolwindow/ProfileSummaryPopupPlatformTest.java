package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ReactorEdge;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ProfileSummaryPopupPlatformTest
    extends BasePlatformTestCase
{
    private static final int DEEP_CHAIN = 80;

    private static final long MODULE_MILLIS = 10L;

    public void testContent_aCriticalPathDeeperThanTheScreen_boundsThePopupInsteadOfGrowingWithIt()
    {
        JScrollPane deep = (JScrollPane) popupOverADeepChain();

        assertTrue(
            "the popup grows with the reactor, so a deep build opens it at screen height",
            deep.getPreferredSize().height < deep.getViewport().getView().getPreferredSize().height);
    }

    public void testContent_aReactorWithOneModule_staysAsShortAsItsFacts()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("org.example:only", 0L, MODULE_MILLIS));

        JScrollPane content = (JScrollPane) ProfileSummaryPopup.content(profile, profile.summarize());

        assertFalse(
            "a one-module reactor is padded out to the scrolling bound",
            content.isPreferredSizeSet());
    }

    private static JComponent popupOverADeepChain()
    {
        BuildProfile profile = new BuildProfile();

        for (int module = 0; module < DEEP_CHAIN; module++)
        {
            profile.accept(new ModuleTiming(moduleNamed(module), module * MODULE_MILLIS, MODULE_MILLIS));

            if (module > 0)
            {
                profile.accept(new ReactorEdge(moduleNamed(module), moduleNamed(module - 1)));
            }
        }

        BuildProfileSummary summary = profile.summarize();

        assertEquals(
            "the fixture has to put every module on the critical path, otherwise nothing is being measured",
            DEEP_CHAIN,
            summary.criticalPath().size());

        assertTrue(
            "the fixture has to exceed the scrolling bound, otherwise no bound is being measured",
            DEEP_CHAIN > ProfileSummaryPopup.VISIBLE_STEPS);

        return ProfileSummaryPopup.content(profile, summary);
    }

    private static String moduleNamed(int module)
    {
        return "org.example:module-" + module;
    }
}
