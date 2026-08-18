package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.model.MavenRemoteRepository;
import org.jetbrains.idea.maven.project.MavenEmbeddersManager;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;

public final class ProjectRepositories
{
    private static final Logger LOG = Logger.getInstance(ProjectRepositories.class);

    private ProjectRepositories()
    {
    }

    public static List<MavenRemoteRepository> declaredIn(
        Project project, MavenProject module, ArtifactKind kind)
    {
        Map<String, MavenRemoteRepository> byUrl = new LinkedHashMap<>();

        for (MavenProject candidate : (module == null) ? MavenProjects.all(project) : List.of(module))
        {
            for (MavenRemoteRepository repository : repositoriesOf(candidate, kind))
            {
                byUrl.putIfAbsent(StringUtils.trimToEmpty(repository.getUrl()), repository);
            }
        }

        byUrl.remove(StringUtils.EMPTY);

        return List.copyOf(byUrl.values());
    }

    public static List<MavenRemoteRepository> effective(
        Project project, MavenProject module, ArtifactKind kind)
    {
        List<MavenRemoteRepository> declared = declaredIn(project, module, kind);

        MavenProject anchor = (module == null) ? firstOf(project) : module;

        if ((anchor == null) || declared.isEmpty())
        {
            return declared;
        }

        MavenEmbeddersManager embedders = MavenProjectsManager.getInstance(project).getEmbeddersManager();

        MavenEmbedderWrapper embedder = null;

        try
        {
            embedder = embedders.getEmbedder(anchor, MavenEmbeddersManager.FOR_DEPENDENCIES_RESOLVE);

            return sorted(embedder.resolveRepositories(declared), declared);
        }
        catch (ProcessCanceledException canceled)
        {
            throw canceled;
        }
        catch (RuntimeException unavailable)
        {
            LOG.debug("Maven Prime could not resolve the project repositories through Maven", unavailable);

            return declared;
        }
        finally
        {
            if (embedder != null)
            {
                embedders.release(embedder);
            }
        }
    }

    private static List<MavenRemoteRepository> sorted(
        Set<MavenRemoteRepository> resolved, List<MavenRemoteRepository> fallback)
    {
        if ((resolved == null) || resolved.isEmpty())
        {
            return fallback;
        }

        List<MavenRemoteRepository> ordered = new ArrayList<>(resolved);

        ordered.sort(
            Comparator.comparing(
                repository -> StringUtils.defaultString(repository.getId()),
                String.CASE_INSENSITIVE_ORDER));

        return List.copyOf(ordered);
    }

    private static MavenProject firstOf(Project project)
    {
        List<MavenProject> all = MavenProjects.all(project);

        return all.isEmpty() ? null : all.get(0);
    }

    private static List<MavenRemoteRepository> repositoriesOf(MavenProject module, ArtifactKind kind)
    {
        return (kind == ArtifactKind.PLUGIN)
            ? module.getRemotePluginRepositories()
            : module.getRemoteRepositories();
    }
}
