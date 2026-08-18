package org.bitstrings.idea.plugins.mavenprime.repository;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

public record RepositoryFiles(
    Path directory, Path artifact, Path pom, Path sources, Path javadoc, long sizeBytes)
{
    static final String SOURCES_CLASSIFIER = "sources";

    static final String JAVADOC_CLASSIFIER = "javadoc";

    private static final String JAR_EXTENSION = "jar";

    private static final String POM_EXTENSION = "pom";

    public static RepositoryFiles of(Path localRepository, ArtifactCoordinates coordinates)
    {
        Path directory = coordinates.directoryIn(localRepository);

        return new RepositoryFiles(
            directory,
            regularFile(directory, coordinates.fileName()),
            regularFile(directory, coordinates.fileName(StringUtils.EMPTY, POM_EXTENSION)),
            regularFile(directory, coordinates.fileName(SOURCES_CLASSIFIER, JAR_EXTENSION)),
            regularFile(directory, coordinates.fileName(JAVADOC_CLASSIFIER, JAR_EXTENSION)),
            sizeOf(directory));
    }

    public Path presentableFile()
    {
        if (artifact != null)
        {
            return artifact;
        }

        return (pom != null) ? pom : directory;
    }

    private static Path regularFile(Path directory, String fileName)
    {
        Path candidate = directory.resolve(fileName);

        return Files.isRegularFile(candidate) ? candidate : null;
    }

    private static long sizeOf(Path directory)
    {
        long total = 0L;

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory))
        {
            for (Path entry : entries)
            {
                total += Files.isRegularFile(entry) ? Files.size(entry) : 0L;
            }
        }
        catch (IOException unreadable)
        {
            return 0L;
        }

        return total;
    }
}
