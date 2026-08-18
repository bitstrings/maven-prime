package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysis;
import org.bitstrings.idea.plugins.mavenprime.dependencies.ResolutionReason;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

final class PomDependencyNodes
{
    private PomDependencyNodes()
    {
    }

    static MavenArtifactNode rootOf(PomDependency dependency)
    {
        for (MavenArtifactNode root : DependencyAnalysis.of(dependency.module()).getRoots())
        {
            if (isSameArtifact(root.getArtifact(), dependency))
            {
                return root;
            }
        }

        return null;
    }

    static boolean hasTransitives(PomDependency dependency)
    {
        MavenArtifactNode root = rootOf(dependency);

        if (root == null)
        {
            return false;
        }

        for (MavenArtifactNode child : DependencyAnalysis.childrenOf(root))
        {
            if (!ResolutionReason.of(child).isOmitted())
            {
                return true;
            }
        }

        return false;
    }

    static List<MavenArtifactNode> transitivesOf(PomDependency dependency)
    {
        MavenArtifactNode root = rootOf(dependency);

        if (root == null)
        {
            return List.of();
        }

        List<MavenArtifactNode> transitives = new ArrayList<>();

        collectInto(root, transitives);

        return transitives;
    }

    private static void collectInto(MavenArtifactNode node, List<MavenArtifactNode> transitives)
    {
        for (MavenArtifactNode child : DependencyAnalysis.childrenOf(node))
        {
            if (!ResolutionReason.of(child).isOmitted())
            {
                transitives.add(child);

                collectInto(child, transitives);
            }
        }
    }

    private static boolean isSameArtifact(MavenArtifact artifact, PomDependency dependency)
    {
        return (artifact != null)
            && dependency.groupId().equals(artifact.getGroupId())
            && dependency.artifactId().equals(artifact.getArtifactId());
    }
}
