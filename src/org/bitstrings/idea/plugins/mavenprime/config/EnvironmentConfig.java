package org.bitstrings.idea.plugins.mavenprime.config;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

public final class EnvironmentConfig
{
    public DistributionConfig distribution;

    public String settingsFile;

    public String jre;

    public String vmOptions;

    public EnvironmentConfig()
    {
    }

    public static EnvironmentConfig of(BuildContextEnvironment environment)
    {
        EnvironmentConfig config = new EnvironmentConfig();

        DistributionSpec spec = environment.rawDistribution();

        config.distribution = spec.isContext() ? null : DistributionConfig.of(spec);
        config.settingsFile = StringUtils.trimToNull(environment.settingsFile);
        config.jre = StringUtils.trimToNull(environment.jreName);
        config.vmOptions = StringUtils.trimToNull(environment.vmOptions);

        return config;
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
