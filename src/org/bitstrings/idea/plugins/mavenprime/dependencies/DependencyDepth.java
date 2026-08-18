package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum DependencyDepth
{
    ALL(Integer.MAX_VALUE),
    DIRECT(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final int levels;

    DependencyDepth(int levels)
    {
        this.levels = levels;
    }

    public boolean allows(int level)
    {
        return level < levels;
    }

    public String getTitle()
    {
        return (this == ALL)
            ? MavenPrimeBundle.message("mavenprime.dependencies.depth.all")
            : MavenPrimeBundle.message("mavenprime.dependencies.depth.levels", Integer.valueOf(levels));
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
