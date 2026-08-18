package org.bitstrings.idea.plugins.mavenprime.distribution;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public final class MavenVersion
{
    public static final MavenVersion UNKNOWN = new MavenVersion(StringUtils.EMPTY);

    private static final String SEGMENT_SEPARATOR = ".";

    private static final int MAJOR_SEGMENT = 0;

    private static final int MINOR_SEGMENT = 1;

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

    public int getMajor()
    {
        return segment(MAJOR_SEGMENT);
    }

    public int getMinor()
    {
        return segment(MINOR_SEGMENT);
    }

    public boolean isAtLeast(int requiredMajor)
    {
        return isAtLeast(requiredMajor, 0);
    }

    public boolean isAtLeast(int requiredMajor, int requiredMinor)
    {
        return isKnown()
            && ((getMajor() > requiredMajor)
                || ((getMajor() == requiredMajor) && (getMinor() >= requiredMinor)));
    }

    private int segment(int index)
    {
        String[] segments = StringUtils.split(version, SEGMENT_SEPARATOR);

        return ((segments == null) || (index >= segments.length)) ? 0 : leadingNumber(segments[index]);
    }

    private static int leadingNumber(String segment)
    {
        int end = 0;

        while ((end < segment.length()) && Character.isDigit(segment.charAt(end)))
        {
            end++;
        }

        return NumberUtils.toInt(segment.substring(0, end));
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
