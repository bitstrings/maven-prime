package org.bitstrings.idea.plugins.mavenprime.actions;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.utils.actions.MavenActionUtil;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public final class MavenPrimeActionContext
{
    private MavenPrimeActionContext()
    {
    }

    public static MavenProject resolveModule(AnActionEvent event)
    {
        Project project = event.getProject();

        if (project == null)
        {
            return null;
        }

        MavenProject fromMavenView = MavenActionUtil.getMavenProject(event.getDataContext());

        if (fromMavenView != null)
        {
            return fromMavenView;
        }

        MavenProject fromFile = MavenProjects.forFile(project, event.getData(CommonDataKeys.VIRTUAL_FILE));

        if (fromFile != null)
        {
            return fromFile;
        }

        Module module = event.getData(PlatformCoreDataKeys.MODULE);

        MavenProject fromModule = MavenProjects.forModule(project, module);

        return (fromModule != null) ? fromModule : soleRoot(project);
    }

    public static MavenProject resolveModule(Project project, VirtualFile file)
    {
        MavenProject fromFile = MavenProjects.forFile(project, file);

        return (fromFile != null) ? fromFile : soleRoot(project);
    }

    private static MavenProject soleRoot(Project project)
    {
        List<MavenProject> roots = MavenProjects.roots(project);

        return (roots.size() == 1) ? roots.get(0) : null;
    }
}
