package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

import com.intellij.openapi.util.text.StringUtil;

public record DependencyCriteria(String search, DependencyFilterMode filter, DependencyScopeFilter scope)
{
    public static final DependencyCriteria UNFILTERED =
        new DependencyCriteria(StringUtils.EMPTY, DependencyFilterMode.ALL, DependencyScopeFilter.ALL);

    public boolean accepts(MavenArtifactNode node, DependencyAnalysis analysis)
    {
        return matchesFilter(node, analysis) && scope.accepts(node.getArtifact()) && matchesSearch(node);
    }

    public boolean matchesSearch(MavenArtifactNode node)
    {
        return !hasSearch()
            || StringUtil.containsIgnoreCase(node.getArtifact().getDisplayStringFull(), search);
    }

    public boolean hasSearch()
    {
        return StringUtils.isNotEmpty(search);
    }

    public boolean isNarrowed()
    {
        return hasSearch() || (filter != DependencyFilterMode.ALL) || (scope != DependencyScopeFilter.ALL);
    }

    private boolean matchesFilter(MavenArtifactNode node, DependencyAnalysis analysis)
    {
        return switch (filter)
        {
            case CONFLICTS -> analysis.isConflicted(node);
            case OMITTED -> ResolutionReason.of(node).isOmitted();
            case ALL -> true;
        };
    }
}
