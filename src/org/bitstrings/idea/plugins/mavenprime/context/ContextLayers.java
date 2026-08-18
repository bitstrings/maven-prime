package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public final class ContextLayers
{
    private ContextLayers()
    {
    }

    public static BuildContextEnvironment environment(
        BuildContextEnvironment shared, EnvironmentOverride local)
    {
        BuildContextEnvironment resolved = new BuildContextEnvironment();

        resolved.distribution =
            (local.distribution == null) ? shared.rawDistribution() : local.distribution.copy();
        resolved.settingsFile = inherited(local.settingsFile, shared.settingsFile);
        resolved.jreName = inherited(local.jreName, shared.jreName);
        resolved.vmOptions = inherited(local.vmOptions, shared.vmOptions);

        return resolved;
    }

    public static EnvironmentOverride narrowEnvironment(
        BuildContextEnvironment shared, BuildContextEnvironment edited)
    {
        EnvironmentOverride local = new EnvironmentOverride();

        local.distribution = edited.getDistribution().equals(shared.getDistribution())
            ? null
            : edited.getDistribution();
        local.settingsFile = narrowed(edited.settingsFile, shared.settingsFile);
        local.jreName = narrowed(edited.jreName, shared.jreName);
        local.vmOptions = narrowed(edited.vmOptions, shared.vmOptions);

        return local;
    }

    public static List<BuildContextProperty> properties(
        Map<String, String> shared, List<PropertyOverride> local, Collection<String> ignored)
    {
        Map<String, String> team = declaredProperties(shared);
        Map<String, PropertyOverride> overrides = byKey(local);
        Set<String> suppressed = declaredNames(ignored);

        List<BuildContextProperty> resolved = new ArrayList<>();

        for (Map.Entry<String, String> entry : team.entrySet())
        {
            if (!suppressed.contains(entry.getKey()))
            {
                resolved.add(resolve(entry.getKey(), entry.getValue(), overrides.get(entry.getKey())));
            }
        }

        for (PropertyOverride override : local)
        {
            if (override.isDefined() && !team.containsKey(override.key.trim()) && (override.value != null))
            {
                resolved.add(resolve(override.key.trim(), null, override));
            }
        }

        return resolved;
    }

    public static List<PropertyOverride> narrowProperties(
        Map<String, String> shared, List<BuildContextProperty> effective)
    {
        Map<String, String> team = declaredProperties(shared);

        List<PropertyOverride> overrides = new ArrayList<>();

        for (BuildContextProperty property : effective)
        {
            if (!property.isDefined())
            {
                continue;
            }

            String key = property.key.trim();

            PropertyOverride override =
                PropertyOverride.of(
                    key,
                    property.value.equals(team.get(key)) ? null : property.value,
                    property.appliesToImport ? Boolean.TRUE : null);

            if (override.isDefined())
            {
                overrides.add(override);
            }
        }

        return overrides;
    }

    public static Set<String> keysOf(List<BuildContextProperty> properties)
    {
        Set<String> keys = new LinkedHashSet<>();

        for (BuildContextProperty property : properties)
        {
            if (property.isDefined())
            {
                keys.add(property.key.trim());
            }
        }

        return keys;
    }

    public static String valueOf(List<BuildContextProperty> properties, String key)
    {
        String value = null;

        for (BuildContextProperty property : properties)
        {
            if (property.isDefined() && property.key.trim().equals(key))
            {
                value = property.value;
            }
        }

        return value;
    }

    public static Set<String> overriddenKeys(List<PropertyOverride> overrides)
    {
        Set<String> keys = new LinkedHashSet<>();

        for (PropertyOverride override : overrides)
        {
            if (override.isDefined())
            {
                keys.add(override.key.trim());
            }
        }

        return keys;
    }

    public static Map<String, ContextOrigin> originsOf(Collection<String> shared, Set<String> overridden)
    {
        Map<String, ContextOrigin> origins = new LinkedHashMap<>();

        for (String key : shared)
        {
            origins.put(key, overridden.contains(key) ? ContextOrigin.OVERRIDDEN : ContextOrigin.SHARED);
        }

        return origins;
    }

    private static BuildContextProperty resolve(String key, String team, PropertyOverride override)
    {
        if (override == null)
        {
            return BuildContextProperty.of(key, team, false);
        }

        return BuildContextProperty.of(
            key,
            (override.value == null) ? team : override.value,
            (override.appliesToImport != null) && override.appliesToImport.booleanValue());
    }

    public static Map<String, Boolean> profiles(Map<String, Boolean> shared, List<ProfileOverride> local)
    {
        Map<String, Boolean> resolved = new LinkedHashMap<>(declaredProfiles(shared));

        for (ProfileOverride override : local)
        {
            if (!override.isDefined())
            {
                continue;
            }

            if (override.enabled == null)
            {
                resolved.remove(override.name.trim());
            }
            else
            {
                resolved.put(override.name.trim(), override.enabled);
            }
        }

        return resolved;
    }

    public static List<ProfileOverride> absorb(
        List<ProfileOverride> local,
        Map<String, Boolean> pushed,
        Map<String, Boolean> current,
        Collection<String> available)
    {
        Map<String, Boolean> pushedProfiles = declaredProfiles(pushed);
        Map<String, Boolean> currentProfiles = declaredProfiles(current);
        Set<String> declared = declaredNames(available);

        Map<String, Boolean> overrides = asMap(local);

        Set<String> touched = new LinkedHashSet<>(pushedProfiles.keySet());

        touched.addAll(currentProfiles.keySet());

        for (String name : touched)
        {
            if (Objects.equals(pushedProfiles.get(name), currentProfiles.get(name)))
            {
                continue;
            }

            if (currentProfiles.containsKey(name) || declared.contains(name))
            {
                overrides.put(name, currentProfiles.get(name));
            }
        }

        return asOverrides(overrides);
    }

    public static List<ProfileOverride> narrowProfiles(
        Map<String, Boolean> shared, List<ProfileOverride> local)
    {
        Map<String, Boolean> team = declaredProfiles(shared);
        Map<String, Boolean> overrides = asMap(local);

        overrides.entrySet().removeIf(entry -> Objects.equals(entry.getValue(), team.get(entry.getKey())));

        return asOverrides(overrides);
    }

    public static List<ProfileOverride> withOverride(List<ProfileOverride> local, String name, Boolean enabled)
    {
        Map<String, Boolean> opinions = asMap(local);

        opinions.put(StringUtils.trimToEmpty(name), enabled);

        return asOverrides(opinions);
    }

    public static Map<String, Boolean> asMap(List<ProfileOverride> overrides)
    {
        Map<String, Boolean> opinions = new LinkedHashMap<>();

        for (ProfileOverride override : overrides)
        {
            if (override.isDefined())
            {
                opinions.put(override.name.trim(), override.enabled);
            }
        }

        return opinions;
    }

    public static List<ProfileOverride> asOverrides(Map<String, Boolean> opinions)
    {
        List<ProfileOverride> overrides = new ArrayList<>(opinions.size());

        opinions.forEach((name, enabled) -> overrides.add(ProfileOverride.of(name, enabled)));

        return overrides;
    }

    public static boolean forceClean(Boolean shared, Boolean local)
    {
        return (local == null) ? ((shared != null) && shared.booleanValue()) : local.booleanValue();
    }

    public static Boolean narrowForceClean(Boolean shared, boolean effective)
    {
        return (effective == ((shared != null) && shared.booleanValue())) ? null : Boolean.valueOf(effective);
    }

    private static String inherited(String local, String shared)
    {
        return (local == null) ? StringUtils.defaultString(shared) : local;
    }

    private static String narrowed(String edited, String shared)
    {
        return StringUtils.defaultString(edited).equals(StringUtils.defaultString(shared))
            ? null
            : StringUtils.defaultString(edited);
    }

    public static Map<String, String> declaredProperties(Map<String, String> properties)
    {
        Map<String, String> declared = new LinkedHashMap<>();

        properties.forEach(
            (key, value) ->
            {
                if (StringUtils.isNotBlank(key) && (value != null))
                {
                    declared.put(key.trim(), value);
                }
            });

        return declared;
    }

    public static Map<String, Boolean> declaredProfiles(Map<String, Boolean> profiles)
    {
        Map<String, Boolean> declared = new LinkedHashMap<>();

        profiles.forEach(
            (name, enabled) ->
            {
                if (StringUtils.isNotBlank(name) && (enabled != null))
                {
                    declared.put(name.trim(), enabled);
                }
            });

        return declared;
    }

    private static Set<String> declaredNames(Collection<String> names)
    {
        Set<String> declared = new LinkedHashSet<>();

        for (String name : names)
        {
            if (StringUtils.isNotBlank(name))
            {
                declared.add(name.trim());
            }
        }

        return declared;
    }

    private static Map<String, PropertyOverride> byKey(List<PropertyOverride> overrides)
    {
        Map<String, PropertyOverride> byKey = new LinkedHashMap<>();

        for (PropertyOverride override : overrides)
        {
            if (override.isDefined())
            {
                byKey.putIfAbsent(override.key.trim(), override);
            }
        }

        return byKey;
    }

}
