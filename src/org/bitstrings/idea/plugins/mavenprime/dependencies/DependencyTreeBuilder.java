package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.idea.maven.model.MavenArtifactNode;

public final class DependencyTreeBuilder
{
    private final DependencyAnalysis analysis;

    private final DependencyCriteria criteria;

    private final ArtifactSizes sizes;

    private final DependencyView view;

    private final Set<String> matched = new LinkedHashSet<>();

    public DependencyTreeBuilder(
        DependencyAnalysis analysis, DependencyCriteria criteria, ArtifactSizes sizes, DependencyView view)
    {
        this.analysis = analysis;
        this.criteria = criteria;
        this.sizes = sizes;
        this.view = view;
    }

    public DefaultMutableTreeNode build()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        switch (view.mode())
        {
            case TREE -> addTreeNodes(root);
            case FLAT -> addFlatNodes(root);
            case BY_SCOPE ->
                addGroupedNodes(root, node -> DependencyScopeFilter.effectiveScope(node.getArtifact()));
            case BY_GROUP_ID -> addGroupedNodes(root, node -> node.getArtifact().getGroupId());
        }

        return root;
    }

    public Set<String> getMatchedArtifacts()
    {
        return Set.copyOf(matched);
    }

    public long totalMatchedSize()
    {
        long total = 0L;

        for (MavenArtifactNode node : distinctNodes())
        {
            if (matches(node))
            {
                total += sizes.sizeOf(node.getArtifact());
            }
        }

        return total;
    }

    private void addTreeNodes(DefaultMutableTreeNode root)
    {
        for (MavenArtifactNode node : sorted(analysis.getRoots()))
        {
            addNode(root, node, 0);
        }
    }

    private boolean addNode(DefaultMutableTreeNode parent, MavenArtifactNode node, int level)
    {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);

        boolean anyChildMatches = false;

        if (view.depth().allows(level + 1))
        {
            for (MavenArtifactNode child : sorted(DependencyAnalysis.childrenOf(node)))
            {
                anyChildMatches |= addNode(treeNode, child, level + 1);
            }
        }

        if (!matches(node) && !anyChildMatches)
        {
            return false;
        }

        parent.add(treeNode);

        countMatch(node);

        return true;
    }

    private void addFlatNodes(DefaultMutableTreeNode root)
    {
        for (MavenArtifactNode node : sorted(distinctNodes()))
        {
            if (matches(node))
            {
                root.add(new DefaultMutableTreeNode(node));

                countMatch(node);
            }
        }
    }

    private void addGroupedNodes(DefaultMutableTreeNode root, Function<MavenArtifactNode, String> grouping)
    {
        Map<String, List<MavenArtifactNode>> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (MavenArtifactNode node : distinctNodes())
        {
            if (matches(node))
            {
                groups.computeIfAbsent(grouping.apply(node), group -> new ArrayList<>()).add(node);
            }
        }

        for (Map.Entry<String, List<MavenArtifactNode>> group : groups.entrySet())
        {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group.getKey());

            for (MavenArtifactNode node : sorted(group.getValue()))
            {
                groupNode.add(new DefaultMutableTreeNode(node));

                countMatch(node);
            }

            root.add(groupNode);
        }
    }

    private List<MavenArtifactNode> distinctNodes()
    {
        Map<String, MavenArtifactNode> distinct = new LinkedHashMap<>();

        collectDistinct(analysis.getRoots(), distinct, 0);

        return new ArrayList<>(distinct.values());
    }

    private void collectDistinct(
        List<MavenArtifactNode> nodes, Map<String, MavenArtifactNode> distinct, int level)
    {
        if (!view.depth().allows(level))
        {
            return;
        }

        for (MavenArtifactNode node : nodes)
        {
            distinct.putIfAbsent(node.getArtifact().getDisplayStringFull(), node);

            collectDistinct(DependencyAnalysis.childrenOf(node), distinct, level + 1);
        }
    }

    private void countMatch(MavenArtifactNode node)
    {
        if (criteria.hasSearch() && criteria.matchesSearch(node))
        {
            matched.add(node.getArtifact().getDisplayStringFull());
        }
    }

    private List<MavenArtifactNode> sorted(List<MavenArtifactNode> nodes)
    {
        Comparator<MavenArtifactNode> comparator = view.sort().comparator(sizes);

        if (comparator == null)
        {
            return nodes;
        }

        List<MavenArtifactNode> ordered = new ArrayList<>(nodes);

        ordered.sort(comparator);

        return ordered;
    }

    private boolean matches(MavenArtifactNode node)
    {
        return criteria.accepts(node, analysis);
    }
}
