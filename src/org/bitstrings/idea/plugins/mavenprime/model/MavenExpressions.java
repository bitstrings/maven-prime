package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public final class MavenExpressions
{
    private static final String PROJECT_PREFIX = "project.";

    private static final String POM_PREFIX = "pom.";

    private MavenExpressions()
    {
    }

    public static Map<String, String> builtIns(ProjectFacts facts, Map<String, String> properties)
    {
        Map<String, String> expressions = new LinkedHashMap<>();

        put(expressions, "groupId", facts.groupId());
        put(expressions, "artifactId", facts.artifactId());
        put(expressions, "version", facts.version());
        put(expressions, "name", facts.name());
        put(expressions, "packaging", facts.packaging());
        put(expressions, "basedir", facts.baseDirectory());
        put(expressions, "build.directory", facts.buildDirectory());
        put(expressions, "build.outputDirectory", facts.outputDirectory());
        put(expressions, "build.finalName", facts.finalName());
        put(expressions, "parent.groupId", facts.parentGroupId());
        put(expressions, "parent.artifactId", facts.parentArtifactId());
        put(expressions, "parent.version", facts.parentVersion());

        for (Map.Entry<String, String> property : properties.entrySet())
        {
            expressions.putIfAbsent(property.getKey(), property.getValue());
        }

        return expressions;
    }

    private static void put(Map<String, String> expressions, String name, String value)
    {
        if (StringUtils.isBlank(value))
        {
            return;
        }

        expressions.put(PROJECT_PREFIX + name, value);
        expressions.put(POM_PREFIX + name, value);
        expressions.put(name, value);
    }

    public record ProjectFacts(
        String groupId,
        String artifactId,
        String version,
        String name,
        String packaging,
        String baseDirectory,
        String buildDirectory,
        String outputDirectory,
        String finalName,
        String parentGroupId,
        String parentArtifactId,
        String parentVersion)
    {
    }
}
