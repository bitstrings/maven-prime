package org.bitstrings.idea.plugins.mavenprime.config;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionKind;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

public final class DistributionConfig
{
    public String kind;

    public String path;

    public DistributionConfig()
    {
    }

    public static DistributionConfig of(DistributionSpec spec)
    {
        DistributionConfig config = new DistributionConfig();

        config.kind = spec.kind.name();
        config.path = StringUtils.trimToNull(spec.path);

        return config;
    }

    public DistributionSpec toSpec()
    {
        return new DistributionSpec(
            EnumUtils.getEnum(DistributionKind.class, kind, DistributionKind.CONTEXT),
            StringUtils.defaultString(path));
    }
}
