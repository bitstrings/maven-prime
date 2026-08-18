package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.jetbrains.idea.maven.model.MavenArtifact;

public final class ArtifactLocation
{
    private ArtifactLocation()
    {
    }

    public static Path pathOf(MavenArtifact artifact)
    {
        File file = artifact.getFile();

        if (file == null)
        {
            return null;
        }

        try
        {
            return file.toPath();
        }
        catch (InvalidPathException ignored)
        {
            return null;
        }
    }

    public static Path fileOf(MavenArtifact artifact)
    {
        Path path = pathOf(artifact);

        return ((path != null) && Files.isRegularFile(path)) ? path : null;
    }

    public static Path directoryOf(MavenArtifact artifact)
    {
        Path path = pathOf(artifact);

        Path directory = (path == null) ? null : path.getParent();

        return ((directory != null) && Files.isDirectory(directory)) ? directory : null;
    }
}
