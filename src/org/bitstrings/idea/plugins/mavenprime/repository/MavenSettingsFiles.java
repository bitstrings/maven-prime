package org.bitstrings.idea.plugins.mavenprime.repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;

import com.intellij.openapi.project.Project;
import com.intellij.util.SystemProperties;

public final class MavenSettingsFiles
{
    static final String SETTINGS_FILE = ".m2/settings.xml";

    static final String GLOBAL_SETTINGS_FILE = "conf/settings.xml";

    private MavenSettingsFiles()
    {
    }

    public static List<Path> of(Project project)
    {
        Path userHome = Paths.get(SystemProperties.getUserHome());

        List<Path> files = new ArrayList<>();

        Path global = MavenPaths.toPath(userHome, globalSettingsOf(project));

        if (global != null)
        {
            files.add(global);
        }

        Path user = MavenPaths.toPath(userHome, MavenConfiguration.settingsFileOf(project));

        files.add((user == null) ? userHome.resolve(SETTINGS_FILE) : user);

        return List.copyOf(files);
    }

    private static String globalSettingsOf(Project project)
    {
        return MavenInstallationService
            .getInstance(project)
            .resolve(BuildContext.getInstance(project).getDistribution(), null)
            .getMavenHome()
            .map(home -> home.resolve(GLOBAL_SETTINGS_FILE).toString())
            .orElse(null);
    }
}
