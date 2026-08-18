package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenConstants;

import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;

public final class DependencyRowText
{
    public static final String ARROW = " → ";

    public static final String SEPARATOR = " · ";

    private DependencyRowText()
    {
    }

    public static SimpleTextAttributes versionAttributes(
        ResolutionReason reason, boolean conflicted, boolean colored)
    {
        if (reason.isOmitted())
        {
            return SimpleTextAttributes.GRAYED_ATTRIBUTES;
        }

        return (conflicted && colored)
            ? SimpleTextAttributes.ERROR_ATTRIBUTES
            : SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    public static void appendName(
        MavenArtifact artifact,
        boolean showGroupId,
        SimpleTextAttributes attributes,
        BiConsumer<String, SimpleTextAttributes> appender)
    {
        if (showGroupId)
        {
            appender.accept(artifact.getGroupId() + ':', SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        appender.accept(artifact.getArtifactId(), attributes);
    }

    public static void appendVersion(
        MavenArtifactNode node,
        ResolutionReason reason,
        SimpleTextAttributes attributes,
        BiConsumer<String, SimpleTextAttributes> appender)
    {
        VersionTransition transition = reason.transition(node);

        if (transition == null)
        {
            appender.accept(':' + node.getArtifact().getVersion(), attributes);

            return;
        }

        appender.accept(':' + transition.from(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        appender.accept(ARROW, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        appender.accept(transition.to(), attributes);
    }

    public static void appendScope(SimpleColoredComponent row, MavenArtifact artifact, boolean colored)
    {
        String scope = DependencyScopeFilter.effectiveScope(artifact);

        if (MavenConstants.SCOPE_COMPILE.equals(scope))
        {
            return;
        }

        row.append(StringUtils.SPACE + scope, DependencyColors.scopeAttributes(scope, colored));
    }

    public static void appendSize(SimpleColoredComponent row, ArtifactSizes sizes, MavenArtifactNode node)
    {
        long own = sizes.sizeOf(node.getArtifact());
        long subtree = sizes.subtreeSizeOf(node);

        row.append(SEPARATOR + Formats.formatFileSize(own), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);

        if (subtree > own)
        {
            row.append(
                " (" + Formats.formatFileSize(subtree) + ')', SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
        }
    }

    public static void appendMarker(SimpleColoredComponent row, ResolutionReason reason, boolean colored)
    {
        String marker = reason.marker();

        if (marker == null)
        {
            return;
        }

        row.append(StringUtils.SPACE + marker, DependencyColors.markerAttributes(reason, colored));
    }
}
