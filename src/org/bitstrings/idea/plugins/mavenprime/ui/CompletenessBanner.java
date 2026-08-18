package org.bitstrings.idea.plugins.mavenprime.ui;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;

public final class CompletenessBanner
{
    private CompletenessBanner()
    {
    }

    public static void hide(SimpleColoredComponent target)
    {
        target.clear();
        target.setVisible(false);
    }

    public static void render(SimpleColoredComponent target, BuildCompleteness completeness)
    {
        if (completeness.isComplete())
        {
            hide(target);

            return;
        }

        target.clear();
        target.setVisible(true);
        target.setIcon(AllIcons.General.Warning);
        target.append(messageOf(completeness), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    static String messageOf(BuildCompleteness completeness)
    {
        Long dropped = Long.valueOf(completeness.getDroppedEvents());

        if (dropped.longValue() == 0L)
        {
            return MavenPrimeBundle.message("mavenprime.spy.incomplete.truncated");
        }

        return completeness.isTruncated()
            ? MavenPrimeBundle.message("mavenprime.spy.incomplete.droppedAndTruncated", dropped)
            : MavenPrimeBundle.message("mavenprime.spy.incomplete.dropped", dropped);
    }
}
