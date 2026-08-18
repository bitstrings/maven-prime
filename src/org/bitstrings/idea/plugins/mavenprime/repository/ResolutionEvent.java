package org.bitstrings.idea.plugins.mavenprime.repository;

public sealed interface ResolutionEvent
{
    String coordinates();

    String repository();

    record ArtifactDownloading(String coordinates, String repository, String url)
        implements ResolutionEvent
    {
    }

    record ArtifactFailed(String coordinates, String repository, String message)
        implements ResolutionEvent
    {
    }

    record MetadataFailed(String coordinates, String repository, String message)
        implements ResolutionEvent
    {
    }
}
