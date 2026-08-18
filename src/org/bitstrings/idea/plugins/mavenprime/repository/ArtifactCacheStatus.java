package org.bitstrings.idea.plugins.mavenprime.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;

public record ArtifactCacheStatus(
    ArtifactCoordinates coordinates,
    Path directory,
    Path artifactFile,
    boolean present,
    String originRepository,
    List<RepositoryAttempt> attempts,
    List<Path> negativeCacheFiles)
{
    public enum Verdict
    {
        RESOLVED,
        NEVER_ATTEMPTED,
        NOT_FOUND,
        TRANSFER_FAILED
    }

    public static ArtifactCacheStatus missing(ArtifactCoordinates coordinates, Path directory)
    {
        return new ArtifactCacheStatus(
            coordinates,
            directory,
            directory.resolve(coordinates.fileName()),
            false,
            null,
            List.of(),
            List.of());
    }

    public Verdict verdict()
    {
        if (present)
        {
            return Verdict.RESOLVED;
        }

        if (attempts.stream().noneMatch(RepositoryAttempt::failed))
        {
            return Verdict.NEVER_ATTEMPTED;
        }

        return attempts.stream().anyMatch(attempt -> attempt.failed() && !attempt.notFound())
            ? Verdict.TRANSFER_FAILED
            : Verdict.NOT_FOUND;
    }

    public OptionalLong lastAttemptMillis()
    {
        return attempts.stream().mapToLong(RepositoryAttempt::lastUpdatedMillis).max();
    }

    public boolean isNegativelyCached()
    {
        return !present && attempts.stream().anyMatch(RepositoryAttempt::failed);
    }
}
