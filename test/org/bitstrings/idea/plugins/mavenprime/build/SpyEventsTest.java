package org.bitstrings.idea.plugins.mavenprime.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.BuildFinished;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleResult;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.MojoStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Problem;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Severity;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;
import org.junit.Test;

public class SpyEventsTest
{
    @Test
    public void parse_projectStarted_splitsTheCoordinatesIntoGroupAndArtifact()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:my-module", "My Module"))
                .orElseThrow();

        assertEquals(new ModuleStarted("org.example", "my-module"), event);
    }

    @Test
    public void parse_projectSucceeded_yieldsASuccessfulResult()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(SpyProtocol.encode(SpyProtocol.PROJECT_SUCCEEDED, "org.example:my-module"))
                .orElseThrow();

        assertEquals(new ModuleResult("org.example:my-module", true, false, ""), event);
    }

    @Test
    public void parse_projectSkipped_marksTheResultSkipped()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(SpyProtocol.encode(SpyProtocol.PROJECT_SKIPPED, "org.example:my-module"))
                .orElseThrow();

        assertEquals(new ModuleResult("org.example:my-module", false, true, ""), event);
    }

    @Test
    public void parse_mojoStarted_usesTheGoalAsTheCoordinates()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MOJO_STARTED, "org.example:my-module", "maven-compiler-plugin:compile"))
                .orElseThrow();

        assertEquals(new MojoStarted("maven-compiler-plugin:compile", "", "org.example:my-module"), event);
    }

    @Test
    public void parse_mojoFailed_reportsTheFailureAsAnError()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MOJO_FAILED,
                        "org.example:my-module",
                        "maven-surefire-plugin:test",
                        "There are test failures"))
                .orElseThrow();

        assertEquals(Problem.withoutPosition(Severity.ERROR, "There are test failures"), event);
    }

    @Test
    public void parse_sessionEndedWithoutAFailure_reportsASuccessfulBuild()
    {
        MavenLogEvent event =
            SpyEvents.parse(SpyProtocol.encode(SpyProtocol.SESSION_ENDED, "")).orElseThrow();

        assertEquals(new BuildFinished(true), event);
    }

    @Test
    public void parse_sessionEndedWithAFailure_reportsAFailedBuild()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(SpyProtocol.encode(SpyProtocol.SESSION_ENDED, "Some modules failed"))
                .orElseThrow();

        assertEquals(new BuildFinished(false), event);
    }

    @Test
    public void parse_mojoProblemWithAPosition_readsFileLineAndColumn()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MOJO_PROBLEM,
                        "org.example:my-module",
                        "/src/Foo.java:[12,17] cannot find symbol"))
                .orElseThrow();

        assertEquals(
            new Problem(Severity.ERROR, "cannot find symbol", "/src/Foo.java", 12, 17), event);
    }

    @Test
    public void parse_mojoProblemDetailLine_isReportedWithoutAPosition()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MOJO_PROBLEM, "org.example:my-module", "symbol: method calcuate(int)"))
                .orElseThrow();

        assertEquals(
            Problem.withoutPosition(Severity.ERROR, "symbol: method calcuate(int)"), event);
    }

    @Test
    public void parse_mojoProblemLineNumberTooLargeForAnInt_isReportedWithoutAPosition()
    {
        MavenLogEvent event =
            SpyEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MOJO_PROBLEM, "org.example:my-module", "/src/Foo.java:[99999999999,1] boom"))
                .orElseThrow();

        assertEquals(
            Problem.withoutPosition(Severity.ERROR, "/src/Foo.java:[99999999999,1] boom"), event);
    }

    @Test
    public void parse_mojoProblemWithNoText_yieldsNothing()
    {
        assertTrue(
            SpyEvents.parse(SpyProtocol.encode(SpyProtocol.MOJO_PROBLEM, "org.example:my-module", "")).isEmpty());
    }

    @Test
    public void parse_sessionStarted_carriesNoBuildTreeEventOfItsOwn()
    {
        assertTrue(SpyEvents.parse(SpyProtocol.encode(SpyProtocol.SESSION_STARTED, "12")).isEmpty());
    }

    @Test
    public void parse_unknownEventName_isIgnoredRatherThanFailing()
    {
        assertTrue(SpyEvents.parse("EVENT_FROM_A_NEWER_SPY\tsomething").isEmpty());
    }

    @Test
    public void parse_emptyLine_isIgnored()
    {
        assertTrue(SpyEvents.parse("").isEmpty());
    }

    @Test
    public void encode_fieldContainingATab_doesNotBreakTheFraming()
    {
        String line = SpyProtocol.encode(SpyProtocol.PROJECT_FAILED, "org.example:mod", "boom\there\nand there");

        assertEquals(3, SpyProtocol.decode(line).length);
    }

    @Test
    public void droppedCount_theLossReportEndingABuild_yieldsHowManyEventsWereLost()
    {
        assertEquals(42L, SpyEvents.droppedCount(SpyProtocol.encode(SpyProtocol.DROPPED, "42")));
    }

    @Test
    public void droppedCount_anOrdinaryBuildEvent_reportsNoLoss()
    {
        assertEquals(0L, SpyEvents.droppedCount(SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:mod")));
    }

    @Test
    public void droppedCount_aLossReportCarryingGarbage_reportsNoLossRatherThanThrowing()
    {
        assertEquals(0L, SpyEvents.droppedCount(SpyProtocol.encode(SpyProtocol.DROPPED, "not-a-number")));
    }

    @Test
    public void droppedCount_aLossReportWithNoCount_reportsNoLoss()
    {
        assertEquals(0L, SpyEvents.droppedCount(SpyProtocol.DROPPED));
    }

    @Test
    public void droppedCount_anEmptyLine_reportsNoLoss()
    {
        assertEquals(0L, SpyEvents.droppedCount(""));
    }
}
