package org.bitstrings.idea.plugins.mavenprime.ui;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ProfileEntriesTest
{
    @Test
    public void merge_profilesInReactorOrder_ordersThemByNameSoBothSurfacesAgree()
    {
        assertEquals(
            "the tool window table and the profiles menu read the same list, so it has one order",
            List.of("alpha", "Beta", "gamma"),
            namesIn(ProfileEntries.merge(orderedSet("gamma", "alpha", "Beta"), Map.of())));
    }

    @Test
    public void merge_mixedCaseNames_ordersThemCaseInsensitively()
    {
        assertEquals(
            List.of("alpha", "Beta", "gamma"),
            namesIn(ProfileEntries.merge(orderedSet("Beta", "gamma", "alpha"), Map.of())));
    }

    @Test
    public void merge_aProfileSetButNoLongerDeclared_isStillListedSoItCanBeUnset()
    {
        assertEquals(
            List.of("gone", "release"),
            namesIn(ProfileEntries.merge(Set.of("release"), Map.of("gone", Boolean.TRUE))));
    }

    @Test
    public void merge_declaredAndSelectedOverlapping_listsEachProfileOnce()
    {
        assertEquals(
            List.of("release"),
            namesIn(ProfileEntries.merge(Set.of("release"), Map.of("release", Boolean.FALSE))));
    }

    @Test
    public void merge_anEnabledProfile_carriesItsStateThrough()
    {
        assertEquals(
            ProfileState.ENABLED,
            ProfileEntries.merge(Set.of("release"), Map.of("release", Boolean.TRUE)).get(0).getState());
    }

    @Test
    public void merge_aDisabledProfile_carriesItsStateThrough()
    {
        assertEquals(
            ProfileState.DISABLED,
            ProfileEntries.merge(Set.of("release"), Map.of("release", Boolean.FALSE)).get(0).getState());
    }

    @Test
    public void merge_aDeclaredProfileNobodyTouched_isReportedAsUnset()
    {
        assertEquals(
            ProfileState.NOT_SET,
            ProfileEntries.merge(Set.of("release"), Map.of()).get(0).getState());
    }

    private static Set<String> orderedSet(String... names)
    {
        return new LinkedHashSet<>(List.of(names));
    }

    private static List<String> namesIn(List<ProfileEntry> entries)
    {
        return entries.stream().map(ProfileEntry::getName).toList();
    }
}
