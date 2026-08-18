package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.junit.Test;

public class DependencyAnalysisTest
{
    @Test
    public void pathToRoot_transitiveNode_listsTheRootFirst()
    {
        MavenArtifactNode root = node(null, artifact("org.example", "root", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode child = node(root, artifact("com.lib", "child", "2.0"), MavenArtifactState.ADDED, null);

        List<MavenArtifactNode> path = DependencyAnalysis.pathToRoot(child);

        assertEquals(List.of(root, child), path);
    }

    @Test
    public void key_artifact_joinsGroupIdAndArtifactId()
    {
        assertEquals("com.lib:child", DependencyAnalysis.key(artifact("com.lib", "child", "2.0")));
    }

    @Test
    public void isConflicted_artifactPresentInTwoVersions_isTrue()
    {
        MavenArtifactNode first = node(null, artifact("com.lib", "shared", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode second =
            node(
                null,
                artifact("com.lib", "shared", "2.0"),
                MavenArtifactState.CONFLICT,
                artifact("com.lib", "shared", "1.0"));

        DependencyAnalysis analysis = analysisOf(List.of(first, second));

        assertTrue(analysis.isConflicted(first));
    }

    @Test
    public void isConflicted_artifactPresentInOneVersion_isFalse()
    {
        MavenArtifactNode only = node(null, artifact("com.lib", "shared", "1.0"), MavenArtifactState.ADDED, null);

        assertFalse(analysisOf(List.of(only)).isConflicted(only));
    }

    @Test
    public void occurrencesOf_artifactPresentTwice_returnsBothNodes()
    {
        MavenArtifactNode first = node(null, artifact("com.lib", "shared", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode second =
            node(null, artifact("com.lib", "shared", "2.0"), MavenArtifactState.CONFLICT, null);

        assertEquals(2, analysisOf(List.of(first, second)).occurrencesOf(first).size());
    }

    @Test
    public void getTotalCount_nestedTree_countsEveryNode()
    {
        MavenArtifactNode root = node(null, artifact("org.example", "root", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode child = node(root, artifact("com.lib", "child", "2.0"), MavenArtifactState.ADDED, null);

        root.setDependencies(List.of(child));

        assertEquals(2, analysisOf(List.of(root)).getTotalCount());
    }

    @Test
    public void getConflictedKeys_treeWithoutConflict_isEmpty()
    {
        MavenArtifactNode root = node(null, artifact("org.example", "root", "1.0"), MavenArtifactState.ADDED, null);

        assertTrue(analysisOf(List.of(root)).getConflictedKeys().isEmpty());
    }

    private static DependencyAnalysis analysisOf(List<MavenArtifactNode> roots)
    {
        return DependencyAnalysis.ofTree(roots);
    }

    static MavenArtifact artifact(String groupId, String artifactId, String version)
    {
        return new MavenArtifact(
            groupId, artifactId, version, version, "jar", null, "compile", false, "jar", null, null, false, false);
    }

    static MavenArtifactNode node(
        MavenArtifactNode parent, MavenArtifact artifact, MavenArtifactState state, MavenArtifact related)
    {
        return new MavenArtifactNode(parent, artifact, state, related, "compile", null, null);
    }
}
