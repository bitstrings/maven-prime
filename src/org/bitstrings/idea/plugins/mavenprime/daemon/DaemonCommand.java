package org.bitstrings.idea.plugins.mavenprime.daemon;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum DaemonCommand
{
    STATUS("--status", "status"),
    STOP("--stop", "stop"),
    PURGE("--purge", "purge");

    private final String option;

    private final String bundleKey;

    DaemonCommand(String option, String bundleKey)
    {
        this.option = option;
        this.bundleKey = bundleKey;
    }

    public String getOption()
    {
        return option;
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.daemon.command." + bundleKey);
    }
}
