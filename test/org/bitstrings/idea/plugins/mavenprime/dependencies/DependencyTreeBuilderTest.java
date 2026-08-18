package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.artifact;
import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.node;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.junit.Test;

public class DependencyTreeBuilderTest
{
    @Test
    public void build_treeView_keepsTheDependencyHierarchy()
    {
        assertEquals(
            List.of("org.example:root", "com.lib:child"),
            coordinatesIn(builderOver(nestedRoots(), DependencyView.DEFAULT).build()));
    }

    @Test
    public void build_flatView_liftsEveryTransitiveOntoOneLevel()
    {
        DefaultMutableTreeNode root =
            builderOver(
                nestedRoots(),
                new DependencyView(
                    DependencyViewMode.FLAT, DependencySortMode.RESOLUTION, DependencyDepth.ALL))
                .build();

        assertEquals(2, root.getChildCount());
    }

    @Test
    public void build_flatViewOfTheSameArtifactReachedTwice_listsItOnce()
    {
        MavenArtifactNode first = root("org.example", "first");
        MavenArtifactNode second = root("org.example", "second");

        first.setDependencies(List.of(child(first, "com.lib", "shared")));
        second.setDependencies(List.of(child(second, "com.lib", "shared")));

        DefaultMutableTreeNode built =
            builderOver(
                List.of(first, second),
                new DependencyView(
                    DependencyViewMode.FLAT, DependencySortMode.RESOLUTION, DependencyDepth.ALL))
                .build();

        assertEquals(3, built.getChildCount());
    }

    @Test
    public void build_groupedByGroupId_ordersTheGroupsByNameRatherThanByEncounter()
    {
        MavenArtifactNode zulu = root("zulu.corp", "one");
        MavenArtifactNode alpha = root("alpha.corp", "two");
        MavenArtifactNode mike = root("mike.corp", "three");

        DefaultMutableTreeNode built =
            builderOver(
                List.of(zulu, alpha, mike),
                new DependencyView(
                    DependencyViewMode.BY_GROUP_ID, DependencySortMode.RESOLUTION, DependencyDepth.ALL))
                .build();

        assertEquals(List.of("alpha.corp", "mike.corp", "zulu.corp"), groupNamesIn(built));
    }

    @Test
    public void build_depthLimitedToDirectDependencies_leavesTransitivesOut()
    {
        DefaultMutableTreeNode built =
            builderOver(
                nestedRoots(),
                new DependencyView(
                    DependencyViewMode.TREE, DependencySortMode.RESOLUTION, DependencyDepth.DIRECT))
                .build();

        assertEquals(List.of("org.example:root"), coordinatesIn(built));
    }

    @Test
    public void build_aSearchMatchingOnlyATransitive_keepsTheAncestorThatLeadsToIt()
    {
        DefaultMutableTreeNode built =
            new DependencyTreeBuilder(
                DependencyAnalysis.ofTree(nestedRoots()),
                new DependencyCriteria("child", DependencyFilterMode.ALL, DependencyScopeFilter.ALL),
                ArtifactSizes.EMPTY,
                DependencyView.DEFAULT)
                .build();

        assertEquals(
            "a matched transitive is unreachable if its parent is filtered away",
            List.of("org.example:root", "com.lib:child"),
            coordinatesIn(built));
    }

    @Test
    public void getMatchedArtifacts_aSearchMatchingOneNode_reportsOnlyThatNode()
    {
        DependencyTreeBuilder builder =
            new DependencyTreeBuilder(
                DependencyAnalysis.ofTree(nestedRoots()),
                new DependencyCriteria("child", DependencyFilterMode.ALL, DependencyScopeFilter.ALL),
                ArtifactSizes.EMPTY,
                DependencyView.DEFAULT);

        builder.build();

        assertEquals(1, builder.getMatchedArtifacts().size());
    }

    @Test
    public void getMatchedArtifacts_noSearchAtAll_reportsNothingAsMatched()
    {
        DependencyTreeBuilder builder = builderOver(nestedRoots(), DependencyView.DEFAULT);

        builder.build();

        assertTrue(builder.getMatchedArtifacts().isEmpty());
    }

    @Test
    public void build_flatViewLimitedToDirect_stillListsTheDirectDependencies()
    {
        DefaultMutableTreeNode built =
            builderOver(
                nestedRoots(),
                new DependencyView(
                    DependencyViewMode.FLAT, DependencySortMode.RESOLUTION, DependencyDepth.DIRECT))
                .build();

        assertEquals(
            "a depth limit narrows the flat list, it never empties it",
            List.of("org.example:root"),
            coordinatesIn(built));
    }

    @Test
    public void build_groupedViewLimitedToDirect_stillListsTheDirectDependencies()
    {
        DefaultMutableTreeNode built =
            builderOver(
                nestedRoots(),
                new DependencyView(
                    DependencyViewMode.BY_GROUP_ID, DependencySortMode.RESOLUTION, DependencyDepth.DIRECT))
                .build();

        assertEquals(List.of("org.example"), groupNamesIn(built));
    }

    @Test
    public void build_flatViewLimitedToTwoLevels_listsTheRootAndItsChild()
    {
        DefaultMutableTreeNode built =
            builderOver(
                nestedRoots(),
                new DependencyView(
                    DependencyViewMode.FLAT, DependencySortMode.RESOLUTION, DependencyDepth.TWO))
                .build();

        assertEquals(List.of("org.example:root", "com.lib:child"), coordinatesIn(built));
    }

    private static DependencyTreeBuilder builderOver(List<MavenArtifactNode> roots, DependencyView view)
    {
        return new DependencyTreeBuilder(
            DependencyAnalysis.ofTree(roots), DependencyCriteria.UNFILTERED, ArtifactSizes.EMPTY, view);
    }

    private static List<MavenArtifactNode> nestedRoots()
    {
        MavenArtifactNode root = root("org.example", "root");

        root.setDependencies(List.of(child(root, "com.lib", "child")));

        return List.of(root);
    }

    private static MavenArtifactNode root(String groupId, String artifactId)
    {
        return node(null, artifact(groupId, artifactId, "1.0"), MavenArtifactState.ADDED, null);
    }

    private static MavenArtifactNode child(MavenArtifactNode parent, String groupId, String artifactId)
    {
        return node(parent, artifact(groupId, artifactId, "2.0"), MavenArtifactState.ADDED, null);
    }

    private static List<String> groupNamesIn(DefaultMutableTreeNode root)
    {
        List<String> names = new ArrayList<>();

        for (int index = 0; index < root.getChildCount(); index++)
        {
            names.add(String.valueOf(((DefaultMutableTreeNode) root.getChildAt(index)).getUserObject()));
        }

        return names;
    }

    private static List<String> coordinatesIn(DefaultMutableTreeNode root)
    {
        List<String> coordinates = new ArrayList<>();

        collect(root, coordinates);

        return coordinates;
    }

    private static void collect(DefaultMutableTreeNode parent, List<String> coordinates)
    {
        for (int index = 0; index < parent.getChildCount(); index++)
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(index);

            if (child.getUserObject() instanceof MavenArtifactNode artifactNode)
            {
                coordinates.add(DependencyAnalysis.key(artifactNode.getArtifact()));
            }

            collect(child, coordinates);
        }
    }
}
