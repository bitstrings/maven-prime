package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

public final class ProfileGroups
{
    private ProfileGroups()
    {
    }

    public static List<ProfileGroup> of(BuildProfile profile, ProfileView view)
    {
        boolean byModule = view.grouping().isByModule();

        Map<String, ModuleTiming> spans = byModule ? spansOf(profile) : Map.of();
        Map<String, Long> payoff = byModule ? profile.getPayoff() : Map.of();

        List<ProfileGroup> groups = new ArrayList<>();

        for (Map.Entry<String, List<MojoTiming>> entry : retained(profile, view).entrySet())
        {
            ModuleTiming span = spans.get(entry.getKey());

            groups.add(
                new ProfileGroup(
                    entry.getKey(),
                    (span == null) ? startOf(entry.getValue()) : span.startMillis(),
                    (span == null) ? workOf(entry.getValue()) : span.durationMillis(),
                    payoff.getOrDefault(entry.getKey(), Long.valueOf(ProfileGroup.NO_PAYOFF)).longValue(),
                    membersOf(entry.getValue(), view)));
        }

        groups.sort(orderOf(view));

        return List.copyOf(groups);
    }

    private static String textOf(MojoTiming mojo)
    {
        return mojo.module() + ' ' + mojo.goal() + ' ' + mojo.executionId() + ' ' + mojo.phase();
    }

    private static Map<String, List<MojoTiming>> retained(BuildProfile profile, ProfileView view)
    {
        Map<String, List<MojoTiming>> retained = new LinkedHashMap<>();

        for (Map.Entry<String, List<MojoTiming>> entry : grouped(profile, view).entrySet())
        {
            if (view.matches(entry.getKey()))
            {
                retained.put(entry.getKey(), entry.getValue());

                continue;
            }

            List<MojoTiming> matching =
                entry.getValue().stream().filter(mojo -> view.matches(textOf(mojo))).toList();

            if (!matching.isEmpty())
            {
                retained.put(entry.getKey(), matching);
            }
        }

        return retained;
    }

    private static Map<String, List<MojoTiming>> grouped(BuildProfile profile, ProfileView view)
    {
        Map<String, List<MojoTiming>> grouped = new LinkedHashMap<>();

        if (view.grouping().isByModule())
        {
            for (ModuleTiming module : profile.getModules())
            {
                grouped.computeIfAbsent(module.module(), key -> new ArrayList<>());
            }
        }

        for (MojoTiming mojo : profile.getMojos())
        {
            grouped.computeIfAbsent(view.grouping().keyOf(mojo), key -> new ArrayList<>()).add(mojo);
        }

        return grouped;
    }

    private static Map<String, ModuleTiming> spansOf(BuildProfile profile)
    {
        Map<String, ModuleTiming> spans = new LinkedHashMap<>();

        for (ModuleTiming module : profile.getModules())
        {
            spans.put(module.module(), module);
        }

        return spans;
    }

    private static List<MojoTiming> membersOf(List<MojoTiming> mojos, ProfileView view)
    {
        Comparator<MojoTiming> order =
            view
                .sort()
                .memberComparator()
                .thenComparing(MojoTiming::module)
                .thenComparing(MojoTiming::goal)
                .thenComparing(MojoTiming::executionId);

        return mojos.stream().sorted(view.descending() ? order.reversed() : order).toList();
    }

    private static Comparator<ProfileGroup> orderOf(ProfileView view)
    {
        Comparator<ProfileGroup> order =
            view.sort().comparator().thenComparing(ProfileGroup::name);

        return view.descending() ? order.reversed() : order;
    }

    private static long startOf(List<MojoTiming> mojos)
    {
        return mojos.stream().mapToLong(MojoTiming::startMillis).min().orElse(0L);
    }

    private static long workOf(List<MojoTiming> mojos)
    {
        return mojos.stream().mapToLong(MojoTiming::durationMillis).sum();
    }
}
