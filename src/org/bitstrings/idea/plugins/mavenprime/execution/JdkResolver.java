package org.bitstrings.idea.plugins.mavenprime.execution;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.SystemProperties;

public final class JdkResolver
{
    private JdkResolver()
    {
    }

    public static Optional<Path> resolveJavaHome(Project project, String jreName)
    {
        if (MavenRunnerSettings.USE_INTERNAL_JAVA.equals(jreName))
        {
            return asPath(SystemProperties.getJavaHome());
        }

        if (MavenRunnerSettings.USE_JAVA_HOME.equals(jreName))
        {
            return asPath(EnvironmentUtil.getValue("JAVA_HOME"));
        }

        if (StringUtils.isBlank(jreName) || MavenRunnerSettings.USE_PROJECT_JDK.equals(jreName))
        {
            return projectJdkHome(project);
        }

        Sdk jdk = ProjectJdkTable.getInstance().findJdk(jreName);

        return (jdk == null) ? projectJdkHome(project) : asPath(jdk.getHomePath());
    }

    private static Optional<Path> projectJdkHome(Project project)
    {
        Sdk projectJdk = ProjectRootManager.getInstance(project).getProjectSdk();

        return (projectJdk == null) ? Optional.empty() : asPath(projectJdk.getHomePath());
    }

    private static Optional<Path> asPath(String path)
    {
        return StringUtils.isBlank(path) ? Optional.empty() : Optional.of(Paths.get(path));
    }
}
