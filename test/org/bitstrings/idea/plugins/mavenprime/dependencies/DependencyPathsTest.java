package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.artifact;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyPaths.PathHead;
import org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyPaths.ResolutionVerdict;
import org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyPaths.Rule;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.junit.Test;

public class DependencyPathsTest
{
    @Test
    public void ancestorsOf_directDependency_hasOnlyTheRoot()
    {
        MavenArtifactNode root = node(null, artifact("org.example", "app", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode direct = node(root, artifact("com.lib", "shared", "2.0"), MavenArtifactState.ADDED, null);

        root.setDependencies(List.of(direct));

        assertEquals(List.of(root), DependencyPaths.ancestorsOf(direct));
    }

    @Test
    public void depthOf_transitiveDependency_countsTheEdgesToTheRoot()
    {
        MavenArtifactNode root = node(null, artifact("org.example", "app", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode middle = node(root, artifact("com.mid", "mid", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode deep = node(middle, artifact("com.lib", "shared", "2.0"), MavenArtifactState.ADDED, null);

        middle.setDependencies(List.of(deep));
        root.setDependencies(List.of(middle));

        assertEquals(2, DependencyPaths.depthOf(deep));
    }

    @Test
    public void verdictFor_oneVersionOnly_reportsNoConflict()
    {
        assertEquals(Rule.SINGLE, verdictOf(singleVersionTree()).rule());
    }

    @Test
    public void verdictFor_twoVersions_reportsNearestWins()
    {
        assertEquals(Rule.NEAREST_WINS, verdictOf(conflictedTree()).rule());
    }

    @Test
    public void verdictFor_twoVersions_selectsTheVersionThatWasNotOmitted()
    {
        assertEquals("2.0", verdictOf(conflictedTree()).selectedVersion());
    }

    @Test
    public void verdictFor_twoVersions_countsEveryPath()
    {
        assertEquals(2, verdictOf(conflictedTree()).pathCount());
    }

    @Test
    public void verdictFor_premanagedWinner_reportsDependencyManagement()
    {
        MavenArtifactNode root = node(null, artifact("org.example", "app", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode managed =
            new MavenArtifactNode(
                root, artifact("com.lib", "shared", "2.0"), MavenArtifactState.ADDED, null, "compile", "1.0", null);

        root.setDependencies(List.of(managed));

        ResolutionVerdict verdict = DependencyPaths.verdictFor(DependencyAnalysis.ofTree(List.of(root)), managed);

        assertEquals(Rule.MANAGED, verdict.rule());
        assertEquals("1.0", verdict.declaredVersion());
    }

    @Test
    public void verdictFor_artifactAbsentFromTheTree_isNull()
    {
        MavenArtifactNode orphan =
            node(null, artifact("com.lib", "orphan", "1.0"), MavenArtifactState.ADDED, null);

        assertNull(DependencyPaths.verdictFor(DependencyAnalysis.ofTree(List.of()), orphan));
    }

    @Test
    public void headsFor_theOmittedOccurrenceComesFirstInTheTree_stillMarksTheResolvedOneAsWinner()
    {
        MavenArtifactNode root = conflictedTree();

        List<PathHead> heads = DependencyPaths.headsFor(DependencyAnalysis.ofTree(List.of(root)), selectedIn(root));

        PathHead winner = heads.stream().filter(PathHead::winner).findFirst().orElseThrow();

        assertEquals("2.0", winner.occurrence().getArtifact().getVersion());
    }

    @Test
    public void verdictFor_theOmittedOccurrenceComesFirstInTheTree_stillSelectsTheResolvedVersion()
    {
        assertEquals("2.0", verdictOf(conflictedTree()).selectedVersion());
    }

    @Test
    public void headsFor_twoOccurrences_marksExactlyOneWinner()
    {
        MavenArtifactNode root = conflictedTree();

        List<PathHead> heads = DependencyPaths.headsFor(DependencyAnalysis.ofTree(List.of(root)), selectedIn(root));

        assertEquals(1, heads.stream().filter(PathHead::winner).count());
    }

    @Test
    public void headsFor_twoOccurrences_ordersTheShallowestPathFirst()
    {
        MavenArtifactNode root = conflictedTree();

        List<PathHead> heads = DependencyPaths.headsFor(DependencyAnalysis.ofTree(List.of(root)), selectedIn(root));

        assertEquals(1, heads.get(0).depth());
    }

    @Test
    public void headsFor_omittedOccurrence_isNotTheWinner()
    {
        MavenArtifactNode root = conflictedTree();

        List<PathHead> heads = DependencyPaths.headsFor(DependencyAnalysis.ofTree(List.of(root)), selectedIn(root));

        assertFalse(
            heads
                .stream()
                .filter(head -> ResolutionReason.of(head.occurrence()).isOmitted())
                .anyMatch(PathHead::winner));
    }

    private static ResolutionVerdict verdictOf(MavenArtifactNode root)
    {
        return DependencyPaths.verdictFor(DependencyAnalysis.ofTree(List.of(root)), selectedIn(root));
    }

    private static MavenArtifactNode selectedIn(MavenArtifactNode root)
    {
        for (MavenArtifactNode child : root.getDependencies())
        {
            if ("shared".equals(child.getArtifact().getArtifactId()))
            {
                return child;
            }
        }

        return root.getDependencies().get(0).getDependencies().get(0);
    }

    private static MavenArtifactNode singleVersionTree()
    {
        MavenArtifactNode root = node(null, artifact("org.example", "app", "1.0"), MavenArtifactState.ADDED, null);

        root.setDependencies(
            List.of(node(root, artifact("com.lib", "shared", "2.0"), MavenArtifactState.ADDED, null)));

        return root;
    }

    private static MavenArtifactNode conflictedTree()
    {
        MavenArtifactNode root = node(null, artifact("org.example", "app", "1.0"), MavenArtifactState.ADDED, null);

        MavenArtifactNode winner = node(root, artifact("com.lib", "shared", "2.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode carrier = node(root, artifact("com.mid", "mid", "1.0"), MavenArtifactState.ADDED, null);
        MavenArtifactNode loser =
            node(
                carrier,
                artifact("com.lib", "shared", "1.0"),
                MavenArtifactState.CONFLICT,
                artifact("com.lib", "shared", "2.0"));

        carrier.setDependencies(List.of(loser));
        root.setDependencies(List.of(carrier, winner));

        return root;
    }

    private static MavenArtifactNode node(
        MavenArtifactNode parent, MavenArtifact artifact, MavenArtifactState state, MavenArtifact related)
    {
        return new MavenArtifactNode(parent, artifact, state, related, "compile", null, null);
    }
}
