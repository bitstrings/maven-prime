package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import javax.swing.table.TableColumn;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.util.ui.UIUtil;

public class ModelProvenancePanelPlatformTest
    extends BasePlatformTestCase
{
    private static final String WIDTH_PREFIX = "mavenPrime.model.columnWidth.";

    private static final int COLUMNS = 3;

    private static final int VIEWPORT_WIDTH = 1000;

    private static final int NAME_WIDTH = 600;

    private static final int RESTORED_NAME_WIDTH = 432;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        for (int column = 0; column < COLUMNS; column++)
        {
            PropertiesComponent.getInstance(getProject()).unsetValue(WIDTH_PREFIX + column);
        }
    }

    public void testConfigureTable_theModelTable_leavesRoomForAHorizontalScrollbar()
    {
        assertFalse(
            "a table that tracks the viewport width is squeezed into it, so no horizontal scrollbar can ever "
                + "appear and a long Declared In value stays truncated with no way to read it",
            tableOf().getScrollableTracksViewportWidth());
    }

    public void testSizeColumnsToViewport_theFirstLayout_givesTheEffectiveModelColumnMostOfTheWidth()
    {
        ModelProvenancePanel panel = panel();

        panel.sizeColumnsToViewport(VIEWPORT_WIDTH);

        assertEquals(
            "the names are the longest values in the table, so a narrow first column is the one the user "
                + "would have to widen by hand every session",
            NAME_WIDTH,
            columnOf(panel, 0).getPreferredWidth());
    }

    public void testSizeColumnsToViewport_aLaterLayout_keepsTheWidthsAlreadyOnScreen()
    {
        ModelProvenancePanel panel = panel();

        panel.sizeColumnsToViewport(VIEWPORT_WIDTH);
        panel.sizeColumnsToViewport(VIEWPORT_WIDTH * 2);

        assertEquals(
            "redistributing on every resize would undo the widths the user set, every time the tool window "
                + "changes size",
            NAME_WIDTH,
            columnOf(panel, 0).getPreferredWidth());
    }

    public void testConfigureTable_widthsFromAnEarlierSession_areRestored()
    {
        storeWidths();

        assertEquals(
            "a column width the user set by hand is worth nothing if it dies with the session",
            RESTORED_NAME_WIDTH,
            columnOf(panel(), 0).getPreferredWidth());
    }

    public void testSizeColumnsToViewport_widthsFromAnEarlierSession_surviveTheFirstLayout()
    {
        storeWidths();

        ModelProvenancePanel panel = panel();

        panel.sizeColumnsToViewport(VIEWPORT_WIDTH);

        assertEquals(
            "the proportional default is for a table that has never been sized, so applying it over restored "
                + "widths discards them on the first paint",
            RESTORED_NAME_WIDTH,
            columnOf(panel, 0).getPreferredWidth());
    }

    public void testColumnChanged_aColumnTheUserResized_isWrittenToTheProjectState()
    {
        ModelProvenancePanel panel = panel();

        panel.sizeColumnsToViewport(VIEWPORT_WIDTH);
        columnOf(panel, 0).setWidth(RESTORED_NAME_WIDTH);

        assertEquals(
            "nothing else writes the width, so a drag that is not recorded here is a drag the next session "
                + "forgets",
            RESTORED_NAME_WIDTH,
            PropertiesComponent.getInstance(getProject()).getInt(WIDTH_PREFIX + 0, 0));
    }

    private void storeWidths()
    {
        PropertiesComponent.getInstance(getProject()).setValue(WIDTH_PREFIX + 0, RESTORED_NAME_WIDTH, 0);

        for (int column = 1; column < COLUMNS; column++)
        {
            PropertiesComponent.getInstance(getProject()).setValue(WIDTH_PREFIX + column, NAME_WIDTH, 0);
        }
    }

    private TreeTableView tableOf()
    {
        return tableOf(panel());
    }

    private static TableColumn columnOf(ModelProvenancePanel panel, int column)
    {
        return tableOf(panel).getColumnModel().getColumn(column);
    }

    private static TreeTableView tableOf(ModelProvenancePanel panel)
    {
        return UIUtil.findComponentOfType(panel, TreeTableView.class);
    }

    private ModelProvenancePanel panel()
    {
        ModelProvenancePanel panel = new ModelProvenancePanel(getProject(), () -> null);

        Disposer.register(getTestRootDisposable(), panel);

        return panel;
    }
}
