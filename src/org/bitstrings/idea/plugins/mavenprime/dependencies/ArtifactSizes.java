package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

public record ArtifactSizes(Map<Path, Long> byPath)
{
    public static final ArtifactSizes EMPTY = new ArtifactSizes(Map.of());

    public long sizeOf(MavenArtifact artifact)
    {
        Path path = ArtifactLocation.pathOf(artifact);

        Long known = (path == null) ? null : byPath.get(path);

        return (known == null) ? 0L : known.longValue();
    }

    public long subtreeSizeOf(MavenArtifactNode node)
    {
        Set<Path> counted = new LinkedHashSet<>();

        collect(node, counted);

        long total = 0L;

        for (Path path : counted)
        {
            Long known = byPath.get(path);

            total += (known == null) ? 0L : known.longValue();
        }

        return total;
    }

    private static void collect(MavenArtifactNode node, Set<Path> counted)
    {
        Path path = ArtifactLocation.pathOf(node.getArtifact());

        if (path != null)
        {
            counted.add(path);
        }

        for (MavenArtifactNode child : DependencyAnalysis.childrenOf(node))
        {
            collect(child, counted);
        }
    }
}
