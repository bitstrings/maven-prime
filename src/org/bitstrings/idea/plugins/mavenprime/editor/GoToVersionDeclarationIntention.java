package org.bitstrings.idea.plugins.mavenprime.editor;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.model.ModelOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceLog;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

public final class GoToVersionDeclarationIntention
    extends PomDependencyIntention
{
    @Override
    public String getText()
    {
        return MavenPrimeBundle.message("mavenprime.editor.gotoVersion");
    }

    @Override
    protected boolean isAvailableFor(Project project, PomDependency dependency)
    {
        return originOf(project, dependency) != null;
    }

    @Override
    protected void invokeFor(Project project, Editor editor, PomDependency dependency)
    {
        ModelOrigin origin = originOf(project, dependency);

        if (origin == null)
        {
            return;
        }

        VirtualFile declaring = LocalFileSystem.getInstance().findFileByPath(origin.file());

        if (declaring != null)
        {
            new OpenFileDescriptor(project, declaring, origin.line() - 1, 0).navigate(true);
        }
    }

    private static ModelOrigin originOf(Project project, PomDependency dependency)
    {
        for (DependencyOrigin declared
            : ProvenanceLog.getInstance(project).getDependencies(dependency.moduleKey()))
        {
            if (dependency.matches(declared.name()) && declared.origin().isNavigable())
            {
                return declared.origin();
            }
        }

        return null;
    }
}
