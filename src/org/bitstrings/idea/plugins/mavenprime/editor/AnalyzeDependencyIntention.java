package org.bitstrings.idea.plugins.mavenprime.editor;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.dependencies.AnalyzerInspection;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

public final class AnalyzeDependencyIntention
    extends PomDependencyIntention
{
    @Override
    public String getText()
    {
        return MavenPrimeBundle.message("mavenprime.editor.analyze");
    }

    @Override
    protected boolean isAvailableFor(Project project, PomDependency dependency)
    {
        return AnalyzerInspection.isAvailable(dependency.module())
            && (PomDependencyNodes.rootOf(dependency) != null);
    }

    @Override
    protected void invokeFor(Project project, Editor editor, PomDependency dependency)
    {
        MavenArtifactNode root = PomDependencyNodes.rootOf(dependency);

        if (root != null)
        {
            AnalyzerInspection.analyze(
                project, dependency.module(), ArtifactCoordinates.of(root.getArtifact()));
        }
    }
}
