package org.bitstrings.idea.plugins.mavenprime.goals;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class GoalIdsTest
{
    @Test
    public void slug_aPlainGoalLine_isUsedAsItStands()
    {
        assertEquals("install", GoalIds.slug("install"));
    }

    @Test
    public void slug_aGoalLineWithSpaces_joinsThemWithHyphens()
    {
        assertEquals("clean-install", GoalIds.slug("clean install"));
    }

    @Test
    public void slug_pluginCoordinatesAndFlags_dropEveryPunctuationRun()
    {
        assertEquals("dependency-resolve-u", GoalIds.slug("dependency:resolve -U"));
    }

    @Test
    public void slug_aNameWithBracketsAndCase_readsAsLowercaseWords()
    {
        assertEquals("clean-install-skip-tests", GoalIds.slug("Clean Install (skip tests)"));
    }

    @Test
    public void slug_leadingAndTrailingPunctuation_isNotLeftOnTheIdentifier()
    {
        assertEquals("verify", GoalIds.slug("  -- verify --  "));
    }

    @Test
    public void slug_theSameTextTwice_yieldsTheSameIdentifier()
    {
        assertEquals(GoalIds.slug("clean install"), GoalIds.slug("clean install"));
    }

    @Test
    public void slug_textWithNothingUsable_fallsBackToAReadableName()
    {
        assertEquals(GoalIds.FALLBACK, GoalIds.slug("!!!"));
    }

    @Test
    public void slug_aBlankValue_fallsBackToAReadableName()
    {
        assertEquals(GoalIds.FALLBACK, GoalIds.slug(StringUtils.SPACE));
    }

    @Test
    public void uniqueIn_anIdentifierNobodyHolds_isKept()
    {
        assertEquals("verify", GoalIds.uniqueIn("verify", Set.of("install")));
    }

    @Test
    public void uniqueIn_anIdentifierAlreadyTaken_gainsACountingSuffix()
    {
        assertEquals("verify-2", GoalIds.uniqueIn("verify", Set.of("verify")));
    }

    @Test
    public void uniqueIn_aRunOfTakenIdentifiers_countsPastAllOfThem()
    {
        assertEquals("verify-4", GoalIds.uniqueIn("verify", List.of("verify", "verify-2", "verify-3")));
    }
}
