package org.bitstrings.idea.plugins.mavenprime.util;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;

public class ReloadableCacheTest
{
    @Test
    public void get_theFirstCall_answersWithWhatTheLoaderProduced()
    {
        ReloadableCache<String> cache = new ReloadableCache<>(new Loader("loaded"));

        assertEquals("loaded", cache.get());
    }

    @Test
    public void get_aSecondCallWithNoInvalidation_answersWithoutLoadingAgain()
    {
        Loader loader = new Loader("loaded", "reloaded");

        ReloadableCache<String> cache = new ReloadableCache<>(loader);

        cache.get();
        cache.get();

        assertEquals(1, loader.loads);
    }

    @Test
    public void get_afterInvalidate_loadsTheValueAgain()
    {
        ReloadableCache<String> cache = new ReloadableCache<>(new Loader("stale", "fresh"));

        cache.get();
        cache.invalidate();

        assertEquals("fresh", cache.get());
    }

    @Test
    public void get_aLoadThatOverlappedAnInvalidate_isNotServedToLaterCallers()
    {
        Loader loader = new Loader("stale", "fresh");

        ReloadableCache<String> cache = new ReloadableCache<>(loader);

        loader.racingWith(cache);

        cache.get();

        assertEquals(
            "a value read before the source changed must never be published as current",
            "fresh",
            cache.get());
    }

    @Test
    public void get_aLoadThatOverlappedAnInvalidate_stillAnswersTheCallerThatStartedIt()
    {
        Loader loader = new Loader("stale", "fresh");

        ReloadableCache<String> cache = new ReloadableCache<>(loader);

        loader.racingWith(cache);

        assertEquals("stale", cache.get());
    }

    private static final class Loader
        implements Supplier<String>
    {
        private final Iterator<String> produced;

        private ReloadableCache<String> racing;

        private int loads;

        Loader(String... values)
        {
            produced = List.of(values).iterator();
        }

        void racingWith(ReloadableCache<String> cache)
        {
            racing = cache;
        }

        @Override
        public String get()
        {
            loads++;

            if (racing != null)
            {
                racing.invalidate();
                racing = null;
            }

            return produced.next();
        }
    }
}
