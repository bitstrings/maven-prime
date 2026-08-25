package org.bitstrings.idea.plugins.mavenprime.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;

public final class ContextConfig
{
    public Map<String, Boolean> profiles;

    public Map<String, String> properties;

    public EnvironmentConfig environment;

    public Boolean forceClean;

    public ContextConfig()
    {
    }

    public static ContextConfig of(
        Map<String, Boolean> profiles,
        Map<String, String> properties,
        BuildContextEnvironment environment,
        boolean forceClean)
    {
        ContextConfig config = new ContextConfig();

        config.profiles = profiles.isEmpty() ? null : new LinkedHashMap<>(profiles);
        config.properties = properties.isEmpty() ? null : new LinkedHashMap<>(properties);
        config.environment = environment.isDeclared() ? EnvironmentConfig.of(environment) : null;
        config.forceClean = forceClean ? Boolean.TRUE : null;

        return config;
    }

    public Map<String, Boolean> getProfiles()
    {
        return (profiles == null) ? Map.of() : profiles;
    }

    public Map<String, String> getProperties()
    {
        return (properties == null) ? Map.of() : properties;
    }

    public BuildContextEnvironment getEnvironment()
    {
        return (environment == null) ? new BuildContextEnvironment() : environment.toEnvironment();
    }
}
