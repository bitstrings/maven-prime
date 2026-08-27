package org.bitstrings.idea.plugins.mavenprime.spy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;

import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Build;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SpyReporterTest
{
    private static final String MODULE = "org.example:core";

    private static final String GOAL = "maven-compiler-plugin:compile";

    private static final String EXECUTION_ID = "default-compile";

    private static final String SUREFIRE_KEY = "org.apache.maven.plugins:maven-surefire-plugin";

    private static final String PARENT_MODEL_ID = "org.example:parent:1.0.0";

    private static final String PARENT_POM = "/repo/parent/pom.xml";

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    private final RecordingSink sink = new RecordingSink();

    private final SteppedClock clock = new SteppedClock();

    private SpyReporter reporter;

    @Before
    public void createReporter()
    {
        reporter = new SpyReporter(sink, clock);
    }

    @Test
    public void report_projectStarted_writesTheModuleKeyAndItsDisplayName()
    {
        reporter.report(event(ExecutionEvent.Type.ProjectStarted).on("org.example", "core", "Core"));

        assertEquals(
            SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, MODULE, "Core"),
            sink.firstOf(SpyProtocol.PROJECT_STARTED));
    }

    @Test
    public void report_projectStartedForAModuleWithNoProject_writesAnEmptyKey()
    {
        reporter.report(event(ExecutionEvent.Type.ProjectStarted));

        assertEquals(
            SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "", ""),
            sink.firstOf(SpyProtocol.PROJECT_STARTED));
    }

    @Test
    public void report_projectStarted_writesEachBuildPluginBesideWhereItsVersionWasDeclared()
    {
        reporter.report(
            event(ExecutionEvent.Type.ProjectStarted)
                .on("org.example", "core", "Core", surefire("3.2.5", 31)));

        assertEquals(
            SpyProtocol.encode(
                SpyProtocol.MODEL_PLUGIN, MODULE, SUREFIRE_KEY, "3.2.5", PARENT_MODEL_ID, PARENT_POM, "31"),
            sink.firstOf(SpyProtocol.MODEL_PLUGIN));
    }

    @Test
    public void report_projectStartedForAModelWithoutABuild_writesNoPluginProvenance()
    {
        reporter.report(event(ExecutionEvent.Type.ProjectStarted).on("org.example", "core", "Core"));

        assertNull(sink.firstOf(SpyProtocol.MODEL_PLUGIN));
    }

    @Test
    public void report_sessionStartedOutsideAReactor_writesAProjectCountOfZero()
    {
        reporter.report(event(ExecutionEvent.Type.SessionStarted));

        assertEquals(
            SpyProtocol.encode(SpyProtocol.SESSION_STARTED, "0"),
            sink.firstOf(SpyProtocol.SESSION_STARTED));
    }

    @Test
    public void report_mojoStartedThenSucceeded_writesTheDurationItMeasured()
    {
        clock.set(1000L);
        reporter.report(event(ExecutionEvent.Type.SessionStarted));

        clock.set(1500L);
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoStarted));

        clock.set(1800L);
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoSucceeded));

        assertEquals(
            SpyProtocol.encode(
                SpyProtocol.MOJO_TIMING,
                MODULE,
                GOAL,
                EXECUTION_ID,
                "500",
                "300",
                "",
                worker()),
            sink.firstOf(SpyProtocol.MOJO_TIMING));
    }

    @Test
    public void report_aMojoBoundToALifecyclePhase_writesThePhaseItRanIn()
    {
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoStarted).inPhase("test"));
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoSucceeded).inPhase("test"));

        assertEquals("test", SpyProtocol.decode(sink.firstOf(SpyProtocol.MOJO_TIMING))[6]);
    }

    @Test
    public void report_aMojoInvokedStraightFromTheCommandLine_writesNoPhase()
    {
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoStarted));
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoSucceeded));

        assertEquals("", SpyProtocol.decode(sink.firstOf(SpyProtocol.MOJO_TIMING))[6]);
    }

    @Test
    public void report_aMojoThatSucceededWithoutEverStarting_writesNoTiming()
    {
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoSucceeded));

        assertNull(sink.firstOf(SpyProtocol.MOJO_TIMING));
    }

    @Test
    public void report_aMojoSkippedAfterStarting_forgetsItSoNoTimingIsEverWritten()
    {
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoStarted));
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoSkipped));
        reporter.report(mojoEvent(ExecutionEvent.Type.MojoSucceeded));

        assertNull(sink.firstOf(SpyProtocol.MOJO_TIMING));
    }

    @Test
    public void report_projectSucceeded_measuresItsOffsetFromTheSessionStart()
    {
        clock.set(2000L);
        reporter.report(event(ExecutionEvent.Type.SessionStarted));

        clock.set(2400L);
        reporter.report(event(ExecutionEvent.Type.ProjectStarted).on("org.example", "core", "Core"));

        clock.set(2900L);
        reporter.report(event(ExecutionEvent.Type.ProjectSucceeded).on("org.example", "core", "Core"));

        assertEquals(
            SpyProtocol.encode(
                SpyProtocol.PROJECT_TIMING, MODULE, "400", "500", worker()),
            sink.firstOf(SpyProtocol.PROJECT_TIMING));
    }

    @Test
    public void report_projectSucceeded_namesTheBuilderThreadThatRanIt()
    {
        reporter.report(event(ExecutionEvent.Type.ProjectStarted).on("org.example", "core", "Core"));
        reporter.report(event(ExecutionEvent.Type.ProjectSucceeded).on("org.example", "core", "Core"));

        assertEquals(
            "Maven renames the pooled thread after the project it is running, so only the id tells "
                + "two modules that shared a worker from two that did not",
            worker(),
            SpyProtocol.decode(sink.firstOf(SpyProtocol.PROJECT_TIMING))[4]);
    }

    @Test
    public void report_aProjectStartingBeforeTheSession_clampsItsOffsetToZero()
    {
        clock.set(5000L);
        reporter.report(event(ExecutionEvent.Type.ProjectStarted).on("org.example", "core", "Core"));

        clock.set(6000L);
        reporter.report(event(ExecutionEvent.Type.SessionStarted));
        reporter.report(event(ExecutionEvent.Type.ProjectSucceeded).on("org.example", "core", "Core"));

        assertEquals("0", SpyProtocol.decode(sink.firstOf(SpyProtocol.PROJECT_TIMING))[2]);
    }

    @Test
    public void report_mojoFailed_splitsTheLongMessageIntoOneProblemPerLine()
    {
        reporter.report(
            mojoEvent(ExecutionEvent.Type.MojoFailed)
                .failing(new MojoFailureException("short", "short", "first\nsecond")));

        assertEquals(
            Arrays.asList(
                SpyProtocol.encode(SpyProtocol.MOJO_PROBLEM, MODULE, "first"),
                SpyProtocol.encode(SpyProtocol.MOJO_PROBLEM, MODULE, "second")),
            sink.allOf(SpyProtocol.MOJO_PROBLEM));
    }

    @Test
    public void report_mojoFailedWithBlankLinesInTheDetail_skipsTheBlanks()
    {
        reporter.report(
            mojoEvent(ExecutionEvent.Type.MojoFailed)
                .failing(new MojoFailureException("short", "short", "first\n\n   \nsecond")));

        assertEquals(2, sink.allOf(SpyProtocol.MOJO_PROBLEM).size());
    }

    @Test
    public void report_mojoFailedWhoseDetailRepeatsTheFailure_writesNoProblemAtAll()
    {
        reporter.report(
            mojoEvent(ExecutionEvent.Type.MojoFailed)
                .failing(new MojoFailureException("same", "same", "same")));

        assertTrue(sink.allOf(SpyProtocol.MOJO_PROBLEM).isEmpty());
    }

    @Test
    public void writeReactorEdges_aGraphWithUpstreams_writesOneEdgePerUpstream()
    {
        MavenProject reactor = project("org.example", "reactor", "Reactor");
        MavenProject core = project("org.example", "core", "Core");

        reporter.writeReactorEdges(graph(Arrays.asList(reactor, core), core, Arrays.asList(reactor)));

        assertEquals(
            Arrays.asList(SpyProtocol.encode(SpyProtocol.REACTOR_EDGE, MODULE, "org.example:reactor")),
            sink.allOf(SpyProtocol.REACTOR_EDGE));
    }

    @Test
    public void writeReactorEdges_noGraphBecauseThereIsNoSession_writesNothing()
    {
        reporter.writeReactorEdges(null);

        assertTrue(sink.lines.isEmpty());
    }

    @Test
    public void report_anArtifactDownloaded_writesWhenItStartedHowLongItTookAndItsSize()
        throws IOException
    {
        File downloaded = fileOfSize(2048);

        clock.set(1000L);
        reporter.report(event(ExecutionEvent.Type.SessionStarted));
        reporter.report(downloadEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING, null));

        clock.set(1750L);
        reporter.report(downloadEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, downloaded));

        assertEquals(
            SpyProtocol.encode(
                SpyProtocol.ARTIFACT_DOWNLOADED,
                "org.example:widget:jar:1.0",
                "central",
                "0",
                "750",
                "2048"),
            sink.firstOf(SpyProtocol.ARTIFACT_DOWNLOADED));
    }

    @Test
    public void report_anArtifactDownloadedThatWasNeverAnnounced_writesNoTiming()
    {
        reporter.report(downloadEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, null));

        assertNull(
            "a duration measured from a start nobody recorded is the whole session, not the transfer",
            sink.firstOf(SpyProtocol.ARTIFACT_DOWNLOADED));
    }

    @Test
    public void report_metadataDownloaded_isReportedApartFromTheArtifacts()
    {
        reporter.report(metadataEvent(RepositoryEvent.EventType.METADATA_DOWNLOADING));
        reporter.report(metadataEvent(RepositoryEvent.EventType.METADATA_DOWNLOADED));

        assertEquals(
            "org.example:widget:1.0",
            SpyProtocol.decode(sink.firstOf(SpyProtocol.METADATA_DOWNLOADED))[1]);
    }

    @Test
    public void report_aMetadataDownload_isNotAnnouncedAsAnArtifactDownload()
    {
        reporter.report(metadataEvent(RepositoryEvent.EventType.METADATA_DOWNLOADING));

        assertNull(
            "the Repository tab lists what it was told is downloading, and metadata is not an artifact",
            sink.firstOf(SpyProtocol.ARTIFACT_DOWNLOADING));
    }

    @Test
    public void sizeOf_aDownloadWhoseFileTheEventNeverCarried_readsItFromTheArtifact()
        throws IOException
    {
        Artifact carrying = artifact("").setFile(fileOfSize(64));

        assertEquals(
            64L,
            SpyReporter.sizeOf(
                downloadEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, null, carrying)));
    }

    @Test
    public void sizeOf_aDownloadWithNoFileAnywhere_readsAsZeroRatherThanFailing()
    {
        assertEquals(0L, SpyReporter.sizeOf(downloadEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, null)));
    }

    @Test
    public void writeArtifactFailure_aFailureCarryingNoText_writesNothing()
    {
        reporter.writeArtifactFailure(artifact(""), remote(), "");

        assertTrue(sink.lines.isEmpty());
    }

    @Test
    public void writeArtifactFailure_aRealFailure_writesTheCoordinatesRepositoryAndReason()
    {
        reporter.writeArtifactFailure(artifact(""), remote(), "boom");

        assertEquals(
            SpyProtocol.encode(
                SpyProtocol.ARTIFACT_FAILED, "org.example:widget:jar:1.0", "central", "boom"),
            sink.firstOf(SpyProtocol.ARTIFACT_FAILED));
    }

    @Test
    public void writeMetadataFailure_aRealFailure_writesTheMetadataCoordinates()
    {
        reporter.writeMetadataFailure(metadata(), remote(), "boom");

        assertEquals(
            SpyProtocol.encode(SpyProtocol.METADATA_FAILED, "org.example:widget:1.0", "central", "boom"),
            sink.firstOf(SpyProtocol.METADATA_FAILED));
    }

    @Test
    public void coordinates_anArtifactWithoutAClassifier_leavesTheClassifierSlotOut()
    {
        assertEquals("org.example:widget:jar:1.0", SpyReporter.coordinates(artifact("")));
    }

    @Test
    public void coordinates_anArtifactWithAClassifier_placesItBeforeTheVersion()
    {
        assertEquals("org.example:widget:jar:sources:1.0", SpyReporter.coordinates(artifact("sources")));
    }

    @Test
    public void coordinates_noArtifactAtAll_yieldsAnEmptyField()
    {
        assertEquals("", SpyReporter.coordinates((Artifact) null));
    }

    @Test
    public void metadataCoordinates_noMetadataAtAll_yieldsAnEmptyField()
    {
        assertEquals("", SpyReporter.metadataCoordinates(null));
    }

    @Test
    public void repositoryId_noRepositoryAtAll_yieldsAnEmptyField()
    {
        assertEquals("", SpyReporter.repositoryId(null));
    }

    @Test
    public void repositoryUrl_aLocalRepository_yieldsNoUrlBecauseThereIsNothingToProbe()
    {
        assertEquals("", SpyReporter.repositoryUrl(new LocalRepository("/repository")));
    }

    @Test
    public void repositoryUrl_aRemoteRepository_yieldsWhereItWasFetchedFrom()
    {
        assertEquals("https://repo.example/maven2", SpyReporter.repositoryUrl(remote()));
    }

    @Test
    public void describe_aFailureCarryingNoMessage_namesItsClassInstead()
    {
        assertEquals(
            "java.lang.IllegalStateException", SpyReporter.describe(new IllegalStateException()));
    }

    @Test
    public void describe_aFailureCarryingAMessage_usesTheMessage()
    {
        assertEquals("boom", SpyReporter.describe(new IllegalStateException("boom")));
    }

    @Test
    public void describe_noFailureAtAll_yieldsAnEmptyField()
    {
        assertEquals("", SpyReporter.describe(null));
    }

    @Test
    public void sessionFailure_aResultCarryingAnException_reportsThatException()
    {
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        result.addException(new IllegalStateException("from the result"));

        assertEquals("from the result", SpyReporter.sessionFailure("", result));
    }

    @Test
    public void sessionFailure_aReportedFailureAlongsideAResultException_keepsTheReportedOne()
    {
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        result.addException(new IllegalStateException("from the result"));

        assertEquals("reported", SpyReporter.sessionFailure("reported", result));
    }

    @Test
    public void sessionFailure_noResultBecauseThereWasNoSession_reportsNoFailure()
    {
        assertEquals("", SpyReporter.sessionFailure("", null));
    }

    @Test
    public void longMessage_aMojoFailureWithinReach_returnsItsDetail()
    {
        assertEquals(
            "detail",
            SpyReporter.longMessage(
                mojoEvent(ExecutionEvent.Type.MojoFailed)
                    .failing(new MojoFailureException("short", "short", "detail"))));
    }

    @Test
    public void longMessage_aMojoFailureBuriedDeeperThanTheCauseLimit_givesUpRatherThanWalkingForever()
    {
        assertEquals(
            "",
            SpyReporter.longMessage(
                mojoEvent(ExecutionEvent.Type.MojoFailed).failing(buried(11))));
    }

    @Test
    public void longMessage_aMojoFailureJustInsideTheCauseLimit_isStillFound()
    {
        assertEquals(
            "detail",
            SpyReporter.longMessage(
                mojoEvent(ExecutionEvent.Type.MojoFailed).failing(buried(9))));
    }

    @Test
    public void mojoKey_twoExecutionsOfTheSameGoalOnOneModule_areKeptApart()
    {
        Event first = mojoEvent(ExecutionEvent.Type.MojoStarted);

        Event second =
            event(ExecutionEvent.Type.MojoStarted)
                .on("org.example", "core", "Core")
                .running("maven-compiler-plugin", "compile", "second-compile");

        assertTrue(!SpyReporter.mojoKey(first).equals(SpyReporter.mojoKey(second)));
    }

    private static String worker()
    {
        return String.valueOf(Thread.currentThread().threadId());
    }

    private static Exception buried(int depth)
    {
        Exception failure = new MojoFailureException("short", "short", "detail");

        for (int level = 0; level < depth; level++)
        {
            failure = new IllegalStateException("wrapper", failure);
        }

        return failure;
    }

    private static Artifact artifact(String classifier)
    {
        return new DefaultArtifact("org.example", "widget", classifier, "jar", "1.0");
    }

    private static Metadata metadata()
    {
        return new DefaultMetadata(
            "org.example", "widget", "1.0", "maven-metadata.xml", Metadata.Nature.RELEASE);
    }

    private File fileOfSize(int bytes)
        throws IOException
    {
        File file = temporary.newFile();

        Files.write(file.toPath(), new byte[bytes]);

        return file;
    }

    private static RepositoryEvent downloadEvent(RepositoryEvent.EventType type, File file)
    {
        return downloadEvent(type, file, artifact(""));
    }

    private static RepositoryEvent downloadEvent(
        RepositoryEvent.EventType type, File file, Artifact subject)
    {
        return new RepositoryEvent.Builder(new DefaultRepositorySystemSession(), type)
            .setArtifact(subject)
            .setRepository(remote())
            .setFile(file)
            .build();
    }

    private static RepositoryEvent metadataEvent(RepositoryEvent.EventType type)
    {
        return new RepositoryEvent.Builder(new DefaultRepositorySystemSession(), type)
            .setMetadata(metadata())
            .setRepository(remote())
            .build();
    }

    private static RemoteRepository remote()
    {
        return new RemoteRepository.Builder("central", "default", "https://repo.example/maven2").build();
    }

    private static MavenProject project(String groupId, String artifactId, String name, Plugin... plugins)
    {
        Model model = new Model();

        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setName(name);

        if (plugins.length > 0)
        {
            Build build = new Build();

            build.setPlugins(Arrays.asList(plugins));
            model.setBuild(build);
        }

        return new MavenProject(model);
    }

    private static Plugin surefire(String version, int line)
    {
        InputSource source = new InputSource();

        source.setModelId(PARENT_MODEL_ID);
        source.setLocation(PARENT_POM);

        Plugin plugin = new Plugin();

        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-surefire-plugin");
        plugin.setVersion(version);
        plugin.setLocation("version", new InputLocation(line, 1, source));

        return plugin;
    }

    private static ProjectDependencyGraph graph(
        List<MavenProject> sorted, MavenProject downstream, List<MavenProject> upstreams)
    {
        return new ProjectDependencyGraph()
        {
            @Override
            public List<MavenProject> getAllProjects()
            {
                return sorted;
            }

            @Override
            public List<MavenProject> getSortedProjects()
            {
                return sorted;
            }

            @Override
            public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive)
            {
                return new ArrayList<MavenProject>();
            }

            @Override
            public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive)
            {
                return (project == downstream) ? upstreams : new ArrayList<MavenProject>();
            }
        };
    }

    private static Event event(ExecutionEvent.Type type)
    {
        return new Event(type);
    }

    private static Event mojoEvent(ExecutionEvent.Type type)
    {
        return event(type)
            .on("org.example", "core", "Core")
            .running("maven-compiler-plugin", "compile", EXECUTION_ID);
    }

    private static final class Event
        implements ExecutionEvent
    {
        private final Type type;

        private MavenProject project;

        private MojoExecution mojoExecution;

        private Exception exception;

        Event(Type type)
        {
            this.type = type;
        }

        Event on(String groupId, String artifactId, String name, Plugin... plugins)
        {
            project = project(groupId, artifactId, name, plugins);

            return this;
        }

        Event running(String pluginArtifactId, String goal, String executionId)
        {
            Plugin plugin = new Plugin();

            plugin.setArtifactId(pluginArtifactId);

            mojoExecution = new MojoExecution(plugin, goal, executionId);

            return this;
        }

        Event inPhase(String phase)
        {
            mojoExecution.setLifecyclePhase(phase);

            return this;
        }

        Event failing(Exception failure)
        {
            exception = failure;

            return this;
        }

        @Override
        public Type getType()
        {
            return type;
        }

        @Override
        public MavenSession getSession()
        {
            return null;
        }

        @Override
        public MavenProject getProject()
        {
            return project;
        }

        @Override
        public MojoExecution getMojoExecution()
        {
            return mojoExecution;
        }

        @Override
        public Exception getException()
        {
            return exception;
        }
    }

    private static final class RecordingSink
        implements LineSink
    {
        private final List<String> lines = new ArrayList<String>();

        @Override
        public void send(String line)
        {
            lines.add(line);
        }

        String firstOf(String type)
        {
            List<String> matching = allOf(type);

            return matching.isEmpty() ? null : matching.get(0);
        }

        List<String> allOf(String type)
        {
            List<String> matching = new ArrayList<String>();

            for (String line : lines)
            {
                if (type.equals(SpyProtocol.decode(line)[0]))
                {
                    matching.add(line);
                }
            }

            return matching;
        }
    }

    private static final class SteppedClock
        implements LongSupplier
    {
        private long millis;

        void set(long value)
        {
            millis = value;
        }

        @Override
        public long getAsLong()
        {
            return millis;
        }
    }
}
