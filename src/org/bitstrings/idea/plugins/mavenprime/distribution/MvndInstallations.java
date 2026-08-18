package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.intellij.util.text.VersionComparatorUtil;

public final class MvndInstallations
{
    static final String MANAGED_DIRECTORY = ".m2/mvnd/dist";

    private MvndInstallations()
    {
    }

    public static Path rootIn(Path userHome)
    {
        return userHome.resolve(MANAGED_DIRECTORY);
    }

    public static List<Path> managedIn(Path userHome)
    {
        Path root = rootIn(userHome);

        if (!Files.isDirectory(root))
        {
            return List.of();
        }

        List<Path> installed = new ArrayList<>();

        try (DirectoryStream<Path> candidates = Files.newDirectoryStream(root))
        {
            for (Path candidate : candidates)
            {
                if (MvndLayout.isDaemonHome(candidate))
                {
                    installed.add(candidate);
                }
            }
        }
        catch (IOException unreadable)
        {
            return List.of();
        }

        installed.sort(
            Comparator.comparing(
                path -> path.getFileName().toString(),
                (left, right) -> VersionComparatorUtil.compare(right, left)));

        return List.copyOf(installed);
    }
}
