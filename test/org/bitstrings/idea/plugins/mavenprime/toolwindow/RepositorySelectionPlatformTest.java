package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class RepositorySelectionPlatformTest
    extends BasePlatformTestCase
{
    private static final ArtifactCoordinates LANG3 = version("commons-lang3", "3.14.0");

    private static final ArtifactCoordinates GUAVA = version("guava", "33.0");

    public void testSelectionIndexOf_nothingSelectedBefore_selectsTheFirstResult()
    {
        assertEquals(0, RepositoryPanel.selectionIndexOf(List.of(LANG3, GUAVA), null));
    }

    public void testSelectionIndexOf_previousSelectionStillPresent_keepsIt()
    {
        assertEquals(1, RepositoryPanel.selectionIndexOf(List.of(LANG3, GUAVA), GUAVA));
    }

    public void testSelectionIndexOf_previousSelectionGoneFromTheResults_fallsBackToTheFirst()
    {
        assertEquals(0, RepositoryPanel.selectionIndexOf(List.of(LANG3), GUAVA));
    }

    public void testSelectionIndexOf_emptyResults_reportsTheFirstIndex()
    {
        assertEquals(0, RepositoryPanel.selectionIndexOf(List.of(), LANG3));
    }

    private static ArtifactCoordinates version(String artifactId, String version)
    {
        return new ArtifactCoordinates("org.example", artifactId, "jar", StringUtils.EMPTY, version);
    }
}
