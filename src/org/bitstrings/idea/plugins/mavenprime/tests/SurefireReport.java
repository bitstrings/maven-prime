package org.bitstrings.idea.plugins.mavenprime.tests;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

import com.intellij.openapi.util.JDOMUtil;

public final class SurefireReport
{
    private static final String TEST_CASE = "testcase";

    private static final String FAILURE = "failure";

    private static final String ERROR = "error";

    private static final String SKIPPED = "skipped";

    private static final String NAME = "name";

    private static final String CLASS_NAME = "classname";

    private static final String TIME = "time";

    private static final String MESSAGE = "message";

    private static final BigDecimal MILLIS_PER_SECOND = BigDecimal.valueOf(1000L);

    private SurefireReport()
    {
    }

    public static List<TestCaseResult> parse(String xml)
        throws IOException
    {
        try
        {
            return casesIn(JDOMUtil.load(xml));
        }
        catch (JDOMException malformed)
        {
            throw new IOException("the report is not readable yet", malformed);
        }
    }

    private static List<TestCaseResult> casesIn(Element suite)
    {
        List<TestCaseResult> cases = new ArrayList<>();

        for (Element testCase : suite.getChildren(TEST_CASE))
        {
            cases.add(resultOf(testCase));
        }

        return cases;
    }

    private static TestCaseResult resultOf(Element testCase)
    {
        String className = StringUtils.defaultString(testCase.getAttributeValue(CLASS_NAME));
        String name = StringUtils.defaultString(testCase.getAttributeValue(NAME));

        long duration = millisOf(testCase.getAttributeValue(TIME));

        Element skipped = testCase.getChild(SKIPPED);

        if (skipped != null)
        {
            return new TestCaseResult(
                className,
                name,
                duration,
                TestCaseStatus.SKIPPED,
                messageOf(skipped),
                StringUtils.EMPTY);
        }

        Element failure = (testCase.getChild(FAILURE) == null) ? testCase.getChild(ERROR) : testCase.getChild(FAILURE);

        if (failure == null)
        {
            return TestCaseResult.passed(className, name, duration);
        }

        return new TestCaseResult(
            className,
            name,
            duration,
            TestCaseStatus.FAILED,
            messageOf(failure),
            StringUtils.defaultString(failure.getText()).strip());
    }

    private static String messageOf(Element element)
    {
        return StringUtils.defaultString(element.getAttributeValue(MESSAGE));
    }

    private static long millisOf(String seconds)
    {
        if (StringUtils.isBlank(seconds))
        {
            return 0L;
        }

        try
        {
            return new BigDecimal(seconds.replace(",", StringUtils.EMPTY))
                .multiply(MILLIS_PER_SECOND)
                .longValue();
        }
        catch (NumberFormatException unreadable)
        {
            return 0L;
        }
    }
}
