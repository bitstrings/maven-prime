package org.bitstrings.idea.plugins.mavenprime.dependencies;

public record DependencyView(DependencyViewMode mode, DependencySortMode sort, DependencyDepth depth)
{
    public static final DependencyView DEFAULT =
        new DependencyView(DependencyViewMode.TREE, DependencySortMode.RESOLUTION, DependencyDepth.ALL);
}
