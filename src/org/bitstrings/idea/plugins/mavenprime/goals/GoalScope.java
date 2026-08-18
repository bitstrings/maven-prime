package org.bitstrings.idea.plugins.mavenprime.goals;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum GoalScope
{
    GLOBAL("global"),
    PROJECT("project");

    private final String bundleKey;

    GoalScope(String bundleKey)
    {
        this.bundleKey = bundleKey;
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.goalScope." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
