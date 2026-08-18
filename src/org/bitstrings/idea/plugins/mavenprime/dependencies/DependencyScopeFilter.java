package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenConstants;

public enum DependencyScopeFilter
{
    ALL("all", null),
    COMPILE("compile", MavenConstants.SCOPE_COMPILE),
    PROVIDED("provided", MavenConstants.SCOPE_PROVIDED),
    RUNTIME("runtime", MavenConstants.SCOPE_RUNTIME),
    TEST("test", MavenConstants.SCOPE_TEST),
    SYSTEM("system", MavenConstants.SCOPE_SYSTEM);

    private final String bundleKey;

    private final String scope;

    DependencyScopeFilter(String bundleKey, String scope)
    {
        this.bundleKey = bundleKey;
        this.scope = scope;
    }

    public boolean accepts(MavenArtifact artifact)
    {
        return (scope == null) || scope.equalsIgnoreCase(effectiveScope(artifact));
    }

    public static String effectiveScope(MavenArtifact artifact)
    {
        return StringUtils.defaultIfBlank(artifact.getScope(), MavenConstants.SCOPE_COMPILE);
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.dependencies.scope." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
