package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public final class DistributionSpec
{
    public DistributionKind kind;

    public String path;

    public DistributionSpec()
    {
        this(DistributionKind.CONTEXT, StringUtils.EMPTY);
    }

    public DistributionSpec(DistributionKind kind, String path)
    {
        this.kind = kind;
        this.path = StringUtils.defaultString(path);
    }

    public static DistributionSpec context()
    {
        return new DistributionSpec(DistributionKind.CONTEXT, StringUtils.EMPTY);
    }

    public static DistributionSpec ide()
    {
        return new DistributionSpec(DistributionKind.IDE, StringUtils.EMPTY);
    }

    public static DistributionSpec mavenHome(String path)
    {
        return new DistributionSpec(DistributionKind.MAVEN_HOME, path);
    }

    public static DistributionSpec daemonHome(String path)
    {
        return new DistributionSpec(DistributionKind.MVND_HOME, path);
    }

    public boolean isDaemon()
    {
        return kind.isDaemon();
    }

    public boolean isContext()
    {
        return kind == DistributionKind.CONTEXT;
    }

    public boolean isComplete()
    {
        return !kind.requiresPath() || StringUtils.isNotBlank(path);
    }

    public DistributionSpec copy()
    {
        return new DistributionSpec(kind, path);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof DistributionSpec))
        {
            return false;
        }

        DistributionSpec that = (DistributionSpec) other;

        return (kind == that.kind) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(kind, path);
    }

    @Override
    public String toString()
    {
        return kind.requiresPath() ? (kind.name() + ':' + path) : kind.name();
    }
}
