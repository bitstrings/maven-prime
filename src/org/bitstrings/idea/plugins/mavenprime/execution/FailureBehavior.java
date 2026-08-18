package org.bitstrings.idea.plugins.mavenprime.execution;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum FailureBehavior
{
    DEFAULT("default", StringUtils.EMPTY),
    FAIL_FAST("failFast", "-ff"),
    FAIL_AT_END("failAtEnd", "-fae"),
    FAIL_NEVER("failNever", "-fn");

    private final String bundleKey;

    private final String option;

    FailureBehavior(String bundleKey, String option)
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
        return MavenPrimeBundle.message("mavenprime.failureBehavior." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
