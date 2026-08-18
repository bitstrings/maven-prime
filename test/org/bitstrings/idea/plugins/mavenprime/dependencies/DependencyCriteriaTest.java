package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.artifact;
import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.node;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.junit.Test;

public class DependencyCriteriaTest
{
    private static final MavenArtifactNode LANG3 =
        node(null, artifact("org.apache.commons", "commons-lang3", "3.14.0"), MavenArtifactState.ADDED, null);

    private static final MavenArtifactNode OMITTED =
        node(
            null,
            artifact("com.google.guava", "guava", "31.0"),
            MavenArtifactState.CONFLICT,
            artifact("com.google.guava", "guava", "33.0"));

    private static final DependencyAnalysis ANALYSIS = DependencyAnalysis.ofTree(List.of(LANG3, OMITTED));

    @Test
    public void accepts_unfilteredCriteria_acceptsEveryNode()
    {
        assertTrue(DependencyCriteria.UNFILTERED.accepts(LANG3, ANALYSIS));
    }

    @Test
    public void accepts_searchMatchingTheArtifact_acceptsTheNode()
    {
        assertTrue(criteria("lang3").accepts(LANG3, ANALYSIS));
    }

    @Test
    public void accepts_searchMatchingNothing_rejectsTheNode()
    {
        assertFalse(criteria("hibernate").accepts(LANG3, ANALYSIS));
    }

    @Test
    public void accepts_searchInDifferingCase_stillAcceptsTheNode()
    {
        assertTrue(criteria("LANG3").accepts(LANG3, ANALYSIS));
    }

    @Test
    public void accepts_omittedFilterAgainstAnAddedNode_rejectsTheNode()
    {
        assertFalse(omittedOnly().accepts(LANG3, ANALYSIS));
    }

    @Test
    public void accepts_omittedFilterAgainstAnOmittedNode_acceptsTheNode()
    {
        assertTrue(omittedOnly().accepts(OMITTED, ANALYSIS));
    }

    @Test
    public void isNarrowed_unfilteredCriteria_isNotNarrowed()
    {
        assertFalse(DependencyCriteria.UNFILTERED.isNarrowed());
    }

    @Test
    public void isNarrowed_searchOnly_isNarrowed()
    {
        assertTrue(criteria("lang3").isNarrowed());
    }

    @Test
    public void isNarrowed_filterModeOnly_isNarrowed()
    {
        assertTrue(omittedOnly().isNarrowed());
    }

    @Test
    public void isNarrowed_scopeFilterOnly_isNarrowed()
    {
        assertTrue(
            new DependencyCriteria(
                StringUtils.EMPTY, DependencyFilterMode.ALL, DependencyScopeFilter.TEST).isNarrowed());
    }

    @Test
    public void hasSearch_blankSearch_reportsNoSearch()
    {
        assertFalse(DependencyCriteria.UNFILTERED.hasSearch());
    }

    private static DependencyCriteria criteria(String search)
    {
        return new DependencyCriteria(search, DependencyFilterMode.ALL, DependencyScopeFilter.ALL);
    }

    private static DependencyCriteria omittedOnly()
    {
        return new DependencyCriteria(
            StringUtils.EMPTY, DependencyFilterMode.OMITTED, DependencyScopeFilter.ALL);
    }
}
