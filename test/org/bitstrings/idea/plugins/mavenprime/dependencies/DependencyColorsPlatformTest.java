package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jetbrains.idea.maven.model.MavenConstants;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.SimpleTextAttributes;

public class DependencyColorsPlatformTest
    extends BasePlatformTestCase
{
    public void testWinnerAttributes_colorTurnedOff_fallsBackToTheGrayedRendering()
    {
        assertEquals(
            SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES, DependencyColors.winnerAttributes(false));
    }

    public void testWinnerAttributes_colorTurnedOn_paintsTheWinnerItsOwnColor()
    {
        assertNotNull(DependencyColors.winnerAttributes(true).getFgColor());
    }

    public void testWinnerAttributes_colorTurnedOn_staysBoldSoItReadsAsTheChosenPath()
    {
        assertEquals(
            SimpleTextAttributes.STYLE_BOLD, DependencyColors.winnerAttributes(true).getStyle());
    }

    public void testWinnerAttributes_colorTurnedOn_doesNotReuseAnyScopeColor()
    {
        assertNoScopeShares(DependencyColors.winnerAttributes(true));
    }

    public void testMarkerAttributes_aDuplicateWithColorOn_paintsItItsOwnColor()
    {
        assertNotNull(
            DependencyColors.markerAttributes(ResolutionReason.DUPLICATE, true).getFgColor());
    }

    public void testMarkerAttributes_aDuplicateWithColorOff_fallsBackToTheGrayedRendering()
    {
        assertEquals(
            SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES,
            DependencyColors.markerAttributes(ResolutionReason.DUPLICATE, false));
    }

    public void testMarkerAttributes_everyMarkerTheTreeRenders_paintsItItsOwnColor()
    {
        for (ResolutionReason reason : renderedMarkers())
        {
            assertFalse(
                "a marker the tree prints must not fall back to the grayed rendering: " + reason,
                SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
                    .getFgColor()
                    .equals(DependencyColors.markerAttributes(reason, true).getFgColor()));
        }
    }

    public void testMarkerAttributes_everyMarkerTheTreeRenders_reusesNoScopeColor()
    {
        for (ResolutionReason reason : renderedMarkers())
        {
            assertNoScopeShares(DependencyColors.markerAttributes(reason, true));
        }
    }

    public void testMarkerAttributes_theMarkersTheTreeRenders_neverShareAColor()
    {
        List<Color> colors = new ArrayList<>();

        for (ResolutionReason reason : renderedMarkers())
        {
            colors.add(DependencyColors.markerAttributes(reason, true).getFgColor());
        }

        assertEquals(
            "a reader tells one marker from another by color, so no two of them may share one",
            colors.size(),
            new HashSet<>(colors).size());
    }

    public void testMarkerAttributes_aDuplicateAndTheWinner_readDifferentlyFromEachOther()
    {
        assertFalse(
            DependencyColors
                .markerAttributes(ResolutionReason.DUPLICATE, true)
                .getFgColor()
                .equals(DependencyColors.winnerAttributes(true).getFgColor()));
    }

    private static List<ResolutionReason> renderedMarkers()
    {
        List<ResolutionReason> markers = new ArrayList<>();

        for (ResolutionReason reason : ResolutionReason.values())
        {
            if (reason.marker() != null)
            {
                markers.add(reason);
            }
        }

        return markers;
    }

    private static void assertNoScopeShares(SimpleTextAttributes attributes)
    {
        for (String scope : new String[]
        {
            MavenConstants.SCOPE_TEST,
            MavenConstants.SCOPE_PROVIDED,
            MavenConstants.SCOPE_RUNTIME,
            MavenConstants.SCOPE_SYSTEM
        })
        {
            assertFalse(
                "an outcome sharing a scope color reads as if it meant that scope: " + scope,
                attributes.getFgColor().equals(DependencyColors.scopeAttributes(scope, true).getFgColor()));
        }
    }
}
