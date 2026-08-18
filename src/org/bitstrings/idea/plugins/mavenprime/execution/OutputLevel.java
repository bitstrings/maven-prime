package org.bitstrings.idea.plugins.mavenprime.execution;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum OutputLevel
{
    DEFAULT("default", StringUtils.EMPTY),
    QUIET("quiet", "-q"),
    DEBUG("debug", "-X");

    private final String bundleKey;

    private final String option;

    OutputLevel(String bundleKey, String option)
    {
        this.bundleKey = bundleKey;
        this.option = option;
    }

    public String getOption()
    {
        return option;
    }

    public boolean hasOption()
    {
        return StringUtils.isNotEmpty(option);
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.outputLevel." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
