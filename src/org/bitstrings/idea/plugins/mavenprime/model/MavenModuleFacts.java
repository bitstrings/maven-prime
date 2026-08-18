package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.project.MavenProject;

public final class MavenModuleFacts
    implements ModuleFacts
{
    private final MavenProject module;

    private final Map<String, String> expressions;

    public MavenModuleFacts(MavenProject module)
    {
        this.module = module;
        this.expressions = MavenExpressions.builtIns(factsOf(module), propertiesOf(module));
    }

    @Override
    public String moduleKey()
    {
        return MavenProjects.moduleKeyOf(module);
    }

    @Override
    public String expression(String name)
    {
        return expressions.get(name);
    }

    @Override
    public String dependencyVersion(String groupId, String artifactId)
    {
        List<MavenArtifact> found = module.findDependencies(groupId, artifactId);

        return found.isEmpty() ? null : found.get(0).getVersion();
    }

    @Override
    public String pluginVersion(String groupId, String artifactId)
    {
        for (MavenPlugin plugin : module.getPlugins())
        {
            if (
                artifactId.equals(plugin.getArtifactId())
                    && (StringUtils.isBlank(groupId) || groupId.equals(plugin.getGroupId()))
            )
            {
                return plugin.getVersion();
            }
        }

        return null;
    }

    private static MavenExpressions.ProjectFacts factsOf(MavenProject module)
    {
        MavenId id = module.getMavenId();
        MavenId parent = module.getParentId();

        return new MavenExpressions.ProjectFacts(
            id.getGroupId(),
            id.getArtifactId(),
            id.getVersion(),
            module.getName(),
            module.getPackaging(),
            module.getDirectory(),
            module.getBuildDirectory(),
            module.getOutputDirectory(),
            module.getFinalName(),
            (parent == null) ? null : parent.getGroupId(),
            (parent == null) ? null : parent.getArtifactId(),
            (parent == null) ? null : parent.getVersion());
    }

    private static Map<String, String> propertiesOf(MavenProject module)
    {
        Properties declared = module.getProperties();

        Map<String, String> properties = new LinkedHashMap<>();

        for (String name : declared.stringPropertyNames())
        {
            properties.put(name, StringUtils.defaultString(declared.getProperty(name)));
        }

        return properties;
    }
}
