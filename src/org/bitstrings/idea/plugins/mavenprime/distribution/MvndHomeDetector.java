package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;

public final class MvndHomeDetector
{
    private static final String MVND_HOME_VARIABLE = "MVND_HOME";

    private static final String MAVEN_DAEMON_HOME_VARIABLE = "MAVEN_DAEMON_HOME";

    private static final List<String> UNIX_WELL_KNOWN_HOMES =
        List.of(
            "/opt/homebrew/opt/mvnd/libexec",
            "/usr/local/opt/mvnd/libexec",
            "/opt/mvnd",
            "/usr/local/mvnd");

    private static final String SDKMAN_CURRENT = ".sdkman/candidates/mvnd/current";

    private MvndHomeDetector()
    {
    }

    public static List<Path> detect(Path userHome)
    {
        Objects.requireNonNull(userHome, "userHome cannot be null");

        Set<Path> homes = new LinkedHashSet<>();

        addIfDaemonHome(homes, fromEnvironment(MVND_HOME_VARIABLE));
        addIfDaemonHome(homes, fromEnvironment(MAVEN_DAEMON_HOME_VARIABLE));
        addIfDaemonHome(homes, fromSearchPath());
        addIfDaemonHome(homes, userHome.resolve(SDKMAN_CURRENT));

        if (!SystemInfo.isWindows)
        {
            for (String wellKnown : UNIX_WELL_KNOWN_HOMES)
            {
                addIfDaemonHome(homes, Paths.get(wellKnown));
            }
        }

        for (Path managed : MvndInstallations.managedIn(userHome))
        {
            addIfDaemonHome(homes, managed);
        }

        return List.copyOf(homes);
    }

    private static Path fromEnvironment(String variable)
    {
        String value = EnvironmentUtil.getValue(variable);

        return StringUtils.isBlank(value) ? null : Paths.get(value);
    }

    private static Path fromSearchPath()
    {
        for (String executableName : MvndLayout.executableNames())
        {
            File executable = PathEnvironmentVariableUtil.findInPath(executableName);

            if (executable != null)
            {
                return MvndLayout.homeOfExecutable(realPath(executable.toPath()));
            }
        }

        return null;
    }

    private static Path realPath(Path path)
    {
        try
        {
            return path.toRealPath();
        }
        catch (IOException ignored)
        {
            return path.toAbsolutePath().normalize();
        }
    }

    private static void addIfDaemonHome(Set<Path> homes, Path candidate)
    {
        if (candidate == null)
        {
            return;
        }

        Path home = realPath(candidate);

        if (MvndLayout.isDaemonDistribution(home))
        {
            homes.add(home);
        }
    }
}
