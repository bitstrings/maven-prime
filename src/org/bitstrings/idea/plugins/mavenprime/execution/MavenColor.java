package org.bitstrings.idea.plugins.mavenprime.execution;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;

public final class MavenColor
{
    private static final String COLOR_OPTION = "--color";

    private static final String STYLE_COLOR_PREFIX = "-Dstyle.color=";

    private static final String ALWAYS = "always";

    private static final String NEVER = "never";

    private MavenColor()
    {
    }

    public static List<String> optionsFor(MavenInstallation installation, boolean color)
    {
        String mode = color ? ALWAYS : NEVER;

        return installation.isDaemon() ? List.of(COLOR_OPTION, mode) : List.of(STYLE_COLOR_PREFIX + mode);
    }
}
