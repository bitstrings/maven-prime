package org.bitstrings.idea.plugins.mavenprime.config;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;

public final class EnvironmentConfig
{
    public DistributionConfig distribution;

    public String settingsFile;

    public String jre;

    public String vmOptions;

    public EnvironmentConfig()
    {
    }

    public BuildContextEnvironment toEnvironment()
    {
        BuildContextEnvironment environment = new BuildContextEnvironment();

        if (distribution != null)
        {
            environment.distribution = distribution.toSpec();
        }

        environment.settingsFile = StringUtils.trimToEmpty(settingsFile);
        environment.jreName = StringUtils.trimToEmpty(jre);
        environment.vmOptions = StringUtils.trimToEmpty(vmOptions);

        return environment;
    }
}
