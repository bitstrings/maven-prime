package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.Comparator;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

public enum ProfileSort
{
    DURATION("duration", true),
    STARTED("started", false),
    NAME("name", false),
    PAYOFF("payoff", true);

    private final String bundleKey;

    private final boolean descendingByDefault;

    ProfileSort(String bundleKey, boolean descendingByDefault)
    {
        this.bundleKey = bundleKey;
        this.descendingByDefault = descendingByDefault;
    }

    public boolean isDescendingByDefault()
    {
        return descendingByDefault;
    }

    public boolean appliesTo(ProfileGrouping grouping)
    {
        return (this != PAYOFF) || grouping.isByModule();
    }

    public Comparator<ProfileGroup> comparator()
    {
        return switch (this)
        {
            case DURATION -> Comparator.comparingLong(ProfileGroup::durationMillis);
            case STARTED -> Comparator.comparingLong(ProfileGroup::startMillis);
            case NAME -> Comparator.comparing(ProfileGroup::name, String.CASE_INSENSITIVE_ORDER);
            case PAYOFF -> Comparator.comparingLong(ProfileGroup::payoffMillis);
        };
    }

    public Comparator<MojoTiming> memberComparator()
    {
        return switch (this)
        {
            case STARTED -> Comparator.comparingLong(MojoTiming::startMillis);
            case NAME -> Comparator.comparing(MojoTiming::goal, String.CASE_INSENSITIVE_ORDER);
            case DURATION, PAYOFF -> Comparator.comparingLong(MojoTiming::durationMillis);
        };
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.profile.sortBy." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
