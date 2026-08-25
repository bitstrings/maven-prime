package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.util.SystemInfo;

public final class MvndLayout
{
    public static final String EMBEDDED_MAVEN_DIRECTORY = "mvn";

    private static final String BIN_DIRECTORY = "bin";

    private static final List<String> DAEMON_LIBRARY_DIRECTORIES =
        List.of("mvn/lib/mvnd", "mvn/lib/ext", "lib/mvnd", "lib");

    private static final List<String> WINDOWS_EXECUTABLES = List.of("mvnd.exe", "mvnd.cmd", "mvnd.bat");

    private static final List<String> UNIX_EXECUTABLES = List.of("mvnd", "mvnd.sh");

    private static final String DAEMON_JAR_GLOB = "mvnd-*.jar";

    private static final Pattern DAEMON_JAR_PATTERN = Pattern.compile("mvnd-[a-zA-Z]+-(\\d.*)\\.jar");

    private MvndLayout()
    {
    }

    public static Path realPath(Path path)
    {
        try
        {
            return path.toRealPath();
        }
        catch (IOException unresolvable)
        {
            return path.toAbsolutePath().normalize();
        }
    }

    public static boolean isDaemonHome(Path home)
    {
        return findExecutable(home) != null;
    }

    public static boolean isDaemonDistribution(Path home)
    {
        return isDaemonHome(home) && Files.isDirectory(home.resolve(EMBEDDED_MAVEN_DIRECTORY));
    }

    public static Path findExecutable(Path home)
    {
        if ((home == null) || !Files.isDirectory(home))
        {
            return null;
        }

        Path binDirectory = home.resolve(BIN_DIRECTORY);

        for (String candidate : executableNames())
        {
            Path executable = binDirectory.resolve(candidate);

            if (Files.isRegularFile(executable))
            {
                return executable;
            }
        }

        return null;
    }

    public static List<String> executableNames()
    {
        return SystemInfo.isWindows ? WINDOWS_EXECUTABLES : UNIX_EXECUTABLES;
    }

    public static Path homeOfExecutable(Path executable)
    {
        Path binDirectory = (executable == null) ? null : executable.getParent();

        return (binDirectory == null) ? null : binDirectory.getParent();
    }

    public static Path embeddedMavenHome(Path home)
    {
        Path embedded = home.resolve(EMBEDDED_MAVEN_DIRECTORY);

        return Files.isDirectory(embedded) ? embedded : home;
    }

    public static MavenVersion daemonVersion(Path home)
    {
        if (home == null)
        {
            return MavenVersion.UNKNOWN;
        }

        for (String libraryDirectory : DAEMON_LIBRARY_DIRECTORIES)
        {
            MavenVersion version = daemonJarVersion(home.resolve(libraryDirectory));

            if (version.isKnown())
            {
                return version;
            }
        }

        return MavenVersion.UNKNOWN;
    }

    private static MavenVersion daemonJarVersion(Path libraryDirectory)
    {
        if (!Files.isDirectory(libraryDirectory))
        {
            return MavenVersion.UNKNOWN;
        }

        try (DirectoryStream<Path> jars = Files.newDirectoryStream(libraryDirectory, DAEMON_JAR_GLOB))
        {
            for (Path jar : jars)
            {
                Matcher matcher = DAEMON_JAR_PATTERN.matcher(jar.getFileName().toString());

                if (matcher.matches())
                {
                    return MavenVersion.parse(matcher.group(1));
                }
            }
        }
        catch (IOException ignored)
        {
            return MavenVersion.UNKNOWN;
        }

        return MavenVersion.UNKNOWN;
    }
}
