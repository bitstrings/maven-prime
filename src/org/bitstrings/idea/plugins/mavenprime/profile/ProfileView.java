package org.bitstrings.idea.plugins.mavenprime.profile;

import org.apache.commons.lang3.StringUtils;

import com.intellij.openapi.util.text.StringUtil;

public record ProfileView(ProfileGrouping grouping, ProfileSort sort, boolean descending, String filter)
{
    public static ProfileView defaults()
    {
        return new ProfileView(
            ProfileGrouping.MODULE,
            ProfileSort.DURATION,
            ProfileSort.DURATION.isDescendingByDefault(),
            StringUtils.EMPTY);
    }

    public ProfileView withGrouping(ProfileGrouping replacement)
    {
        ProfileSort retained = sort.appliesTo(replacement) ? sort : ProfileSort.DURATION;

        return new ProfileView(replacement, retained, descending, filter);
    }

    public ProfileView withSort(ProfileSort replacement)
    {
        return new ProfileView(grouping, replacement, replacement.isDescendingByDefault(), filter);
    }

    public ProfileView withDescending(boolean replacement)
    {
        return new ProfileView(grouping, sort, replacement, filter);
    }

    public ProfileView withFilter(String replacement)
    {
        return new ProfileView(grouping, sort, descending, StringUtils.defaultString(replacement));
    }

    public boolean matches(String text)
    {
        return StringUtils.isBlank(filter) || StringUtil.containsIgnoreCase(text, filter);
    }
}
