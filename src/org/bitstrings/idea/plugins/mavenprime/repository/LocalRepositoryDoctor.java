package org.bitstrings.idea.plugins.mavenprime.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public final class LocalRepositoryDoctor
{
    static final String REMOTE_REPOSITORIES = "_remote.repositories";

    static final String LAST_UPDATED_SUFFIX = ".lastUpdated";

    private static final String UPDATED_KEY_SUFFIX = ".lastUpdated";

    private static final String ERROR_KEY_SUFFIX = ".error";

    private static final char ORIGIN_SEPARATOR = '>';

    private LocalRepositoryDoctor()
    {
    }

    public static ArtifactCacheStatus inspect(Path localRepository, ArtifactCoordinates coordinates)
    {
        Path directory = coordinates.directoryIn(localRepository);

        if (!Files.isDirectory(directory))
        {
            return ArtifactCacheStatus.missing(coordinates, directory);
        }

        Path artifactFile = directory.resolve(coordinates.fileName());

        return new ArtifactCacheStatus(
            coordinates,
            directory,
            artifactFile,
            Files.isRegularFile(artifactFile),
            originOf(directory, coordinates.fileName()),
            attempts(directory.resolve(coordinates.fileName() + LAST_UPDATED_SUFFIX)),
            negativeCacheFiles(directory));
    }

    public static int purge(ArtifactCacheStatus status)
        throws IOException
    {
        int deleted = 0;

        for (Path marker : status.negativeCacheFiles())
        {
            if (Files.deleteIfExists(marker))
            {
                deleted++;
            }
        }

        return deleted;
    }

    public static List<Path> negativeCacheFiles(Path directory)
    {
        List<Path> markers = new ArrayList<>();

        if (!Files.isDirectory(directory))
        {
            return markers;
        }

        try (DirectoryStream<Path> entries =
            Files.newDirectoryStream(directory, '*' + LAST_UPDATED_SUFFIX))
        {
            entries.forEach(markers::add);
        }
        catch (IOException unreadable)
        {
            return markers;
        }

        markers.sort(null);

        return markers;
    }

    private static List<RepositoryAttempt> attempts(Path marker)
    {
        Properties properties = read(marker);

        Map<String, RepositoryAttempt> byRepository = new TreeMap<>();

        for (String name : properties.stringPropertyNames())
        {
            if (name.endsWith(UPDATED_KEY_SUFFIX))
            {
                String repository = name.substring(0, name.length() - UPDATED_KEY_SUFFIX.length());

                byRepository.put(
                    repository,
                    new RepositoryAttempt(
                        repository,
                        NumberUtils.toLong(properties.getProperty(name), 0L),
                        properties.getProperty(repository + ERROR_KEY_SUFFIX),
                        properties.containsKey(repository + ERROR_KEY_SUFFIX)));
            }
        }

        return List.copyOf(byRepository.values());
    }

    private static String originOf(Path directory, String fileName)
    {
        Properties properties = read(directory.resolve(REMOTE_REPOSITORIES));

        Set<String> origins = new TreeSet<>();

        for (String name : properties.stringPropertyNames())
        {
            if (StringUtils.substringBefore(name, ORIGIN_SEPARATOR).equals(fileName))
            {
                origins.add(StringUtils.trimToEmpty(StringUtils.substringAfter(name, ORIGIN_SEPARATOR)));
            }
        }

        origins.remove(StringUtils.EMPTY);

        return origins.isEmpty() ? null : String.join(", ", origins);
    }

    private static Properties read(Path file)
    {
        Properties properties = new Properties();

        if (!Files.isRegularFile(file))
        {
            return properties;
        }

        try (InputStream in = Files.newInputStream(file))
        {
            properties.load(in);
        }
        catch (IOException | IllegalArgumentException unreadable)
        {
            return new Properties();
        }

        return properties;
    }
}
