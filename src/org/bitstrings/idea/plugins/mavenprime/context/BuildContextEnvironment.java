package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

public final class BuildContextEnvironment
{
    public DistributionSpec distribution = DistributionSpec.context();

    public String settingsFile = StringUtils.EMPTY;

    public String jreName = StringUtils.EMPTY;

    public String vmOptions = StringUtils.EMPTY;

    public BuildContextEnvironment()
    {
    }

    public DistributionSpec getDistribution()
    {
        return ((distribution == null) || distribution.isContext())
            ? DistributionSpec.ide()
            : distribution.copy();
    }

    public DistributionSpec rawDistribution()
    {
        return (distribution == null) ? DistributionSpec.context() : distribution.copy();
    }

    public boolean isDeclared()
    {
        return !rawDistribution().isContext()
            || StringUtils.isNotBlank(settingsFile)
            || StringUtils.isNotBlank(jreName)
            || StringUtils.isNotBlank(vmOptions);
    }

    public BuildContextEnvironment copy()
    {
        BuildContextEnvironment copy = new BuildContextEnvironment();

        copy.distribution = rawDistribution();
        copy.settingsFile = StringUtils.defaultString(settingsFile);
        copy.jreName = StringUtils.defaultString(jreName);
        copy.vmOptions = StringUtils.defaultString(vmOptions);

        return copy;
    }

    public boolean resolvesLike(BuildContextEnvironment other)
    {
        return (other != null)
            && Objects.equals(getDistribution(), other.getDistribution())
            && Objects.equals(
                StringUtils.defaultString(settingsFile), StringUtils.defaultString(other.settingsFile));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof BuildContextEnvironment))
        {
            return false;
        }

        BuildContextEnvironment that = (BuildContextEnvironment) other;

        return getDistribution().equals(that.getDistribution())
            && Objects.equals(settingsFile, that.settingsFile)
            && Objects.equals(jreName, that.jreName)
            && Objects.equals(vmOptions, that.vmOptions);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getDistribution(), settingsFile, jreName, vmOptions);
    }
}
