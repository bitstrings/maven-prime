package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyExcluder;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;

public final class ExcludeTransitiveIntention
    extends PomDependencyIntention
{
    @Override
    public String getText()
    {
        return MavenPrimeBundle.message("mavenprime.editor.exclude");
    }

    @Override
    protected boolean isAvailableFor(Project project, PomDependency dependency)
    {
        return PomDependencyNodes.hasTransitives(dependency);
    }

    @Override
    protected void invokeFor(Project project, Editor editor, PomDependency dependency)
    {
        List<MavenArtifactNode> transitives = PomDependencyNodes.transitivesOf(dependency);

        if (transitives.isEmpty())
        {
            return;
        }

        JBPopupFactory
            .getInstance()
            .createPopupChooserBuilder(transitives)
            .setTitle(MavenPrimeBundle.message("mavenprime.editor.exclude.title", dependency.key()))
            .setNamerForFiltering(ExcludeTransitiveIntention::labelOf)
            .setRenderer(new ArtifactNodeRenderer())
            .setItemChosenCallback(
                node -> DependencyExcluder.exclude(project, dependency.module(), node))
            .createPopup()
            .showInBestPositionFor(editor);
    }

    static String labelOf(MavenArtifactNode node)
    {
        return ArtifactCoordinates.of(node.getArtifact()).shortForm();
    }
}
