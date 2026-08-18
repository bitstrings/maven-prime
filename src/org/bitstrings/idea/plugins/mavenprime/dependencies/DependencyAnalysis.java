package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;

public final class DependencyAnalysis
{
    private final List<MavenArtifactNode> roots;

    private final Map<String, List<MavenArtifactNode>> occurrences;

    private final Set<String> conflictedKeys;

    private DependencyAnalysis(
        List<MavenArtifactNode> roots,
        Map<String, List<MavenArtifactNode>> occurrences,
        Set<String> conflictedKeys)
    {
        this.roots = roots;
        this.occurrences = occurrences;
        this.conflictedKeys = conflictedKeys;
    }

    public static DependencyAnalysis of(MavenProject mavenProject)
    {
        return ofTree((mavenProject == null) ? List.of() : mavenProject.getDependencyTree());
    }

    public static DependencyAnalysis ofTree(List<MavenArtifactNode> dependencyTree)
    {
        List<MavenArtifactNode> roots = List.copyOf(dependencyTree);

        Map<String, List<MavenArtifactNode>> occurrences = new LinkedHashMap<>();

        collect(roots, occurrences);

        return new DependencyAnalysis(roots, occurrences, conflictedKeys(occurrences));
    }

    private static void collect(List<MavenArtifactNode> nodes, Map<String, List<MavenArtifactNode>> occurrences)
    {
        for (MavenArtifactNode node : nodes)
        {
            occurrences.computeIfAbsent(key(node.getArtifact()), key -> new ArrayList<>()).add(node);

            collect(childrenOf(node), occurrences);
        }
    }

    private static Set<String> conflictedKeys(Map<String, List<MavenArtifactNode>> occurrences)
    {
        Set<String> conflicted = new LinkedHashSet<>();

        for (Map.Entry<String, List<MavenArtifactNode>> occurrence : occurrences.entrySet())
        {
            Set<String> versions = new LinkedHashSet<>();

            for (MavenArtifactNode node : occurrence.getValue())
            {
                versions.add(node.getArtifact().getVersion());
            }

            if (versions.size() > 1)
            {
                conflicted.add(occurrence.getKey());
            }
        }

        return conflicted;
    }

    public static List<MavenArtifactNode> childrenOf(MavenArtifactNode node)
    {
        List<MavenArtifactNode> children = node.getDependencies();

        return (children == null) ? List.of() : children;
    }

    public static String key(MavenArtifact artifact)
    {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

    public List<MavenArtifactNode> getRoots()
    {
        return roots;
    }

    public Set<String> getConflictedKeys()
    {
        return conflictedKeys;
    }

    public boolean isConflicted(MavenArtifactNode node)
    {
        return conflictedKeys.contains(key(node.getArtifact()));
    }

    public List<MavenArtifactNode> occurrencesOf(MavenArtifactNode node)
    {
        return occurrences.getOrDefault(key(node.getArtifact()), List.of());
    }

    public int getTotalCount()
    {
        int total = 0;

        for (List<MavenArtifactNode> nodes : occurrences.values())
        {
            total += nodes.size();
        }

        return total;
    }

    public static List<MavenArtifactNode> pathToRoot(MavenArtifactNode node)
    {
        List<MavenArtifactNode> path = new ArrayList<>();

        for (MavenArtifactNode current = node; current != null; current = current.getParent())
        {
            path.add(current);
        }

        Collections.reverse(path);

        return path;
    }
}
