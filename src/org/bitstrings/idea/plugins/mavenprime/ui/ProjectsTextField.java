package org.bitstrings.idea.plugins.mavenprime.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;

public final class ProjectsTextField
{
    private ProjectsTextField()
    {
    }

    public static TextFieldWithAutoCompletion<String> create(Project project)
    {
        return TextFieldWithAutoCompletion.create(project, selectorsIn(project), false, StringUtils.EMPTY);
    }

    public static List<String> selectorsIn(Project project)
    {
        List<MavenProject> modules = MavenProjects.all(project);

        Set<String> selectors = new LinkedHashSet<>();

        for (MavenProject module : modules)
        {
            selectors.add(':' + module.getMavenId().getArtifactId());
        }

        for (MavenProject module : modules)
        {
            addRelativePath(selectors, rootDirectoryOf(project), module.getDirectory());
        }

        return new ArrayList<>(selectors);
    }

    private static String rootDirectoryOf(Project project)
    {
        List<MavenProject> roots = MavenProjects.roots(project);

        return roots.isEmpty() ? null : roots.get(0).getDirectory();
    }

    private static void addRelativePath(Set<String> selectors, String rootDirectory, String directory)
    {
        if (StringUtils.isAnyBlank(rootDirectory, directory))
        {
            return;
        }

        try
        {
            Path relative = Path.of(rootDirectory).relativize(Path.of(directory));

            if (!relative.toString().isEmpty())
            {
                selectors.add(relative.toString().replace('\\', '/'));
            }
        }
        catch (IllegalArgumentException unrelated)
        {
            selectors.add(directory);
        }
    }
}
