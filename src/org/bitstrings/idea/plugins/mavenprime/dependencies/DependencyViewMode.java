package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum DependencyViewMode
{
    TREE("tree"),
    FLAT("flat"),
    BY_SCOPE("byScope"),
    BY_GROUP_ID("byGroupId");

    private final String bundleKey;

    DependencyViewMode(String bundleKey)
    {
        this.bundleKey = bundleKey;
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.dependencies.view." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
