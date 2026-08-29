package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.jetbrains.idea.maven.dom.model.MavenDomBuildBase;
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin;
import org.jetbrains.idea.maven.dom.model.MavenDomProfile;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

public final class PomPlugins
{
    private PomPlugins()
    {
    }

    public static MavenDomPlugin findDeclaration(
        MavenDomProjectModel model, String groupId, String artifactId)
    {
        MavenDomPlugin declared = findIn(model, groupId, artifactId);

        if (declared != null)
        {
            return declared;
        }

        return ParentPomSearch.firstInAncestry(model, parent -> findIn(parent, groupId, artifactId));
    }

    private static MavenDomPlugin findIn(MavenDomProjectModel model, String groupId, String artifactId)
    {
        MavenDomPlugin declared = findIn(model.getBuild(), model, groupId, artifactId);

        if (declared != null)
        {
            return declared;
        }

        for (MavenDomProfile profile : model.getProfiles().getProfiles())
        {
            MavenDomPlugin inProfile = findIn(profile.getBuild(), model, groupId, artifactId);

            if (inProfile != null)
            {
                return inProfile;
            }
        }

        return null;
    }

    private static MavenDomPlugin findIn(
        MavenDomBuildBase build, MavenDomProjectModel model, String groupId, String artifactId)
    {
        MavenDomPlugin declared = firstMatch(build.getPlugins().getPlugins(), model, groupId, artifactId);

        return (declared != null)
            ? declared
            : firstMatch(build.getPluginManagement().getPlugins().getPlugins(), model, groupId, artifactId);
    }

    private static MavenDomPlugin firstMatch(
        List<MavenDomPlugin> plugins, MavenDomProjectModel model, String groupId, String artifactId)
    {
        for (MavenDomPlugin plugin : plugins)
        {
            if (matches(plugin, model, groupId, artifactId))
            {
                return plugin;
            }
        }

        return null;
    }

    private static boolean matches(
        MavenDomPlugin plugin, MavenDomProjectModel model, String groupId, String artifactId)
    {
        return artifactId.equals(PomDependencies.resolve(model, plugin.getArtifactId().getStringValue()))
            && groupId.equals(groupIdOf(plugin, model));
    }

    private static String groupIdOf(MavenDomPlugin plugin, MavenDomProjectModel model)
    {
        return StringUtils.defaultIfBlank(
            PomDependencies.resolve(model, plugin.getGroupId().getStringValue()),
            ArtifactCoordinates.DEFAULT_PLUGIN_GROUP_ID);
    }
}
