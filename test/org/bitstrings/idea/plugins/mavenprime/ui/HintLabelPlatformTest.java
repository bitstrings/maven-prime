package org.bitstrings.idea.plugins.mavenprime.ui;

import javax.swing.JComponent;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;

public class HintLabelPlatformTest
    extends BasePlatformTestCase
{
    private static final String LONG_HINT_KEY = "mavenprime.autoRefresh.hint";

    private static final String LONGER_HINT_KEY = "mavenprime.colorConsole.hint";

    public void testUnder_aLongHint_staysNarrowerThanTheSameTextOnOneLine()
    {
        int onOneLine = onOneLine().getPreferredSize().width;
        int bounded = hint().getPreferredSize().width;

        assertTrue(
            "hint width " + bounded + " should be under the single-line width " + onOneLine,
            bounded < onOneLine);
    }

    public void testUnder_twoLongHintsOfDifferentLength_clampToTheSameWidth()
    {
        JComponent shorter = HintLabel.under(new JBCheckBox("anchor"), LONG_HINT_KEY);
        JComponent longer = HintLabel.under(new JBCheckBox("anchor"), LONGER_HINT_KEY);

        assertEquals(
            "hint width must stop following the text",
            longer.getPreferredSize().width,
            shorter.getPreferredSize().width);
    }

    public void testUnder_aLongHint_wrapsOntoMoreThanOneLine()
    {
        int oneLineHigh = onOneLine().getPreferredSize().height;
        int wrapped = hint().getPreferredSize().height;

        assertTrue(
            "hint height " + wrapped + " should span several lines of " + oneLineHigh,
            wrapped > (oneLineHigh * 2));
    }

    public void testForKey_twoLongHintsOfDifferentLength_clampToTheSameWidth()
    {
        JComponent shorter = HintLabel.forKey(LONG_HINT_KEY);
        JComponent longer = HintLabel.forKey(LONGER_HINT_KEY);

        assertEquals(
            "an unanchored hint must be bounded too",
            longer.getPreferredSize().width,
            shorter.getPreferredSize().width);
    }

    private static JComponent hint()
    {
        return HintLabel.under(new JBCheckBox("anchor"), LONG_HINT_KEY);
    }

    private static JComponent onOneLine()
    {
        return new JBLabel(
            MavenPrimeBundle.message(LONG_HINT_KEY),
            UIUtil.ComponentStyle.SMALL,
            UIUtil.FontColor.BRIGHTER);
    }
}
