package org.bitstrings.idea.plugins.mavenprime.spy;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;

public final class SpyReporter
{
    private static final int MAX_CAUSE_DEPTH = 10;

    private static final char KEY_SEPARATOR = '\n';

    private final LineSink sink;

    private final LongSupplier clock;

    private volatile long sessionStart;

    private final Map<String, Long> projectStarts = new ConcurrentHashMap<String, Long>();

    private final Map<String, Long> mojoStarts = new ConcurrentHashMap<String, Long>();

    private final Map<String, Long> downloadStarts = new ConcurrentHashMap<String, Long>();

    public SpyReporter(LineSink sink)
    {
        this(sink, System::currentTimeMillis);
    }

    SpyReporter(LineSink sink, LongSupplier clock)
    {
        this.sink = sink;
        this.clock = clock;
    }

    public void report(ExecutionEvent event)
    {
        switch (event.getType())
        {
            case SessionStarted:
                sessionStart = clock.getAsLong();
                write(SpyProtocol.encode(SpyProtocol.SESSION_STARTED, projectCount(event)));
                writeReactorEdges(graphOf(event));
                break;

            case SessionEnded:
                write(SpyProtocol.encode(SpyProtocol.SESSION_ENDED, sessionFailure(failure(event), resultOf(event))));
                break;

            case ProjectStarted:
                projectStarts.put(key(event), Long.valueOf(clock.getAsLong()));
                write(SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, key(event), name(event)));
                writeProvenance(event);
                break;

            case ProjectSucceeded:
                write(SpyProtocol.encode(SpyProtocol.PROJECT_SUCCEEDED, key(event)));
                writeProjectTiming(event);
                break;

            case ProjectFailed:
                write(SpyProtocol.encode(SpyProtocol.PROJECT_FAILED, key(event), failure(event)));
                writeProjectTiming(event);
                break;

            case ProjectSkipped:
                write(SpyProtocol.encode(SpyProtocol.PROJECT_SKIPPED, key(event)));
                break;

            case MojoStarted:
                mojoStarts.put(mojoKey(event), Long.valueOf(clock.getAsLong()));
                write(
                    SpyProtocol.encode(
                        SpyProtocol.MOJO_STARTED, key(event), goal(event), executionId(event)));
                break;

            case MojoSucceeded:
                writeMojoTiming(event);
                break;

            case MojoFailed:
                write(SpyProtocol.encode(SpyProtocol.MOJO_FAILED, key(event), goal(event), failure(event)));
                writeDiagnostics(event);
                writeMojoTiming(event);
                break;

            case MojoSkipped:
                mojoStarts.remove(mojoKey(event));
                break;

            default:
                break;
        }
    }

    public void report(RepositoryEvent event)
    {
        switch (event.getType())
        {
            case ARTIFACT_DOWNLOADING:
                downloadStarts.put(downloadKey(event), Long.valueOf(clock.getAsLong()));
                write(
                    SpyProtocol.encode(
                        SpyProtocol.ARTIFACT_DOWNLOADING,
                        coordinates(event.getArtifact()),
                        repositoryId(event.getRepository()),
                        repositoryUrl(event.getRepository())));
                break;

            case METADATA_DOWNLOADING:
                downloadStarts.put(downloadKey(event), Long.valueOf(clock.getAsLong()));
                break;

            case ARTIFACT_DOWNLOADED:
                writeDownloadTiming(event, SpyProtocol.ARTIFACT_DOWNLOADED);
                break;

            case METADATA_DOWNLOADED:
                writeDownloadTiming(event, SpyProtocol.METADATA_DOWNLOADED);
                break;

            case ARTIFACT_RESOLVED:
            case ARTIFACT_DESCRIPTOR_INVALID:
            case ARTIFACT_DESCRIPTOR_MISSING:
                writeArtifactFailure(event.getArtifact(), event.getRepository(), firstException(event));
                break;

            case METADATA_INVALID:
                writeMetadataFailure(event.getMetadata(), event.getRepository(), firstException(event));
                break;

            default:
                break;
        }
    }

    void writeArtifactFailure(Artifact artifact, ArtifactRepository repository, String failure)
    {
        if (failure.length() > 0)
        {
            write(
                SpyProtocol.encode(
                    SpyProtocol.ARTIFACT_FAILED,
                    coordinates(artifact),
                    repositoryId(repository),
                    failure));
        }
    }

    void writeMetadataFailure(Metadata metadata, ArtifactRepository repository, String failure)
    {
        if (failure.length() > 0)
        {
            write(
                SpyProtocol.encode(
                    SpyProtocol.METADATA_FAILED,
                    metadataCoordinates(metadata),
                    repositoryId(repository),
                    failure));
        }
    }

    void writeReactorEdges(ProjectDependencyGraph graph)
    {
        if (graph == null)
        {
            return;
        }

        for (MavenProject project : graph.getSortedProjects())
        {
            for (MavenProject upstream : graph.getUpstreamProjects(project, false))
            {
                write(SpyProtocol.encode(SpyProtocol.REACTOR_EDGE, coordinates(project), coordinates(upstream)));
            }
        }
    }

    private void writeProvenance(ExecutionEvent event)
    {
        MavenProject project = event.getProject();

        if (project == null)
        {
            return;
        }

        String module = key(event);

        for (Dependency dependency : project.getModel().getDependencies())
        {
            write(
                SpyProtocol.encode(
                    prefixed(
                        SpyProtocol.MODEL_DEPENDENCY,
                        module,
                        dependency.getManagementKey(),
                        nullToEmpty(dependency.getVersion()),
                        dependency.getLocation("version"))));
        }

        writePlugins(module, project.getModel().getBuild());

        InputLocation properties = project.getModel().getLocation("properties");

        for (Map.Entry<Object, Object> property : project.getModel().getProperties().entrySet())
        {
            write(
                SpyProtocol.encode(
                    prefixed(
                        SpyProtocol.MODEL_PROPERTY,
                        module,
                        String.valueOf(property.getKey()),
                        String.valueOf(property.getValue()),
                        (properties == null) ? null : properties.getLocation(property.getKey()))));
        }
    }

    private void writePlugins(String module, Build build)
    {
        if (build == null)
        {
            return;
        }

        for (Plugin plugin : build.getPlugins())
        {
            write(
                SpyProtocol.encode(
                    prefixed(
                        SpyProtocol.MODEL_PLUGIN,
                        module,
                        plugin.getKey(),
                        nullToEmpty(plugin.getVersion()),
                        plugin.getLocation("version"))));
        }
    }

    private void writeProjectTiming(ExecutionEvent event)
    {
        Long started = projectStarts.remove(key(event));

        if (started == null)
        {
            return;
        }

        write(
            SpyProtocol.encode(
                SpyProtocol.PROJECT_TIMING,
                key(event),
                offset(started.longValue()),
                duration(started.longValue()),
                worker()));
    }

    private void writeMojoTiming(ExecutionEvent event)
    {
        Long started = mojoStarts.remove(mojoKey(event));

        if (started == null)
        {
            return;
        }

        write(
            SpyProtocol.encode(
                SpyProtocol.MOJO_TIMING,
                key(event),
                goal(event),
                executionId(event),
                offset(started.longValue()),
                duration(started.longValue()),
                phase(event),
                worker()));
    }

    private void writeDownloadTiming(RepositoryEvent event, String type)
    {
        Long started = downloadStarts.remove(downloadKey(event));

        if (started == null)
        {
            return;
        }

        write(
            SpyProtocol.encode(
                type,
                subjectOf(event),
                repositoryId(event.getRepository()),
                offset(started.longValue()),
                duration(started.longValue()),
                String.valueOf(sizeOf(event))));
    }

    private void writeDiagnostics(ExecutionEvent event)
    {
        String detail = longMessage(event);

        if (detail.equals(failure(event)))
        {
            return;
        }

        for (String line : detail.split("\n"))
        {
            String diagnostic = line.trim();

            if (diagnostic.length() > 0)
            {
                write(SpyProtocol.encode(SpyProtocol.MOJO_PROBLEM, key(event), diagnostic));
            }
        }
    }

    String offset(long started)
    {
        return String.valueOf(Math.max(0L, started - sessionStart));
    }

    String duration(long started)
    {
        return String.valueOf(Math.max(0L, clock.getAsLong() - started));
    }

    static String sessionFailure(String reported, MavenExecutionResult result)
    {
        if (reported.length() > 0)
        {
            return reported;
        }

        return ((result == null) || !result.hasExceptions()) ? "" : describe(result.getExceptions().get(0));
    }

    static String coordinates(Artifact artifact)
    {
        if (artifact == null)
        {
            return "";
        }

        StringBuilder coordinates = new StringBuilder();

        coordinates.append(artifact.getGroupId()).append(':').append(artifact.getArtifactId());
        coordinates.append(':').append(artifact.getExtension());

        if ((artifact.getClassifier() != null) && (artifact.getClassifier().length() > 0))
        {
            coordinates.append(':').append(artifact.getClassifier());
        }

        return coordinates.append(':').append(artifact.getVersion()).toString();
    }

    static String metadataCoordinates(Metadata metadata)
    {
        if (metadata == null)
        {
            return "";
        }

        return metadata.getGroupId() + ':' + metadata.getArtifactId() + ':' + metadata.getVersion();
    }

    static String repositoryId(ArtifactRepository repository)
    {
        return (repository == null) ? "" : repository.getId();
    }

    static String repositoryUrl(ArtifactRepository repository)
    {
        return (repository instanceof RemoteRepository) ? ((RemoteRepository) repository).getUrl() : "";
    }

    static String describe(Throwable failure)
    {
        if (failure == null)
        {
            return "";
        }

        return (failure.getMessage() == null) ? failure.getClass().getName() : failure.getMessage();
    }

    static String longMessage(ExecutionEvent event)
    {
        Throwable failure = event.getException();

        for (int depth = 0; (failure != null) && (depth < MAX_CAUSE_DEPTH); depth++)
        {
            if (failure instanceof MojoFailureException)
            {
                String detail = ((MojoFailureException) failure).getLongMessage();

                if (detail != null)
                {
                    return detail;
                }
            }

            failure = failure.getCause();
        }

        return "";
    }

    static String mojoKey(ExecutionEvent event)
    {
        return key(event) + KEY_SEPARATOR + goal(event) + KEY_SEPARATOR + executionId(event);
    }

    static String downloadKey(RepositoryEvent event)
    {
        return subjectOf(event) + KEY_SEPARATOR + repositoryId(event.getRepository());
    }

    static String subjectOf(RepositoryEvent event)
    {
        return (event.getArtifact() == null)
            ? metadataCoordinates(event.getMetadata())
            : coordinates(event.getArtifact());
    }

    static long sizeOf(RepositoryEvent event)
    {
        File file = event.getFile();

        if ((file == null) && (event.getArtifact() != null))
        {
            file = event.getArtifact().getFile();
        }

        return ((file == null) || !file.isFile()) ? 0L : file.length();
    }

    static String key(ExecutionEvent event)
    {
        MavenProject project = event.getProject();

        return (project == null) ? "" : (project.getGroupId() + ':' + project.getArtifactId());
    }

    private static String[] prefixed(
        String type, String module, String name, String value, InputLocation location)
    {
        InputSource source = (location == null) ? null : location.getSource();

        return new String[]
        {
            type,
            module,
            name,
            value,
            (source == null) ? "" : nullToEmpty(source.getModelId()),
            (source == null) ? "" : nullToEmpty(source.getLocation()),
            (location == null) ? "0" : String.valueOf(location.getLineNumber())
        };
    }

    private static String nullToEmpty(String text)
    {
        return (text == null) ? "" : text;
    }

    private static String firstException(RepositoryEvent event)
    {
        List<Exception> failures = event.getExceptions();

        return ((failures == null) || failures.isEmpty()) ? "" : describe(failures.get(0));
    }

    private static ProjectDependencyGraph graphOf(ExecutionEvent event)
    {
        MavenSession session = event.getSession();

        return (session == null) ? null : session.getProjectDependencyGraph();
    }

    private static MavenExecutionResult resultOf(ExecutionEvent event)
    {
        MavenSession session = event.getSession();

        return (session == null) ? null : session.getResult();
    }

    private static String coordinates(MavenProject project)
    {
        return project.getGroupId() + ':' + project.getArtifactId();
    }

    private static String projectCount(ExecutionEvent event)
    {
        return (event.getSession() == null) ? "0" : String.valueOf(event.getSession().getProjects().size());
    }

    private static String name(ExecutionEvent event)
    {
        MavenProject project = event.getProject();

        return (project == null) ? "" : project.getName();
    }

    private static String goal(ExecutionEvent event)
    {
        MojoExecution execution = event.getMojoExecution();

        if (execution == null)
        {
            return "";
        }

        return execution.getArtifactId() + ':' + execution.getGoal();
    }

    private static String executionId(ExecutionEvent event)
    {
        MojoExecution execution = event.getMojoExecution();

        if ((execution == null) || (execution.getExecutionId() == null))
        {
            return "";
        }

        return execution.getExecutionId();
    }

    private static String phase(ExecutionEvent event)
    {
        MojoExecution execution = event.getMojoExecution();

        return (execution == null) ? "" : nullToEmpty(execution.getLifecyclePhase());
    }

    // Maven renames the pooled builder thread after the project it is running, so the name identifies
    // the module rather than the worker. The id survives the rename, and SpyReporterTest pins it.
    private static String worker()
    {
        return String.valueOf(Thread.currentThread().getId());
    }

    private static String failure(ExecutionEvent event)
    {
        return describe(event.getException());
    }

    private void write(String line)
    {
        sink.send(line);
    }
}
