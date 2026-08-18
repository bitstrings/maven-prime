package org.bitstrings.idea.plugins.mavenprime.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class SurefireReportTest
{
    private static final String CLASS_NAME = "org.example.CartTest";

    private static final String REPORT =
        "<testsuite name=\"org.example.CartTest\" tests=\"4\" time=\"1.5\">"
            + "<testcase name=\"addsAnItem\" classname=\"org.example.CartTest\" time=\"0.25\"/>"
            + "<testcase name=\"rejectsANegativeQuantity\" classname=\"org.example.CartTest\" time=\"0.5\">"
            + "<failure message=\"expected 2 but was 3\" type=\"java.lang.AssertionError\">"
            + "at org.example.CartTest.rejectsANegativeQuantity(CartTest.java:42)</failure>"
            + "</testcase>"
            + "<testcase name=\"talksToTheDatabase\" classname=\"org.example.CartTest\" time=\"0\">"
            + "<error message=\"connection refused\" type=\"java.net.ConnectException\">stack</error>"
            + "</testcase>"
            + "<testcase name=\"pendingFeature\" classname=\"org.example.CartTest\" time=\"0\">"
            + "<skipped message=\"not implemented\"/>"
            + "</testcase>"
            + "</testsuite>";

    @Test
    public void parse_aReportSurefireWrote_readsEveryCaseInOrder()
        throws IOException
    {
        assertEquals(
            "a dropped case is a test the tree silently forgets to show",
            List.of("addsAnItem", "rejectsANegativeQuantity", "talksToTheDatabase", "pendingFeature"),
            SurefireReport.parse(REPORT).stream().map(TestCaseResult::name).toList());
    }

    @Test
    public void parse_aCaseThatPassed_isReportedAsPassedWithItsDuration()
        throws IOException
    {
        TestCaseResult passed = SurefireReport.parse(REPORT).get(0);

        assertEquals(TestCaseStatus.PASSED, passed.status());
        assertEquals("surefire reports seconds and the tree shows milliseconds", 250L, passed.durationMillis());
        assertEquals(CLASS_NAME, passed.className());
    }

    @Test
    public void parse_aCaseThatFailedAnAssertion_carriesTheMessageAndTheStack()
        throws IOException
    {
        TestCaseResult failed = SurefireReport.parse(REPORT).get(1);

        assertEquals(TestCaseStatus.FAILED, failed.status());
        assertEquals("expected 2 but was 3", failed.message());
        assertTrue(
            "without the stack the tree cannot offer a link to the failing line",
            failed.details().contains("CartTest.java:42"));
    }

    @Test
    public void parse_aCaseThatThrew_isReportedAsFailedRatherThanIgnored()
        throws IOException
    {
        assertEquals(
            "surefire records an exception as <error>, and an error is still a failing test",
            TestCaseStatus.FAILED,
            SurefireReport.parse(REPORT).get(2).status());
    }

    @Test
    public void parse_aCaseThatWasSkipped_isReportedAsSkipped()
        throws IOException
    {
        assertEquals(TestCaseStatus.SKIPPED, SurefireReport.parse(REPORT).get(3).status());
    }

    @Test
    public void parse_aReportStillBeingWritten_refusesItInsteadOfReturningHalfTheCases()
    {
        try
        {
            SurefireReport.parse("<testsuite name=\"org.example.CartTest\"><testcase name=\"half");

            fail("a truncated report has to be retried, not accepted");
        }
        catch (IOException expected)
        {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not readable yet"));
        }
    }
}
