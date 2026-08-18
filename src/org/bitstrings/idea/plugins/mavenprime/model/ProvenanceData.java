package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PluginOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PropertyOrigin;
import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;

public final class ProvenanceData
{
    private final Object lock = new Object();

    private final Map<String, Map<String, DependencyOrigin>> dependencies = new LinkedHashMap<>();

    private final Map<String, Map<String, PluginOrigin>> plugins = new LinkedHashMap<>();

    private final Map<String, Map<String, PropertyOrigin>> properties = new LinkedHashMap<>();

    private final BuildCompleteness completeness = new BuildCompleteness();

    public BuildCompleteness getCompleteness()
    {
        return completeness;
    }

    public void accept(ProvenanceEvent event)
    {
        synchronized (lock)
        {
            switch (event)
            {
                case DependencyOrigin dependency -> index(dependencies, dependency);
                case PluginOrigin plugin -> index(plugins, plugin);
                case PropertyOrigin property -> index(properties, property);
            }
        }
    }

    public List<DependencyOrigin> getDependencies(String module)
    {
        synchronized (lock)
        {
            return entriesOf(dependencies, module);
        }
    }

    public List<PluginOrigin> getPlugins(String module)
    {
        synchronized (lock)
        {
            return entriesOf(plugins, module);
        }
    }

    public List<PropertyOrigin> getProperties(String module)
    {
        synchronized (lock)
        {
            return entriesOf(properties, module);
        }
    }

    public List<String> getModules()
    {
        synchronized (lock)
        {
            Set<String> modules = new LinkedHashSet<>(dependencies.keySet());

            modules.addAll(plugins.keySet());
            modules.addAll(properties.keySet());

            return List.copyOf(modules);
        }
    }

    private static <T extends ProvenanceEvent> void index(Map<String, Map<String, T>> target, T event)
    {
        target.computeIfAbsent(event.module(), module -> new LinkedHashMap<>()).put(event.name(), event);
    }

    private static <T> List<T> entriesOf(Map<String, Map<String, T>> source, String module)
    {
        return List.copyOf(source.getOrDefault(module, Map.of()).values());
    }
}
