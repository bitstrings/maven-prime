package org.bitstrings.idea.plugins.mavenprime;

import org.jetbrains.annotations.NonNls;

import com.intellij.DynamicBundle;

public final class MavenPrimeBundle
    extends DynamicBundle
{
    @NonNls
    private static final String BUNDLE = "messages.MavenPrimeBundle";

    private static final MavenPrimeBundle INSTANCE = new MavenPrimeBundle();

    private MavenPrimeBundle()
    {
        super(MavenPrimeBundle.class, BUNDLE);
    }

    public static String message(String key, Object... params)
    {
        return INSTANCE.getMessage(key, params);
    }
}
