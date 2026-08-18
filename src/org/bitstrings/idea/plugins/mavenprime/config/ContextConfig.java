package org.bitstrings.idea.plugins.mavenprime.config;

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
