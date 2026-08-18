package org.bitstrings.idea.plugins.mavenprime;

import javax.swing.Icon;

import com.intellij.openapi.util.IconLoader;

public final class MavenPrimeIcons
{
    public static final Icon ACTION = load("/icons/mavenPrime.svg");

    public static final Icon DAEMON = load("/icons/daemon.svg");

    private MavenPrimeIcons()
    {
    }

    private static Icon load(String path)
    {
        return IconLoader.getIcon(path, MavenPrimeIcons.class);
    }
}
