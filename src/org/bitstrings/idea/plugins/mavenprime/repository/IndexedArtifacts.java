package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record IndexedArtifacts(List<ArtifactCoordinates> all, Map<String, List<ArtifactCoordinates>> byArtifact)
{
    public static final IndexedArtifacts EMPTY = new IndexedArtifacts(List.of(), Map.of());

    public IndexedArtifacts
    {
        all = List.copyOf(all);
        byArtifact = byArtifact.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    public static IndexedArtifacts of(List<ArtifactCoordinates> scanned)
    {
        Map<String, List<ArtifactCoordinates>> grouped = new HashMap<>();

        for (ArtifactCoordinates candidate : scanned)
        {
            grouped.computeIfAbsent(candidate.artifactKey(), key -> new ArrayList<>()).add(candidate);
        }

        return new IndexedArtifacts(scanned, grouped);
    }

    public List<ArtifactCoordinates> versionsOf(ArtifactCoordinates coordinates)
    {
        return byArtifact.getOrDefault(coordinates.artifactKey(), List.of());
    }
}
