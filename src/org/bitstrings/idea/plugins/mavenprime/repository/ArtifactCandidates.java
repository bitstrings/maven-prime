package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenPlugin;

import com.intellij.openapi.util.text.StringUtil;

public final class ArtifactCandidates
{
    private ArtifactCandidates()
    {
    }

    public static List<ArtifactCoordinates> ofArtifacts(List<MavenArtifact> artifacts)
    {
        List<ArtifactCoordinates> coordinates = new ArrayList<>(artifacts.size());

        for (MavenArtifact artifact : artifacts)
        {
            coordinates.add(ArtifactCoordinates.of(artifact));
        }

        coordinates.sort(ArtifactCoordinates.BY_ARTIFACT);

        return coordinates;
    }

    public static List<ArtifactCoordinates> ofPlugins(List<MavenPlugin> plugins)
    {
        List<ArtifactCoordinates> coordinates = new ArrayList<>(plugins.size());

        for (MavenPlugin plugin : plugins)
        {
            if (StringUtils.isNotBlank(plugin.getVersion()))
            {
                coordinates.add(ArtifactCoordinates.of(plugin));
            }
        }

        coordinates.sort(ArtifactCoordinates.BY_ARTIFACT);

        return coordinates;
    }

    public static List<ArtifactCoordinates> ofResolutionEvents(List<? extends ResolutionEvent> events)
    {
        Set<ArtifactCoordinates> coordinates = new LinkedHashSet<>();

        for (ResolutionEvent event : events)
        {
            ArtifactCoordinates.parse(event.coordinates()).ifPresent(coordinates::add);
        }

        return new ArrayList<>(coordinates);
    }

    public static List<ArtifactCoordinates> matching(List<ArtifactCoordinates> candidates, String needle)
    {
        String trimmed = StringUtils.trimToEmpty(needle);

        if (trimmed.isEmpty())
        {
            return candidates;
        }

        List<ArtifactCoordinates> matching = new ArrayList<>();

        for (ArtifactCoordinates candidate : candidates)
        {
            if (StringUtil.containsIgnoreCase(candidate.shortForm(), trimmed))
            {
                matching.add(candidate);
            }
        }

        return matching;
    }
}
