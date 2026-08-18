package org.bitstrings.idea.plugins.mavenprime;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.FailureBehavior;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.OutputLevel;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.RepositorySource;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.ToolWindowTab;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileState;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BundleKeysPlatformTest
    extends BasePlatformTestCase
{
    private static final String UNRESOLVED_MARKER = "!";

    public void testGetTitle_everyKeyComposedFromAnEnumConstant_resolvesInTheBundle()
    {
        List<String> unresolved = new ArrayList<>();

        for (MavenFlag flag : MavenFlag.values())
        {
            collect(unresolved, flag.getTitle(), flag.getDescription());
        }

        for (OutputLevel level : OutputLevel.values())
        {
            collect(unresolved, level.getTitle());
        }

        for (FailureBehavior behavior : FailureBehavior.values())
        {
            collect(unresolved, behavior.getTitle());
        }

        for (ProfileState state : ProfileState.values())
        {
            collect(unresolved, state.getTitle());
        }

        for (ToolWindowTab tab : ToolWindowTab.values())
        {
            collect(unresolved, tab.getTitle());
        }

        for (RepositorySource source : RepositorySource.values())
        {
            collect(unresolved, source.getTitle());
        }

        assertEquals(
            "a constant added or renamed without its bundle key paints the raw key in the UI, and no "
                + "compiler sees it because the key is composed at run time",
            List.of(),
            unresolved);
    }

    private static void collect(List<String> unresolved, String... messages)
    {
        for (String message : messages)
        {
            if (isUnresolved(message))
            {
                unresolved.add(message);
            }
        }
    }

    private static boolean isUnresolved(String message)
    {
        return message.startsWith(UNRESOLVED_MARKER);
    }
}
