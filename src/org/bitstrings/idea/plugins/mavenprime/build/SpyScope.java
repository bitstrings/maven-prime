package org.bitstrings.idea.plugins.mavenprime.build;

public enum SpyScope
{
    BUILD(true, true),
    MODEL(false, false);

    private final boolean timed;

    private final boolean resolution;

    SpyScope(boolean timed, boolean resolution)
    {
        this.timed = timed;
        this.resolution = resolution;
    }

    public boolean isTimed()
    {
        return timed;
    }

    public boolean collectsResolution()
    {
        return resolution;
    }
}
