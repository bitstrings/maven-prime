package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.Objects;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

public final class EnvironmentOverride
{
    public DistributionSpec distribution;

    public String settingsFile;

    public String jreName;

    public String vmOptions;

    public EnvironmentOverride()
    {
    }

    public boolean isEmpty()
    {
        return (distribution == null)
            && (settingsFile == null)
            && (jreName == null)
            && (vmOptions == null);
    }

    public EnvironmentOverride copy()
    {
        EnvironmentOverride copy = new EnvironmentOverride();

        copy.distribution = (distribution == null) ? null : distribution.copy();
        copy.settingsFile = settingsFile;
        copy.jreName = jreName;
        copy.vmOptions = vmOptions;

        return copy;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof EnvironmentOverride))
        {
            return false;
        }

        EnvironmentOverride that = (EnvironmentOverride) other;

        return Objects.equals(distribution, that.distribution)
            && Objects.equals(settingsFile, that.settingsFile)
            && Objects.equals(jreName, that.jreName)
            && Objects.equals(vmOptions, that.vmOptions);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(distribution, settingsFile, jreName, vmOptions);
    }
}
