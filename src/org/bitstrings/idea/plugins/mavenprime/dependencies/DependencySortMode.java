package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.Comparator;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

public enum DependencySortMode
{
    RESOLUTION("resolution"),
    ARTIFACT_ID("artifactId"),
    GROUP_ID("groupId"),
    SCOPE("scope"),
    SIZE("size");

    private final String bundleKey;

    DependencySortMode(String bundleKey)
    {
        this.bundleKey = bundleKey;
    }

    public Comparator<MavenArtifactNode> comparator(ArtifactSizes sizes)
    {
        return switch (this)
        {
            case RESOLUTION -> null;
            case ARTIFACT_ID -> byArtifactId();
            case GROUP_ID -> byGroupId();
            case SCOPE -> byScope();
            case SIZE -> bySize(sizes);
        };
    }

    private static Comparator<MavenArtifactNode> byArtifactId()
    {
        return Comparator
            .<MavenArtifactNode, String>comparing(
                node -> node.getArtifact().getArtifactId(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(node -> node.getArtifact().getGroupId(), String.CASE_INSENSITIVE_ORDER);
    }

    private static Comparator<MavenArtifactNode> byGroupId()
    {
        return Comparator
            .<MavenArtifactNode, String>comparing(
                node -> node.getArtifact().getGroupId(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(node -> node.getArtifact().getArtifactId(), String.CASE_INSENSITIVE_ORDER);
    }

    private static Comparator<MavenArtifactNode> byScope()
    {
        return Comparator
            .<MavenArtifactNode, String>comparing(
                node -> DependencyScopeFilter.effectiveScope(node.getArtifact()), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(node -> node.getArtifact().getArtifactId(), String.CASE_INSENSITIVE_ORDER);
    }

    private static Comparator<MavenArtifactNode> bySize(ArtifactSizes sizes)
    {
        return Comparator
            .comparingLong((MavenArtifactNode node) -> sizes.sizeOf(node.getArtifact()))
            .reversed()
            .thenComparing(node -> node.getArtifact().getArtifactId(), String.CASE_INSENSITIVE_ORDER);
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.dependencies.sort." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
