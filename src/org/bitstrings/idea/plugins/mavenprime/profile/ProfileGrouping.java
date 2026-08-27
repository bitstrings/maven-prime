package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

public enum ProfileGrouping
{
    MODULE("module", MojoTiming::module),
    PLUGIN("plugin", mojo -> StringUtils.substringBefore(mojo.goal(), ":")),
    GOAL("goal", MojoTiming::goal),
    PHASE("phase", MojoTiming::phase),
    WORKER("worker", MojoTiming::worker);

    private final String bundleKey;

    private final transient Function<MojoTiming, String> key;

    ProfileGrouping(String bundleKey, Function<MojoTiming, String> key)
    {
        this.bundleKey = bundleKey;
        this.key = key;
    }

    public String keyOf(MojoTiming mojo)
    {
        return key.apply(mojo);
    }

    public boolean isByModule()
    {
        return this == MODULE;
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.profile.groupBy." + bundleKey);
    }

    public String getColumnTitle()
    {
        return MavenPrimeBundle.message("mavenprime.profile.column.name." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
