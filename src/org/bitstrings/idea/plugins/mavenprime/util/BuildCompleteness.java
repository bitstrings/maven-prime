package org.bitstrings.idea.plugins.mavenprime.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class BuildCompleteness
{
    private final AtomicLong droppedEvents = new AtomicLong();

    private final AtomicBoolean truncated = new AtomicBoolean();

    public void recordDropped(long events)
    {
        droppedEvents.addAndGet(events);
    }

    public void recordTruncated()
    {
        truncated.set(true);
    }

    public long getDroppedEvents()
    {
        return droppedEvents.get();
    }

    public boolean isTruncated()
    {
        return truncated.get();
    }

    public boolean isComplete()
    {
        return (droppedEvents.get() == 0L) && !truncated.get();
    }
}
