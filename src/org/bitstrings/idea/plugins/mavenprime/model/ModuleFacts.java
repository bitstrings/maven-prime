package org.bitstrings.idea.plugins.mavenprime.model;

public interface ModuleFacts
{
    String moduleKey();

    String expression(String name);

    String dependencyVersion(String groupId, String artifactId);

    String pluginVersion(String groupId, String artifactId);
}
