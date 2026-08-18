package org.bitstrings.idea.plugins.mavenprime.build;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.BuildFinished;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleResult;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.MojoStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Problem;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Severity;
import org.junit.Test;

public class SpyEventsRecordedSessionTest
{
    private static final String MODULE = "org.example.spycheck:core";

    private static final String SOURCE = "/reactor/core/src/main/java/org/example/Broken.java";

    private static final String COMPILE_FAILURE =
        "Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile"
            + " (default-compile) on project core: Compilation failure";

    private static final List<String> SUCCESSFUL_SESSION =
        List.of(
            "SESSION_STARTED\t3",
            "PROJECT_STARTED\torg.example.spycheck:reactor\tSpy Check Reactor",
            "PROJECT_SUCCEEDED\torg.example.spycheck:reactor",
            "PROJECT_STARTED\torg.example.spycheck:core\tSpy Check Core",
            "PROJECT_SUCCEEDED\torg.example.spycheck:core",
            "PROJECT_STARTED\torg.example.spycheck:app\tSpy Check App",
            "PROJECT_SUCCEEDED\torg.example.spycheck:app",
            "SESSION_ENDED\t");

    private static final List<String> FAILING_SESSION =
        List.of(
            "SESSION_STARTED\t3",
            "PROJECT_STARTED\torg.example.spycheck:reactor\tSpy Check Reactor",
            "PROJECT_SUCCEEDED\torg.example.spycheck:reactor",
            "PROJECT_STARTED\torg.example.spycheck:core\tSpy Check Core",
            "MOJO_STARTED\t" + MODULE + "\tmaven-resources-plugin:resources\tdefault-resources",
            "MOJO_STARTED\t" + MODULE + "\tmaven-compiler-plugin:compile\tdefault-compile",
            "MOJO_FAILED\t" + MODULE + "\tmaven-compiler-plugin:compile\t" + COMPILE_FAILURE,
            "MOJO_PROBLEM\t" + MODULE + '\t' + SOURCE
                + ":[7,21] incompatible types: java.lang.String cannot be converted to int",
            "MOJO_PROBLEM\t" + MODULE + '\t' + SOURCE + ":[12,9] cannot find symbol",
            "MOJO_PROBLEM\t" + MODULE + "\tsymbol:   method undefinedMethod()",
            "MOJO_PROBLEM\t" + MODULE + "\tlocation: class org.example.Broken",
            "PROJECT_FAILED\t" + MODULE + '\t' + COMPILE_FAILURE,
            "SESSION_ENDED\t" + COMPILE_FAILURE);

    @Test
    public void parse_recordedSuccessfulReactorSession_yieldsAResultForEveryModule()
    {
        assertEquals(
            List.of(
                new ModuleStarted("org.example.spycheck", "reactor"),
                new ModuleResult("org.example.spycheck:reactor", true, false, ""),
                new ModuleStarted("org.example.spycheck", "core"),
                new ModuleResult("org.example.spycheck:core", true, false, ""),
                new ModuleStarted("org.example.spycheck", "app"),
                new ModuleResult("org.example.spycheck:app", true, false, ""),
                new BuildFinished(true)),
            parseAll(SUCCESSFUL_SESSION));
    }

    @Test
    public void parse_recordedCompilerFailureSession_yieldsThePositionedDiagnostics()
    {
        assertEquals(
            List.of(
                new ModuleStarted("org.example.spycheck", "reactor"),
                new ModuleResult("org.example.spycheck:reactor", true, false, ""),
                new ModuleStarted("org.example.spycheck", "core"),
                new MojoStarted("maven-resources-plugin:resources", "default-resources", MODULE),
                new MojoStarted("maven-compiler-plugin:compile", "default-compile", MODULE),
                Problem.withoutPosition(Severity.ERROR, COMPILE_FAILURE),
                new Problem(
                    Severity.ERROR,
                    "incompatible types: java.lang.String cannot be converted to int",
                    SOURCE,
                    7,
                    21),
                new Problem(Severity.ERROR, "cannot find symbol", SOURCE, 12, 9),
                Problem.withoutPosition(Severity.ERROR, "symbol:   method undefinedMethod()"),
                Problem.withoutPosition(Severity.ERROR, "location: class org.example.Broken"),
                new ModuleResult(MODULE, false, false, COMPILE_FAILURE),
                new BuildFinished(false)),
            parseAll(FAILING_SESSION));
    }

    private static List<MavenLogEvent> parseAll(List<String> lines)
    {
        List<MavenLogEvent> events = new ArrayList<>(lines.size());

        for (String line : lines)
        {
            SpyEvents.parse(line).ifPresent(events::add);
        }

        return events;
    }
}
