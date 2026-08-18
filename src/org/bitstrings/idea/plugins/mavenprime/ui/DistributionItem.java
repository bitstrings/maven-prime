package org.bitstrings.idea.plugins.mavenprime.ui;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;

public record DistributionItem(DistributionSpec spec, String label)
{
    private static final String LOCATION_SEPARATOR = " - ";

    public static DistributionItem of(DistributionSpec spec, MavenInstallation installation)
    {
        String location =
            installation.getHome().map(Path::toString).filter(StringUtils::isNotBlank).orElse(spec.path);

        return new DistributionItem(
            spec,
            StringUtils.isBlank(location)
                ? installation.getDisplayName()
                : (installation.getDisplayName() + LOCATION_SEPARATOR + location));
    }

    @Override
    public String toString()
    {
        return label;
    }
}
