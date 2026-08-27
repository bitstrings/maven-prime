package org.bitstrings.idea.plugins.mavenprime;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.FailureBehavior;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.OutputLevel;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileFacts;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileGrouping;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileSort;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileView;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.DownloadTable;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.ProfileTable;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.RepositorySource;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.ToolWindowTab;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileState;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.util.ui.ColumnInfo;

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

        for (ProfileGrouping grouping : ProfileGrouping.values())
        {
            collect(unresolved, grouping.getTitle(), grouping.getColumnTitle());
        }

        for (ProfileSort sort : ProfileSort.values())
        {
            collect(unresolved, sort.getTitle());
        }

        for (ProfileFacts.Measure measure : ProfileFacts.Measure.values())
        {
            collect(
                unresolved,
                MavenPrimeBundle.message(measure.getBundleKey()),
                MavenPrimeBundle.message(measure.getHelpKey()));
        }

        assertEquals(
            "a constant added or renamed without its bundle key paints the raw key in the UI, and no "
                + "compiler sees it because the key is composed at run time",
            List.of(),
            unresolved);
    }

    public void testGetName_everyColumnHeaderAndItsExplanation_resolveInTheBundle()
    {
        List<String> unresolved = new ArrayList<>();

        collectColumns(unresolved, new DownloadTable().getListTableModel().getColumnInfos());
        collectColumns(unresolved, columnsOf(ProfileGrouping.MODULE));
        collectColumns(unresolved, columnsOf(ProfileGrouping.PLUGIN));

        assertEquals(
            "a column header and its tooltip are composed at run time from a key no compiler sees, so "
                + "a renamed or missing key paints the raw key into the table",
            List.of(),
            unresolved);
    }

    private static ColumnInfo<?, ?>[] columnsOf(ProfileGrouping grouping)
    {
        ProfileTable table = new ProfileTable();
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("org.example:core", 0L, 10L));
        profile.accept(new MojoTiming("org.example:core", "a:b", "default", 0L, 10L));

        table.setProfile(profile, profile.summarize(), ProfileView.defaults().withGrouping(grouping));

        return ((ListTreeTableModelOnColumns) table.getTableModel()).getColumnInfos();
    }

    private static void collectColumns(List<String> unresolved, ColumnInfo<?, ?>[] columns)
    {
        for (ColumnInfo<?, ?> column : columns)
        {
            collect(unresolved, column.getName());

            if (column.getTooltipText() != null)
            {
                collect(unresolved, column.getTooltipText());
            }
        }
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
