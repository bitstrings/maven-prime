package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.junit.Test;

public class ArtifactSizesTest
{
    private static final Path ROOT_PATH = Path.of("/repo/root.jar");

    private static final Path CHILD_PATH = Path.of("/repo/child.jar");

    private static final Path GRANDCHILD_PATH = Path.of("/repo/grandchild.jar");

    private static final ArtifactSizes SIZES =
        new ArtifactSizes(
            Map.of(ROOT_PATH, Long.valueOf(100L), CHILD_PATH, Long.valueOf(20L),
                GRANDCHILD_PATH, Long.valueOf(3L)));

    @Test
    public void subtreeSizeOf_leaf_isItsOwnSize()
    {
        assertEquals(100L, SIZES.subtreeSizeOf(node(ROOT_PATH)));
    }

    @Test
    public void subtreeSizeOf_nodeWithDescendants_addsEveryDistinctArtifact()
    {
        MavenArtifactNode root = node(ROOT_PATH);
        MavenArtifactNode child = node(CHILD_PATH);

        child.setDependencies(List.of(node(GRANDCHILD_PATH)));
        root.setDependencies(List.of(child));

        assertEquals(123L, SIZES.subtreeSizeOf(root));
    }

    @Test
    public void subtreeSizeOf_sameArtifactTwiceInSubtree_countsItOnce()
    {
        MavenArtifactNode root = node(ROOT_PATH);

        root.setDependencies(List.of(node(CHILD_PATH), node(CHILD_PATH)));

        assertEquals(120L, SIZES.subtreeSizeOf(root));
    }

    @Test
    public void subtreeSizeOf_descendantMissingFromTheSnapshot_contributesNothing()
    {
        MavenArtifactNode root = node(ROOT_PATH);

        root.setDependencies(List.of(node(Path.of("/repo/unmeasured.jar"))));

        assertEquals(100L, SIZES.subtreeSizeOf(root));
    }

    @Test
    public void subtreeSizeOf_artifactWithoutFile_contributesNothing()
    {
        assertEquals(0L, SIZES.subtreeSizeOf(node(null)));
    }

    private static MavenArtifactNode node(Path path)
    {
        return new MavenArtifactNode(
            null, artifact(path), MavenArtifactState.ADDED, null, "compile", null, null);
    }

    private static MavenArtifact artifact(Path path)
    {
        return new MavenArtifact(
            "com.lib",
            (path == null) ? "nofile" : path.getFileName().toString(),
            "1.0",
            "1.0",
            "jar",
            null,
            "compile",
            false,
            "jar",
            (path == null) ? null : new File(path.toString()),
            null,
            false,
            false);
    }
}
