package org.bitstrings.idea.plugins.mavenprime.model;

public final class RefreshCoalescer
{
    private boolean running;

    private boolean owed;

    public synchronized boolean isRunning()
    {
        return running;
    }

    public synchronized boolean begin()
    {
        if (running)
        {
            owed = true;

            return false;
        }

        running = true;

        return true;
    }

    public synchronized boolean end()
    {
        running = false;

        boolean replay = owed;

        owed = false;

        return replay;
    }
}
