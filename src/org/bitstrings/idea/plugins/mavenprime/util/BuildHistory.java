package org.bitstrings.idea.plugins.mavenprime.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

public final class BuildHistory<T>
{
    public static final int MAX_RETAINED = 20;

    private final Object lock = new Object();

    private final List<BuildEntry<T>> entries = new ArrayList<>();

    private final Supplier<T> empty;

    private int selected = -1;

    public BuildHistory(Supplier<T> empty)
    {
        this.empty = empty;
    }

    public T start(String name, T data)
    {
        synchronized (lock)
        {
            if (entries.size() >= MAX_RETAINED)
            {
                entries.remove(0);
            }

            entries.add(new BuildEntry<>(StringUtils.defaultString(name), System.currentTimeMillis(), data));

            selected = entries.size() - 1;
        }

        return data;
    }

    public List<BuildEntry<T>> getEntries()
    {
        synchronized (lock)
        {
            return List.copyOf(entries);
        }
    }

    public BuildEntry<T> getSelected()
    {
        synchronized (lock)
        {
            return ((selected < 0) || (selected >= entries.size())) ? null : entries.get(selected);
        }
    }

    public T getSelectedData()
    {
        BuildEntry<T> entry = getSelected();

        return (entry == null) ? empty.get() : entry.data();
    }

    public boolean select(BuildEntry<T> entry)
    {
        synchronized (lock)
        {
            int index = entries.indexOf(entry);

            if ((index < 0) || (index == selected))
            {
                return false;
            }

            selected = index;
        }

        return true;
    }

    public boolean isShowingLatest()
    {
        synchronized (lock)
        {
            return selected == (entries.size() - 1);
        }
    }

    public record BuildEntry<T>(String name, long startedMillis, T data)
    {
    }
}
