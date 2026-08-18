package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

public final class DependencyPaths
{
    private DependencyPaths()
    {
    }

    public static List<MavenArtifactNode> ancestorsOf(MavenArtifactNode node)
    {
        List<MavenArtifactNode> ancestors = new ArrayList<>();

        for (MavenArtifactNode parent = node.getParent(); parent != null; parent = parent.getParent())
        {
            ancestors.add(parent);
        }

        return ancestors;
    }

    public static int depthOf(MavenArtifactNode node)
    {
        return ancestorsOf(node).size();
    }

    public static List<PathHead> headsFor(DependencyAnalysis analysis, MavenArtifactNode node)
    {
        MavenArtifactNode winner = winnerOf(analysis, node);

        List<PathHead> heads = new ArrayList<>();

        for (MavenArtifactNode occurrence : analysis.occurrencesOf(node))
        {
            heads.add(new PathHead(occurrence, depthOf(occurrence), occurrence == winner));
        }

        heads.sort((left, right) -> Integer.compare(left.depth(), right.depth()));

        return heads;
    }

    public static ResolutionVerdict verdictFor(DependencyAnalysis analysis, MavenArtifactNode node)
    {
        List<MavenArtifactNode> occurrences = analysis.occurrencesOf(node);

        MavenArtifactNode winner = winnerOf(analysis, node);

        if (winner == null)
        {
            return null;
        }

        return new ResolutionVerdict(
            winner.getArtifact().getVersion(),
            ruleOf(winner, occurrences),
            depthOf(winner),
            occurrences.size(),
            StringUtils.trimToEmpty(winner.getPremanagedVersion()));
    }

    private static MavenArtifactNode winnerOf(DependencyAnalysis analysis, MavenArtifactNode node)
    {
        List<MavenArtifactNode> occurrences = analysis.occurrencesOf(node);

        for (MavenArtifactNode occurrence : occurrences)
        {
            if (!ResolutionReason.of(occurrence).isOmitted())
            {
                return occurrence;
            }
        }

        return occurrences.isEmpty() ? null : occurrences.get(0);
    }

    private static Rule ruleOf(MavenArtifactNode winner, List<MavenArtifactNode> occurrences)
    {
        if (StringUtils.isNotBlank(winner.getPremanagedVersion()))
        {
            return Rule.MANAGED;
        }

        return (distinctVersionsOf(occurrences).size() > 1) ? Rule.NEAREST_WINS : Rule.SINGLE;
    }

    private static Set<String> distinctVersionsOf(List<MavenArtifactNode> occurrences)
    {
        Set<String> versions = new LinkedHashSet<>();

        for (MavenArtifactNode occurrence : occurrences)
        {
            versions.add(occurrence.getArtifact().getVersion());
        }

        return versions;
    }

    public enum Rule
    {
        SINGLE,
        NEAREST_WINS,
        MANAGED
    }

    public record PathHead(MavenArtifactNode occurrence, int depth, boolean winner)
    {
    }

    public record ResolutionVerdict(
        String selectedVersion, Rule rule, int winnerDepth, int pathCount, String declaredVersion)
    {
    }
}
