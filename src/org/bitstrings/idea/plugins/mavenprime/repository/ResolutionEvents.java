package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactDownloading;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactFailed;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.MetadataFailed;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

public final class ResolutionEvents
{
    private static final int TYPE = 0;

    private static final int COORDINATES = 1;

    private static final int REPOSITORY = 2;

    private static final int DETAIL = 3;

    private ResolutionEvents()
    {
    }

    public static Optional<ResolutionEvent> parse(String line)
    {
        String[] fields = SpyProtocol.decode(line);

        if (fields.length == 0)
        {
            return Optional.empty();
        }

        String coordinates = field(fields, COORDINATES);

        if (StringUtils.isBlank(coordinates))
        {
            return Optional.empty();
        }

        return switch (fields[TYPE])
        {
            case SpyProtocol.ARTIFACT_DOWNLOADING ->
                Optional.of(
                    new ArtifactDownloading(
                        coordinates, field(fields, REPOSITORY), field(fields, DETAIL)));
            case SpyProtocol.ARTIFACT_FAILED ->
                Optional.of(
                    new ArtifactFailed(coordinates, field(fields, REPOSITORY), field(fields, DETAIL)));
            case SpyProtocol.METADATA_FAILED ->
                Optional.of(
                    new MetadataFailed(coordinates, field(fields, REPOSITORY), field(fields, DETAIL)));
            default -> Optional.empty();
        };
    }

    private static String field(String[] fields, int index)
    {
        return (index < fields.length) ? fields[index] : StringUtils.EMPTY;
    }
}
