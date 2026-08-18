package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum DependencyFilterMode
{
    ALL("all"),
    CONFLICTS("conflicts"),
    OMITTED("omitted");

    private final String bundleKey;

    DependencyFilterMode(String bundleKey)
    {
        this.bundleKey = bundleKey;
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.dependencies.filter." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
