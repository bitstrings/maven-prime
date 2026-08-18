package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.indices.MavenGAVIndex;
import org.jetbrains.idea.maven.indices.MavenIndicesManager;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.text.VersionComparatorUtil;

public final class RemoteArtifactIndex
{
    static final int MAX_CANDIDATES = 50_000;

    private static final String DEFAULT_EXTENSION = "jar";

    private static final Logger LOG = Logger.getInstance(RemoteArtifactIndex.class);

    private RemoteArtifactIndex()
    {
    }

    public static List<String> versionsOf(Project project, ArtifactCoordinates coordinates)
    {
        try
        {
            MavenGAVIndex index = indexOf(project);

            return (index == null)
                ? List.of()
                : newestFirst(index.getVersions(coordinates.groupId(), coordinates.artifactId()));
        }
        catch (ProcessCanceledException canceled)
        {
            throw canceled;
        }
        catch (RuntimeException unavailable)
        {
            LOG.debug("Maven Prime could not read the Maven repository index", unavailable);

            return List.of();
        }
    }

    public static List<ArtifactCoordinates> search(Project project, String fragment, int limit)
    {
        String needle = StringUtils.trimToEmpty(fragment);

        if (needle.isEmpty())
        {
            return List.of();
        }

        try
        {
            MavenGAVIndex index = indexOf(project);

            return (index == null)
                ? List.of()
                : ArtifactRanking.bestMatches(collect(index, needle), needle, limit);
        }
        catch (ProcessCanceledException canceled)
        {
            throw canceled;
        }
        catch (RuntimeException unavailable)
        {
            LOG.debug("Maven Prime could not search the Maven repository index", unavailable);

            return List.of();
        }
    }

    static boolean matches(String groupId, String artifactId, String needle)
    {
        return StringUtil.containsIgnoreCase(artifactId, needle)
            || StringUtil.containsIgnoreCase(groupId + ':' + artifactId, needle);
    }

    private static List<ArtifactCoordinates> collect(MavenGAVIndex index, String needle)
    {
        List<ArtifactCoordinates> found = new ArrayList<>();

        for (String groupId : index.getGroupIds())
        {
            for (String artifactId : index.getArtifactIds(groupId))
            {
                ProgressManager.checkCanceled();

                if (found.size() >= MAX_CANDIDATES)
                {
                    return found;
                }

                if (matches(groupId, artifactId, needle))
                {
                    newestOf(index, groupId, artifactId).ifPresent(found::add);
                }
            }
        }

        return found;
    }

    private static Optional<ArtifactCoordinates> newestOf(
        MavenGAVIndex index, String groupId, String artifactId)
    {
        List<String> versions = newestFirst(index.getVersions(groupId, artifactId));

        return versions.isEmpty()
            ? Optional.empty()
            : Optional.of(
                new ArtifactCoordinates(
                    groupId, artifactId, DEFAULT_EXTENSION, StringUtils.EMPTY, versions.get(0)));
    }

    private static MavenGAVIndex indexOf(Project project)
    {
        return MavenIndicesManager.getInstance(project).getCommonGavIndex();
    }

    static List<String> newestFirst(Collection<String> versions)
    {
        if ((versions == null) || versions.isEmpty())
        {
            return List.of();
        }

        List<String> ordered = new ArrayList<>(versions);

        ordered.sort((left, right) -> VersionComparatorUtil.compare(right, left));

        return List.copyOf(ordered);
    }

    public static String newestNewerThan(List<String> newestFirstVersions, String current)
    {
        if (StringUtils.isBlank(current))
        {
            return null;
        }

        for (String candidate : newestFirstVersions)
        {
            if (VersionComparatorUtil.compare(candidate, current) > 0)
            {
                return candidate;
            }
        }

        return null;
    }
}
