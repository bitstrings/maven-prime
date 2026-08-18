package org.bitstrings.idea.plugins.mavenprime.tests;

import org.apache.commons.lang3.StringUtils;

public record TestCaseResult(
    String className, String name, long durationMillis, TestCaseStatus status, String message, String details)
{
    public static TestCaseResult passed(String className, String name, long durationMillis)
    {
        return new TestCaseResult(
            className, name, durationMillis, TestCaseStatus.PASSED, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    public String qualifiedName()
    {
        return className + '.' + name;
    }
}
