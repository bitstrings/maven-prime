package org.bitstrings.idea.plugins.mavenprime.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public final class MavenProjects
{
    private MavenProjects()
    {
    }

    public static boolean isMavenized(Project project)
    {
        MavenProjectsManager manager = MavenProjectsManager.getInstanceIfCreated(project);

        return (manager != null) && manager.isMavenizedProject();
    }

    public static boolean isPossiblyMavenized(Project project)
    {
        MavenProjectsManager manager = MavenProjectsManager.getInstanceIfCreated(project);

        return (manager == null) || manager.isMavenizedProject();
    }

    public static List<MavenProject> all(Project project)
    {
        return MavenProjectsManager.getInstance(project).getProjects();
    }

    public static List<MavenProject> roots(Project project)
    {
        return MavenProjectsManager.getInstance(project).getRootProjects();
    }

    public static Set<String> allProfiles(Project project)
    {
        Set<String> profiles = new LinkedHashSet<>();

        for (MavenProject mavenProject : all(project))
        {
            profiles.addAll(mavenProject.getProfilesIds());
        }

        return profiles;
    }

    public static String moduleKeyOf(MavenProject module)
    {
        return (module == null)
            ? StringUtils.EMPTY
            : (module.getMavenId().getGroupId() + ':' + module.getMavenId().getArtifactId());
    }

    public static MavenProject forFile(Project project, VirtualFile file)
    {
        if (file == null)
        {
            return null;
        }

        return ApplicationManager
            .getApplication()
            .runReadAction(
                (Computable<MavenProject>) () ->
                {
                    MavenProjectsManager manager = MavenProjectsManager.getInstance(project);

                    MavenProject containing = manager.findContainingProject(file);

                    return (containing != null) ? containing : manager.findProject(file);
                });
    }

    public static MavenProject byPomFile(Project project, VirtualFile file)
    {
        if (file == null)
        {
            return null;
        }

        return ApplicationManager
            .getApplication()
            .runReadAction(
                (Computable<MavenProject>) () -> MavenProjectsManager.getInstance(project).findProject(file));
    }

    public static MavenProject forModule(Project project, Module module)
    {
        return (module == null) ? null : MavenProjectsManager.getInstance(project).findProject(module);
    }
}
