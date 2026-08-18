package org.bitstrings.idea.plugins.mavenprime.repository;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.openapi.project.Project;

public final class MavenConfiguration
{
    private MavenConfiguration()
    {
    }

    public static String settingsFileOf(Project project)
    {
        String declared = BuildContext.getInstance(project).getEnvironment().settingsFile;

        return StringUtils.isNotBlank(declared)
            ? declared.trim()
            : StringUtils.trimToNull(
                MavenProjectsManager.getInstance(project).getGeneralSettings().getUserSettingsFile());
    }
}
