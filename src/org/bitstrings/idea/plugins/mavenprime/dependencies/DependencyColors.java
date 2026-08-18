package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.Map;

import org.jetbrains.idea.maven.model.MavenConstants;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;

public final class DependencyColors
{
    private static final Map<String, JBColor> BY_SCOPE =
        Map.of(
            MavenConstants.SCOPE_TEST, JBColor.namedColor("MavenPrime.Scope.test", 0x046F00, 0x69AF80),
            MavenConstants.SCOPE_PROVIDED, JBColor.namedColor("MavenPrime.Scope.provided", 0x02516D, 0x028BBA),
            MavenConstants.SCOPE_RUNTIME, JBColor.namedColor("MavenPrime.Scope.runtime", 0x8D4E81, 0xB264A5),
            MavenConstants.SCOPE_SYSTEM, JBColor.namedColor("MavenPrime.Scope.system", 0x7A5C00, 0xB89B3A));

    private static final JBColor WINNER = JBColor.namedColor("MavenPrime.Paths.winner", 0x1A7F37, 0x3FB950);

    private static final Map<ResolutionReason, JBColor> BY_MARKER =
        Map.of(
            ResolutionReason.DUPLICATE,
            JBColor.namedColor("MavenPrime.Marker.duplicate", 0x6F42C1, 0xA371F7),
            ResolutionReason.EXCLUDED,
            JBColor.namedColor("MavenPrime.Marker.excluded", 0xA31515, 0xE06C75),
            ResolutionReason.CYCLE,
            JBColor.namedColor("MavenPrime.Marker.cycle", 0xB35900, 0xE3A008));

    private DependencyColors()
    {
    }

    public static SimpleTextAttributes scopeAttributes(String scope, boolean colored)
    {
        JBColor color = colored ? BY_SCOPE.get(scope) : null;

        return (color == null)
            ? SimpleTextAttributes.GRAYED_ATTRIBUTES
            : new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
    }

    public static SimpleTextAttributes winnerAttributes(boolean colored)
    {
        return colored
            ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, WINNER)
            : SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES;
    }

    public static SimpleTextAttributes markerAttributes(ResolutionReason reason, boolean colored)
    {
        JBColor color = colored ? BY_MARKER.get(reason) : null;

        return (color == null)
            ? SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
            : new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, color);
    }
}
