package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.ArrayList;
import java.util.List;

public final class BuildContextProperties
{
    public EnvironmentOverride environment = new EnvironmentOverride();

    public List<PropertyOverride> properties = new ArrayList<>();

    public List<String> ignoredProperties = new ArrayList<>();

    public List<ProfileOverride> profiles = new ArrayList<>();

    public List<ProfileOverride> pushedProfiles = new ArrayList<>();

    public Boolean forceClean;

    public BuildContextProperties()
    {
    }

    public BuildContextProperties copy()
    {
        BuildContextProperties copy = new BuildContextProperties();

        copy.environment = environment.copy();
        copy.forceClean = forceClean;
        copy.ignoredProperties = new ArrayList<>(ignoredProperties);

        for (PropertyOverride override : properties)
        {
            copy.properties.add(override.copy());
        }

        for (ProfileOverride override : profiles)
        {
            copy.profiles.add(override.copy());
        }

        for (ProfileOverride override : pushedProfiles)
        {
            copy.pushedProfiles.add(override.copy());
        }

        return copy;
    }
}
