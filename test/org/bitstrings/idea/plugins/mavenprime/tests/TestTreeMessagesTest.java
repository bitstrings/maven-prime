package org.bitstrings.idea.plugins.mavenprime.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class TestTreeMessagesTest
{
    private static final String CLASS_NAME = "org.example.CartTest";

    private static final TestCaseResult PASSED = TestCaseResult.passed(CLASS_NAME, "addsAnItem", 250L);

    private static final TestCaseResult FAILED =
        new TestCaseResult(
            CLASS_NAME, "rejects", 500L, TestCaseStatus.FAILED, "expected 2 but was 3", "at CartTest.java:42");

    private static final TestCaseResult SKIPPED =
        new TestCaseResult(CLASS_NAME, "pending", 0L, TestCaseStatus.SKIPPED, "not implemented", "");

    @Test
    public void of_aPassingCase_opensAndClosesItsSuiteAroundIt()
    {
        List<String> messages = TestTreeMessages.of(List.of(PASSED));

        assertEquals(4, messages.size());
        assertTrue(messages.get(0), messages.get(0).contains("testSuiteStarted"));
        assertTrue(messages.get(1), messages.get(1).contains("testStarted"));
        assertTrue(messages.get(2), messages.get(2).contains("testFinished"));
        assertTrue(messages.get(3), messages.get(3).contains("testSuiteFinished"));
    }

    @Test
    public void of_aPassingCase_carriesALocationTheTreeCanNavigateTo()
    {
        assertTrue(
            "without a location hint the tree shows a name that leads nowhere",
            TestTreeMessages
                .of(List.of(PASSED))
                .get(1)
                .contains("java:test://org.example.CartTest/addsAnItem"));
    }

    @Test
    public void of_aFailingCase_reportsTheFailureBeforeFinishingTheTest()
    {
        List<String> messages = TestTreeMessages.of(List.of(FAILED));

        assertTrue(messages.get(2), messages.get(2).contains("testFailed"));
        assertTrue(messages.get(2), messages.get(2).contains("expected 2 but was 3"));
        assertTrue(messages.get(3), messages.get(3).contains("testFinished"));
    }

    @Test
    public void of_aSkippedCase_reportsItAsIgnoredRatherThanPassing()
    {
        assertTrue(
            "a skipped test counted as passing hides that it never ran",
            TestTreeMessages.of(List.of(SKIPPED)).get(2).contains("testIgnored"));
    }

    @Test
    public void of_casesFromTwoClasses_groupsEachClassIntoItsOwnSuite()
    {
        List<String> messages =
            TestTreeMessages.of(
                List.of(PASSED, TestCaseResult.passed("org.example.OrderTest", "totals", 10L)));

        assertEquals(
            "two suites, each opened and closed, or the tree nests one class inside the other",
            2,
            messages.stream().filter(message -> message.contains("testSuiteStarted")).count());
    }

    @Test
    public void of_aPassingCase_reportsItsDurationSoTheTreeCanShowTiming()
    {
        assertTrue(
            "a tree without timings cannot point at the slow test",
            TestTreeMessages.of(List.of(PASSED)).get(2).contains("duration='250'"));
    }
}
