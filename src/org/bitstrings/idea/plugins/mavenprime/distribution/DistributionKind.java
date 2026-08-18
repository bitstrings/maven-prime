package org.bitstrings.idea.plugins.mavenprime.distribution;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum DistributionKind
{
    CONTEXT("mavenprime.distribution.context"),
    IDE("mavenprime.distribution.ide"),
    MAVEN_HOME("mavenprime.distribution.mavenHome"),
    MVND_HOME("mavenprime.distribution.mvndHome");

    private final String titleKey;

    DistributionKind(String titleKey)
    {
        this.titleKey = titleKey;
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message(titleKey);
    }

    public boolean isDaemon()
    {
        return this == MVND_HOME;
    }

    public boolean requiresPath()
    {
        return (this == MAVEN_HOME) || (this == MVND_HOME);
    }
}
