package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum ToolWindowTab
{
    CONTEXT("context"),
    GOALS("goals"),
    NAVIGATOR("navigator"),
    MODEL("model"),
    PROFILER("profiler"),
    REPOSITORY("repository"),
    DAEMONS("daemons");

    private final String bundleKey;

    ToolWindowTab(String bundleKey)
    {
        this.bundleKey = bundleKey;
    }

    public static List<ToolWindowTab> visible(boolean daemonInUse)
    {
        List<ToolWindowTab> visible = new ArrayList<>();

        for (ToolWindowTab tab : values())
        {
            if ((tab != DAEMONS) || daemonInUse)
            {
                visible.add(tab);
            }
        }

        return visible;
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.tab." + bundleKey);
    }
}
