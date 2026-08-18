package org.bitstrings.idea.plugins.mavenprime.tests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.intellij.execution.testframework.sm.ServiceMessageBuilder;

public final class TestTreeMessages
{
    private static final String LOCATION_HINT = "locationHint";

    private static final String LOCATION_PREFIX = "java:test://";

    private static final String DURATION = "duration";

    private static final String MESSAGE = "message";

    private static final String DETAILS = "details";

    private static final String FRAMEWORK_STARTED = "enteredTheMatrix";

    private TestTreeMessages()
    {
    }

    public static String frameworkStarted()
    {
        return new ServiceMessageBuilder(FRAMEWORK_STARTED).toString();
    }

    public static List<String> of(List<TestCaseResult> results)
    {
        List<String> messages = new ArrayList<>();

        for (Map.Entry<String, List<TestCaseResult>> suite : bySuite(results).entrySet())
        {
            messages.add(
                ServiceMessageBuilder
                    .testSuiteStarted(suite.getKey())
                    .addAttribute(LOCATION_HINT, LOCATION_PREFIX + suite.getKey())
                    .toString());

            for (TestCaseResult result : suite.getValue())
            {
                messages.addAll(of(result));
            }

            messages.add(ServiceMessageBuilder.testSuiteFinished(suite.getKey()).toString());
        }

        return messages;
    }

    private static List<String> of(TestCaseResult result)
    {
        List<String> messages = new ArrayList<>();

        messages.add(
            ServiceMessageBuilder
                .testStarted(result.name())
                .addAttribute(LOCATION_HINT, LOCATION_PREFIX + result.className() + '/' + result.name())
                .toString());

        if (result.status() == TestCaseStatus.FAILED)
        {
            messages.add(
                ServiceMessageBuilder
                    .testFailed(result.name())
                    .addAttribute(MESSAGE, StringUtils.defaultString(result.message()))
                    .addAttribute(DETAILS, StringUtils.defaultString(result.details()))
                    .toString());
        }
        else if (result.status() == TestCaseStatus.SKIPPED)
        {
            messages.add(
                ServiceMessageBuilder
                    .testIgnored(result.name())
                    .addAttribute(MESSAGE, StringUtils.defaultString(result.message()))
                    .toString());
        }

        messages.add(
            ServiceMessageBuilder
                .testFinished(result.name())
                .addAttribute(DURATION, String.valueOf(result.durationMillis()))
                .toString());

        return messages;
    }

    private static Map<String, List<TestCaseResult>> bySuite(List<TestCaseResult> results)
    {
        Map<String, List<TestCaseResult>> bySuite = new LinkedHashMap<>();

        for (TestCaseResult result : results)
        {
            bySuite.computeIfAbsent(result.className(), suite -> new ArrayList<>()).add(result);
        }

        return bySuite;
    }
}
