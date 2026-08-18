package org.bitstrings.idea.plugins.mavenprime.execution;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public enum RequestProblem
{
    NO_GOALS("noGoals"),
    NO_WORKING_DIRECTORY("noWorkingDirectory");

    private final String bundleKey;

    RequestProblem(String bundleKey)
    {
        this.bundleKey = bundleKey;
    }

    public String getMessage()
    {
        return MavenPrimeBundle.message("mavenprime.execution.error." + bundleKey);
    }
}
