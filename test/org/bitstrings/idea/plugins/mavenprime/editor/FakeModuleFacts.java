package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.model.MavenExpressions;
import org.bitstrings.idea.plugins.mavenprime.model.ModuleFacts;

final class FakeModuleFacts
    implements ModuleFacts
{
    private final Map<String, String> expressions;

    private final Map<String, String> dependencyVersions;

    private final Map<String, String> pluginVersions;

    FakeModuleFacts(
        Map<String, String> properties,
        Map<String, String> dependencyVersions,
        Map<String, String> pluginVersions)
    {
        this.expressions =
            MavenExpressions.builtIns(
                new MavenExpressions.ProjectFacts(
                    "org.example",
                    "core",
                    "5.2.0-SNAPSHOT",
                    "Core",
                    "jar",
                    "/repo/core",
                    "/repo/core/target",
                    "/repo/core/target/classes",
                    "core-5.2.0-SNAPSHOT",
                    "org.example",
                    "parent",
                    "5.2.0-SNAPSHOT"),
                properties);
        this.dependencyVersions = Map.copyOf(dependencyVersions);
        this.pluginVersions = Map.copyOf(pluginVersions);
    }

    @Override
    public String moduleKey()
    {
        return ProvenanceFixture.MODULE;
    }

    @Override
    public String expression(String name)
    {
        return expressions.get(name);
    }

    @Override
    public String dependencyVersion(String groupId, String artifactId)
    {
        return dependencyVersions.get(groupId + ':' + artifactId);
    }

    @Override
    public String pluginVersion(String groupId, String artifactId)
    {
        for (Map.Entry<String, String> plugin : pluginVersions.entrySet())
        {
            String[] coordinates = plugin.getKey().split(":");

            if (
                coordinates[1].equals(artifactId)
                    && (StringUtils.isBlank(groupId) || coordinates[0].equals(groupId))
            )
            {
                return plugin.getValue();
            }
        }

        return null;
    }
}
