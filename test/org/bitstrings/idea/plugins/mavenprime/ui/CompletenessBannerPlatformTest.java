package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CompletenessBannerPlatformTest
    extends BasePlatformTestCase
{
    public void testMessageOf_aBuildThatDroppedEvents_namesHowManyWereLost()
    {
        assertTrue(CompletenessBanner.messageOf(dropped(17L)).contains("17"));
    }

    public void testMessageOf_aBuildThatBothDroppedAndEndedEarly_stillNamesHowManyWereLost()
    {
        assertTrue(CompletenessBanner.messageOf(droppedAndTruncated(23L)).contains("23"));
    }

    public void testMessageOf_theThreeWaysDataGoesMissing_eachGetTheirOwnMessage()
    {
        Set<String> messages =
            Set.of(
                CompletenessBanner.messageOf(dropped(5L)),
                CompletenessBanner.messageOf(truncated()),
                CompletenessBanner.messageOf(droppedAndTruncated(5L)));

        assertEquals("each cause of missing data has to read differently", 3, messages.size());
    }

    public void testMessageOf_everyIncompleteCause_resolvesItsBundleText()
    {
        assertResolved(CompletenessBanner.messageOf(dropped(5L)));
        assertResolved(CompletenessBanner.messageOf(truncated()));
        assertResolved(CompletenessBanner.messageOf(droppedAndTruncated(5L)));
    }

    private static BuildCompleteness dropped(long events)
    {
        BuildCompleteness completeness = new BuildCompleteness();

        completeness.recordDropped(events);

        return completeness;
    }

    private static BuildCompleteness truncated()
    {
        BuildCompleteness completeness = new BuildCompleteness();

        completeness.recordTruncated();

        return completeness;
    }

    private static BuildCompleteness droppedAndTruncated(long events)
    {
        BuildCompleteness completeness = dropped(events);

        completeness.recordTruncated();

        return completeness;
    }

    private static void assertResolved(String message)
    {
        assertNotNull(message);
        assertFalse("unresolved bundle key: " + message, message.startsWith("!") && message.endsWith("!"));
    }
}
