package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ReactorPayoffTest
{
    private static final Map<String, Long> BRANCHED =
        Map.of("g:long", Long.valueOf(100L), "g:short", Long.valueOf(80L), "g:tail", Long.valueOf(10L));

    private static final Map<String, Set<String>> JOINS_AT_TAIL =
        Map.of("g:tail", Set.of("g:long", "g:short"));

    @Test
    public void of_aModuleOffTheCriticalPath_isWorthNothing()
    {
        assertEquals(
            "a module that is not on any longest path cannot shorten the build however fast it gets",
            0L,
            payoffOf("g:short"));
    }

    @Test
    public void of_aCriticalModuleWithASecondPathBehindIt_isWorthOnlyTheMarginToThatPath()
    {
        assertEquals(
            "reporting its whole duration promises time the second path takes straight back",
            20L,
            payoffOf("g:long"));
    }

    @Test
    public void of_aModuleEveryPathRunsThrough_isWorthItsWholeDuration()
    {
        assertEquals(10L, payoffOf("g:tail"));
    }

    @Test
    public void of_theOnlyModuleInTheBuild_isWorthItsWholeDuration()
    {
        assertEquals(
            100L,
            ReactorPayoff
                .of(Map.of("g:only", Long.valueOf(100L)), Map.of())
                .get("g:only")
                .longValue());
    }

    @Test
    public void of_aChainOfTwo_makesBothLinksWorthTheirOwnDuration()
    {
        Map<String, Long> payoff =
            ReactorPayoff.of(
                Map.of("g:first", Long.valueOf(30L), "g:second", Long.valueOf(70L)),
                Map.of("g:second", Set.of("g:first")));

        assertEquals(30L, payoff.get("g:first").longValue());
    }

    @Test
    public void of_aBuildWithNoModules_reportsNoPayoffs()
    {
        assertEquals(Map.of(), ReactorPayoff.of(Map.of(), Map.of()));
    }

    private static long payoffOf(String module)
    {
        return ReactorPayoff.of(BRANCHED, JOINS_AT_TAIL).get(module).longValue();
    }
}
