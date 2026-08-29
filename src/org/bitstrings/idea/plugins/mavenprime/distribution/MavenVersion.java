package org.bitstrings.idea.plugins.mavenprime.distribution;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public final class MavenVersion
{
    public static final MavenVersion UNKNOWN = new MavenVersion(StringUtils.EMPTY);

    private static final String SEGMENT_SEPARATOR = ".";

    private final String version;

    private MavenVersion(String version)
    {
        this.version = version;
    }

    public static MavenVersion parse(String text)
    {
        String version = StringUtils.trimToEmpty(text);

        return NumberUtils.isDigits(StringUtils.substringBefore(version, SEGMENT_SEPARATOR))
            ? new MavenVersion(version)
            : UNKNOWN;
    }

    public boolean isKnown()
    {
        return StringUtils.isNotEmpty(version);
    }

    @Override
    public boolean equals(Object other)
    {
        return (other instanceof MavenVersion) && version.equals(((MavenVersion) other).version);
    }

    @Override
    public int hashCode()
    {
        return version.hashCode();
    }

    @Override
    public String toString()
    {
        return version;
    }
}
