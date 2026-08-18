package org.bitstrings.idea.plugins.mavenprime.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ReloadableCache<T>
{
    private final Supplier<T> loader;

    private final AtomicLong generation = new AtomicLong();

    private final AtomicReference<Stamped<T>> value = new AtomicReference<>();

    public ReloadableCache(Supplier<T> loader)
    {
        this.loader = loader;
    }

    public T get()
    {
        long stamp = generation.get();
        Stamped<T> current = value.get();

        if ((current != null) && (current.generation() == stamp))
        {
            return current.value();
        }

        T loaded = loader.get();

        value.compareAndSet(current, new Stamped<>(stamp, loaded));

        return loaded;
    }

    public void invalidate()
    {
        generation.incrementAndGet();
    }

    private record Stamped<T>(long generation, T value)
    {
    }
}
