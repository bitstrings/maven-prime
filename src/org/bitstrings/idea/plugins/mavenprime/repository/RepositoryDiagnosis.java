package org.bitstrings.idea.plugins.mavenprime.repository;

import java.nio.file.Path;
import java.util.List;

public record RepositoryDiagnosis(
    ArtifactCacheStatus status, RepositoryFiles files, List<String> indexedVersions)
{
    public static RepositoryDiagnosis of(
        Path localRepository, ArtifactCoordinates coordinates, List<String> indexedVersions)
    {
        return new RepositoryDiagnosis(
            LocalRepositoryDoctor.inspect(localRepository, coordinates),
            RepositoryFiles.of(localRepository, coordinates),
            List.copyOf(indexedVersions));
    }
}
