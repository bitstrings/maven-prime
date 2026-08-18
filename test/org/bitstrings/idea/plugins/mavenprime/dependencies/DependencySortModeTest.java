package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.artifact;
import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.node;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.junit.Test;

public class DependencySortModeTest
{
    private static final MavenArtifactNode APACHE_LANG3 = added("org.apache.commons", "commons-lang3");

    private static final MavenArtifactNode SHADED_LANG3 = added("com.example.shaded", "commons-lang3");

    private static final MavenArtifactNode GUAVA = added("com.google.guava", "guava");

    @Test
    public void comparator_artifactIdSharedByTwoGroups_ordersThemByGroupId()
    {
        assertEquals(
            List.of(SHADED_LANG3, APACHE_LANG3),
            sorted(DependencySortMode.ARTIFACT_ID, APACHE_LANG3, SHADED_LANG3));
    }

    @Test
    public void comparator_artifactIdSharedByTwoGroups_ignoresTheOrderTheyArrivedIn()
    {
        assertEquals(
            sorted(DependencySortMode.ARTIFACT_ID, APACHE_LANG3, SHADED_LANG3),
            sorted(DependencySortMode.ARTIFACT_ID, SHADED_LANG3, APACHE_LANG3));
    }

    @Test
    public void comparator_distinctArtifactIds_ordersThemAlphabetically()
    {
        assertEquals(
            List.of(APACHE_LANG3, GUAVA), sorted(DependencySortMode.ARTIFACT_ID, GUAVA, APACHE_LANG3));
    }

    @Test
    public void comparator_resolutionOrder_reportsNoComparator()
    {
        assertNull(DependencySortMode.RESOLUTION.comparator(ArtifactSizes.EMPTY));
    }

    private static List<MavenArtifactNode> sorted(DependencySortMode mode, MavenArtifactNode... nodes)
    {
        List<MavenArtifactNode> ordered = new ArrayList<>(List.of(nodes));

        ordered.sort(mode.comparator(ArtifactSizes.EMPTY));

        return ordered;
    }

    private static MavenArtifactNode added(String groupId, String artifactId)
    {
        return node(null, artifact(groupId, artifactId, "1.0"), MavenArtifactState.ADDED, null);
    }
}
