package org.bitstrings.idea.plugins.mavenprime.goals;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class GoalIds
{
    static final String FALLBACK = "goal";

    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");

    private GoalIds()
    {
    }

    public static String slug(String text)
    {
        String slug =
            SEPARATORS.matcher(StringUtils.trimToEmpty(text).toLowerCase(Locale.ROOT)).replaceAll("-");

        return StringUtils.defaultIfEmpty(StringUtils.strip(slug, "-"), FALLBACK);
    }

    public static String uniqueIn(String base, Collection<String> taken)
    {
        if (!taken.contains(base))
        {
            return base;
        }

        for (int ordinal = 2; ordinal < Integer.MAX_VALUE; ordinal++)
        {
            String candidate = base + '-' + ordinal;

            if (!taken.contains(candidate))
            {
                return candidate;
            }
        }

        throw new IllegalStateException("No free identifier remains for: " + base);
    }
}
