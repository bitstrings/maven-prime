package org.bitstrings.idea.plugins.mavenprime.build;

import java.nio.file.Path;

import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.BuildFinished;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleResult;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.MojoStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Problem;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Severity;
import org.bitstrings.idea.plugins.mavenprime.build.RecordingBuildProgress.Outcome;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenBuildOutputListenerPlatformTest
    extends BasePlatformTestCase
{
    private static final String MODULE = "org.example:core";

    private RecordingBuildProgress root;

    private MavenBuildOutputListener listener;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        root = new RecordingBuildProgress("build");
        listener =
            new MavenBuildOutputListener(
                getProject(),
                root,
                new MavenBuildProgressDescriptor("build", "build", "/work"),
                Path.of("/work"));
    }

    public void testAccept_secondModuleStartsWhileTheFirstIsBuilding_keepsBothOpen()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new ModuleStarted("org.example", "web"));

        assertEquals(Outcome.OPEN, root.getChild(0).getOutcome());
    }

    public void testAccept_mojoOfTheFirstModuleAfterASecondStarted_staysUnderItsOwnModule()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new ModuleStarted("org.example", "web"));
        listener.accept(new MojoStarted("maven-compiler-plugin:compile", "default-compile", MODULE));

        assertEquals(1, root.getChild(0).getChildren().size());
        assertEquals(0, root.getChild(1).getChildren().size());
    }

    public void testAccept_resultForTheFirstModuleWhileTheSecondRuns_closesOnlyThatModule()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new ModuleStarted("org.example", "web"));
        listener.accept(new ModuleResult(MODULE, true, false, null));

        assertEquals(Outcome.FINISHED, root.getChild(0).getOutcome());
        assertEquals(Outcome.OPEN, root.getChild(1).getOutcome());
    }

    public void testAccept_moduleStarted_opensAChildProgressNamedAfterTheModule()
    {
        listener.accept(new ModuleStarted("org.example", "core"));

        assertEquals(1, root.getChildren().size());
        assertEquals("core", root.getChild(0).getTitle());
    }

    public void testAccept_mojoStarted_opensAChildProgressUnderTheModule()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new MojoStarted("maven-compiler-plugin:compile", "default-compile", MODULE));

        assertEquals(
            "maven-compiler-plugin:compile (default-compile)",
            root.getChild(0).getChild(0).getTitle());
    }

    public void testAccept_mojoStartedWithoutAnExecutionId_namesTheNodeAfterTheGoalAlone()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new MojoStarted("maven-compiler-plugin:compile", "", MODULE));

        assertEquals("maven-compiler-plugin:compile", root.getChild(0).getChild(0).getTitle());
    }

    public void testAccept_errorProblem_failsTheMojoItBelongsTo()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new MojoStarted("maven-compiler-plugin:compile", "default-compile", MODULE));
        listener.accept(Problem.withoutPosition(Severity.ERROR, "Compilation failure"));
        listener.accept(new ModuleResult(MODULE, false, false, "Compilation failure"));

        assertEquals(Outcome.FAILED, root.getChild(0).getChild(0).getOutcome());
    }

    public void testAccept_errorProblem_failsTheModuleItBelongsTo()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new MojoStarted("maven-compiler-plugin:compile", "default-compile", MODULE));
        listener.accept(Problem.withoutPosition(Severity.ERROR, "Compilation failure"));
        listener.accept(new ModuleResult(MODULE, false, false, "Compilation failure"));

        assertEquals(Outcome.FAILED, root.getChild(0).getOutcome());
    }

    public void testAccept_warningProblem_leavesTheMojoSuccessful()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new MojoStarted("maven-compiler-plugin:compile", "default-compile", MODULE));
        listener.accept(Problem.withoutPosition(Severity.WARNING, "deprecated API"));
        listener.accept(new ModuleResult(MODULE, true, false, ""));

        assertEquals(Outcome.FINISHED, root.getChild(0).getChild(0).getOutcome());
    }

    public void testAccept_moduleSucceeded_finishesTheModuleProgress()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new ModuleResult(MODULE, true, false, ""));

        assertEquals(Outcome.FINISHED, root.getChild(0).getOutcome());
    }

    public void testAccept_nextModuleStarting_leavesThePreviousModuleOpenUntilItsOwnResult()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new ModuleStarted("org.example", "app"));

        assertEquals(Outcome.OPEN, root.getChild(0).getOutcome());
    }

    public void testAccept_moduleFailed_reportsTheCauseOnTheBuildRoot()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new ModuleResult(MODULE, false, false, "Compilation failure"));

        assertTrue(
            root.getMessages().stream().anyMatch(message -> message.contains("Compilation failure")));
    }

    public void testAccept_problemBeforeAnyModuleStarts_isReportedOnTheBuildRoot()
    {
        listener.accept(Problem.withoutPosition(Severity.ERROR, "Cannot resolve settings"));

        assertTrue(
            root.getMessages().stream().anyMatch(message -> message.contains("Cannot resolve settings")));
    }

    public void testAccept_problemCarryingAPosition_isStillReported()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.accept(new MojoStarted("maven-compiler-plugin:compile", "default-compile", MODULE));
        listener.accept(new Problem(Severity.ERROR, "cannot find symbol", "/work/Broken.java", 12, 9));

        assertTrue(
            root.getChild(0)
                .getChild(0)
                .getMessages()
                .stream()
                .anyMatch(message -> message.contains("cannot find symbol")));
    }

    public void testFinish_successfulSessionAndZeroExitCode_finishesTheBuild()
    {
        listener.accept(new BuildFinished(true));
        listener.finish(0);

        assertEquals(Outcome.FINISHED, root.getOutcome());
    }

    public void testFinish_failedSessionDespiteZeroExitCode_failsTheBuild()
    {
        listener.accept(new BuildFinished(false));
        listener.finish(0);

        assertEquals(Outcome.FAILED, root.getOutcome());
    }

    public void testFinish_nonZeroExitCode_failsTheBuild()
    {
        listener.finish(1);

        assertEquals(Outcome.FAILED, root.getOutcome());
    }

    public void testFinish_moduleStillOpen_closesItBeforeFinishingTheBuild()
    {
        listener.accept(new ModuleStarted("org.example", "core"));
        listener.finish(0);

        assertEquals(Outcome.FINISHED, root.getChild(0).getOutcome());
    }
}
