package org.bitstrings.idea.plugins.mavenprime.editor;

import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.utils.MavenUtil;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;

final class PomModules
{
    private PomModules()
    {
    }

    static MavenProject of(XmlTag tag)
    {
        PsiFile file = tag.getContainingFile();

        VirtualFile pom = (file == null) ? null : file.getOriginalFile().getVirtualFile();

        if (pom == null)
        {
            return null;
        }

        Project project = tag.getProject();

        if (!MavenUtil.isPomFile(project, pom))
        {
            return null;
        }

        MavenProject module = MavenProjects.byPomFile(project, pom);

        return ((module != null) && pom.equals(module.getFile())) ? module : null;
    }
}
