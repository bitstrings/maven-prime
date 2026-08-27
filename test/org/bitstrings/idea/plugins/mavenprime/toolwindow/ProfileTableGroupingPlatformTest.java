package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileGroup;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileGrouping;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileView;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.SimpleColoredComponent;

public class ProfileTableGroupingPlatformTest
    extends BasePlatformTestCase
{
    private static final String CORE = "org.example:core";

    private static final String APP = "org.example:app";

    private static final String SUREFIRE = "maven-surefire-plugin:test";

    private static final int MODULE_COLUMNS = 6;

    private static final int GROUPED_COLUMNS = 5;

    private static final int DURATION_COLUMN = 2;

    private static final int PAYOFF_COLUMN = 5;

    private static final int TABLE_WIDTH = 900;

    private static final int TABLE_HEIGHT = 200;

    public void testSetProfile_groupedByModule_offersThePayoffColumn()
    {
        assertEquals(MODULE_COLUMNS, tableShowing(ProfileGrouping.MODULE).getColumnModel().getColumnCount());
    }

    public void testSetProfile_groupedByPlugin_dropsThePayoffColumn()
    {
        assertEquals(
            "a payoff belongs to a reactor node, so under any other grouping the column can only be "
                + "blank down its whole length",
            GROUPED_COLUMNS,
            tableShowing(ProfileGrouping.PLUGIN).getColumnModel().getColumnCount());
    }

    public void testSetProfile_groupedByPluginThenBackByModule_bringsThePayoffColumnBack()
    {
        ProfileTable table = tableShowing(ProfileGrouping.PLUGIN);

        show(table, ProfileGrouping.MODULE);

        assertEquals(MODULE_COLUMNS, table.getColumnModel().getColumnCount());
    }

    public void testSetProfile_groupedByPlugin_putsOnePluginAtTheTopLevelRatherThanOneModule()
    {
        assertEquals("maven-surefire-plugin", groupNameAt(tableShowing(ProfileGrouping.PLUGIN), 0));
    }

    public void testSetProfile_groupedByPlugin_gathersTheModulesThatRanItUnderOneRow()
    {
        ProfileTable table = tableShowing(ProfileGrouping.PLUGIN);

        assertEquals(2, table.getTree().getModel().getChildCount(rowAt(table, 0)));
    }

    public void testSetProfile_groupedByModule_stillLeadsWithTheLongestModule()
    {
        assertEquals(CORE, groupNameAt(tableShowing(ProfileGrouping.MODULE), 0));
    }

    public void testRenderer_aGroupWithNoNameOfItsOwn_labelsItRatherThanLeavingTheCellEmpty()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming(CORE, 0L, 100L));
        profile.accept(new MojoTiming(CORE, SUREFIRE, "cli", 0L, 100L));

        ProfileTable table = new ProfileTable();

        show(table, profile, ProfileGrouping.PHASE);

        assertEquals(
            "a goal run straight from the command line is bound to no phase, and a blank row reads as "
                + "a rendering fault rather than as an answer",
            MavenPrimeBundle.message("mavenprime.profile.group.none"),
            renderedNameAt(table, 0));
    }

    public void testRenderer_groupedByWorker_numbersTheThreadRatherThanPrintingItsId()
    {
        ProfileTable table = tableShowing(ProfileGrouping.WORKER);

        assertEquals(
            "Maven identifies a builder thread only by its id, and an id is not something a reader "
                + "can act on",
            MavenPrimeBundle.message("mavenprime.profile.worker", Integer.valueOf(1)),
            renderedNameAt(table, 0));
    }

    public void testRenderer_groupedByWorkerOnABuildThatRecordedNone_labelsTheRowRatherThanLeavingItBlank()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming(CORE, 0L, 100L));
        profile.accept(new MojoTiming(CORE, SUREFIRE, "default-test", 0L, 100L));

        ProfileTable table = new ProfileTable();

        show(table, profile, ProfileGrouping.WORKER);

        assertEquals(
            MavenPrimeBundle.message("mavenprime.profile.group.none"), renderedNameAt(table, 0));
    }

    public void testRenderer_aGoalUnderAPluginGroup_namesTheModuleItRanIn()
    {
        ProfileTable table = tableShowing(ProfileGrouping.PLUGIN);

        table.getTree().expandRow(0);

        assertTrue(
            "under any grouping but by module, the goal is the row's parent and the module is the only "
                + "thing that tells the rows apart",
            renderedNameAt(table, 1).startsWith(APP) || renderedNameAt(table, 1).startsWith(CORE));
    }

    private static String renderedNameAt(ProfileTable table, int row)
    {
        JTree tree = table.getTree();

        Component rendered =
            tree
                .getCellRenderer()
                .getTreeCellRendererComponent(tree, rowAt(table, row), false, false, true, row, false);

        return ((SimpleColoredComponent) rendered).getCharSequence(false).toString();
    }

    public void testHeader_theDurationColumn_explainsItselfWhereTheReaderPointsAtIt()
    {
        assertEquals(
            "Duration means the module's own span under one grouping and a sum under another, which "
                + "no column header can carry on its own",
            MavenPrimeBundle.message("mavenprime.profile.help.column.duration"),
            headerTooltipAt(tableShowing(ProfileGrouping.MODULE), DURATION_COLUMN));
    }

    public void testHeader_thePayoffColumn_explainsWhatItWouldSave()
    {
        assertEquals(
            MavenPrimeBundle.message("mavenprime.profile.help.column.payoff"),
            headerTooltipAt(tableShowing(ProfileGrouping.MODULE), PAYOFF_COLUMN));
    }

    private static String headerTooltipAt(ProfileTable table, int column)
    {
        table.setSize(TABLE_WIDTH, TABLE_HEIGHT);
        table.doLayout();

        JTableHeader header = table.getTableHeader();
        Rectangle bounds = header.getHeaderRect(column);

        return header.getToolTipText(
            new MouseEvent(
                header,
                MouseEvent.MOUSE_MOVED,
                0L,
                0,
                bounds.x + (bounds.width / 2),
                bounds.height / 2,
                0,
                false));
    }

    private ProfileTable tableShowing(ProfileGrouping grouping)
    {
        ProfileTable table = new ProfileTable();

        show(table, build(), grouping);

        return table;
    }

    private static void show(ProfileTable table, ProfileGrouping grouping)
    {
        show(table, build(), grouping);
    }

    private static void show(ProfileTable table, BuildProfile profile, ProfileGrouping grouping)
    {
        table.setProfile(profile, profile.summarize(), ProfileView.defaults().withGrouping(grouping));
    }

    private static String groupNameAt(ProfileTable table, int row)
    {
        return ((ProfileGroup) rowAt(table, row).getUserObject()).name();
    }

    private static DefaultMutableTreeNode rowAt(ProfileTable table, int row)
    {
        return (DefaultMutableTreeNode) table.getTree().getPathForRow(row).getLastPathComponent();
    }

    private static BuildProfile build()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming(CORE, 0L, 500L, "w-1"));
        profile.accept(new ModuleTiming(APP, 500L, 200L, "w-2"));
        profile.accept(new MojoTiming(CORE, SUREFIRE, "default-test", 0L, 200L, "test", "w-1"));
        profile.accept(new MojoTiming(APP, SUREFIRE, "default-test", 500L, 100L, "test", "w-2"));

        return profile;
    }
}
