package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.idea.maven.model.MavenArtifactNode;

public final class ArtifactSizeCache
{
    private volatile ArtifactSizes current = ArtifactSizes.EMPTY;

    public ArtifactSizes snapshot()
    {
        return current;
    }

    public void prime(List<MavenArtifactNode> nodes)
    {
        Map<Path, Long> measured = new HashMap<>();

        measure(nodes, measured);

        current = new ArtifactSizes(Map.copyOf(measured));
    }

    private static void measure(List<MavenArtifactNode> nodes, Map<Path, Long> measured)
    {
        for (MavenArtifactNode node : nodes)
        {
            Path path = ArtifactLocation.pathOf(node.getArtifact());

            if (path != null)
            {
                measured.computeIfAbsent(path, ArtifactSizeCache::sizeOnDisk);
            }

            measure(DependencyAnalysis.childrenOf(node), measured);
        }
    }

    private static Long sizeOnDisk(Path path)
    {
        File file = path.toFile();

        return Long.valueOf(file.isFile() ? file.length() : 0L);
    }
}
