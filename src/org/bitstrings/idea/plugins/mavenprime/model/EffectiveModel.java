package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PluginOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PropertyOrigin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class EffectiveModel
    implements Disposable
{
    private final Project project;

    private final Map<String, Map<String, PropertyOrigin>> properties = new ConcurrentHashMap<>();

    private final Map<String, Map<String, DependencyOrigin>> dependencies = new ConcurrentHashMap<>();

    private final Map<String, Map<String, PluginOrigin>> plugins = new ConcurrentHashMap<>();

    public EffectiveModel(Project project)
    {
        this.project = project;

        project
            .getMessageBus()
            .connect(this)
            .subscribe(
                ProvenanceLog.TOPIC, (ProvenanceLog.ProvenanceListener) this::invalidate);
    }

    public static EffectiveModel getInstance(Project project)
    {
        return project.getService(EffectiveModel.class);
    }

    @Override
    public void dispose()
    {
    }

    public boolean hasDataFor(String module)
    {
        return !StringUtils.isBlank(module)
            && (
                !propertiesOf(module).isEmpty()
                    || !dependenciesOf(module).isEmpty()
                    || !pluginsOf(module).isEmpty()
            );
    }

    public PropertyOrigin propertyOf(String module, String name)
    {
        return StringUtils.isAnyBlank(module, name) ? null : propertiesOf(module).get(name);
    }

    public DependencyOrigin dependencyOf(String module, String managementKey)
    {
        return StringUtils.isAnyBlank(module, managementKey) ? null : dependenciesOf(module).get(managementKey);
    }

    public PluginOrigin pluginOf(String module, String pluginKey)
    {
        return StringUtils.isAnyBlank(module, pluginKey) ? null : pluginsOf(module).get(pluginKey);
    }

    private Map<String, PropertyOrigin> propertiesOf(String module)
    {
        return properties.computeIfAbsent(
            module, key -> indexed(ProvenanceLog.getInstance(project).getProperties(key), PropertyOrigin::name));
    }

    private Map<String, DependencyOrigin> dependenciesOf(String module)
    {
        return dependencies.computeIfAbsent(
            module,
            key -> indexed(ProvenanceLog.getInstance(project).getDependencies(key), DependencyOrigin::name));
    }

    private Map<String, PluginOrigin> pluginsOf(String module)
    {
        return plugins.computeIfAbsent(
            module, key -> indexed(ProvenanceLog.getInstance(project).getPlugins(key), PluginOrigin::name));
    }

    private static <T> Map<String, T> indexed(List<T> origins, Function<T, String> name)
    {
        Map<String, T> indexed = new LinkedHashMap<>();

        for (T origin : origins)
        {
            indexed.put(name.apply(origin), origin);
        }

        return indexed;
    }

    private void invalidate()
    {
        properties.clear();
        dependencies.clear();
        plugins.clear();
    }
}
