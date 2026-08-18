package org.bitstrings.idea.plugins.mavenprime.ui;

import java.awt.Component;

import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringUtils;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;

public class PropertiesTableModelPlatformTest
    extends BasePlatformTestCase
{
    private static final String KEY = "maven.test.skip";

    private static final String VALUE = "true";

    public void testGetRenderer_aPropertyWithAValue_paintsTheValueBesideItsKey()
    {
        assertEquals(
            "a value in a column of its own leaves the user pairing it back to its key by eye",
            KEY + " = " + VALUE,
            paintedTextOf(KEY, VALUE));
    }

    public void testGetRenderer_aPropertyLeftDeliberatelyValueless_paintsNoDanglingSeparator()
    {
        assertEquals(
            "a separator with nothing after it hides that the bare -Dkey form was meant",
            KEY,
            paintedTextOf(KEY, StringUtils.EMPTY));
    }

    public void testGetColumnCount_theModel_holdsTheKeyAndValueInOneColumn()
    {
        assertEquals(
            "a second column reintroduces the pairing the single row was meant to remove",
            1,
            new PropertiesTableModel().getColumnCount());
    }

    public void testGetValueAt_theCellCopiedToTheClipboard_carriesTheKeyAndValueAsText()
    {
        PropertiesTableModel model = new PropertiesTableModel();

        model.addProperty(KEY, VALUE);

        assertEquals(
            "the cell value is what Ctrl+C and a screen reader read, so an entry with no text form "
                + "hands them its identity hash",
            KEY + " = " + VALUE,
            String.valueOf(model.getValueAt(0, 0)));
    }

    public void testGetValueAt_aPropertyLeftDeliberatelyValueless_carriesTheBareKey()
    {
        PropertiesTableModel model = new PropertiesTableModel();

        model.addProperty(KEY, StringUtils.EMPTY);

        assertEquals(
            "a trailing separator in the clipboard reads as a value the user never set",
            KEY,
            String.valueOf(model.getValueAt(0, 0)));
    }

    private static String paintedTextOf(String key, String value)
    {
        PropertiesTableModel model = new PropertiesTableModel();

        model.addProperty(key, value);

        @SuppressWarnings("unchecked")
        ColumnInfo<PropertyEntry, PropertyEntry> info =
            (ColumnInfo<PropertyEntry, PropertyEntry>) model.getColumnInfos()[0];

        PropertyEntry entry = model.getItem(0);

        TableCellRenderer renderer = info.getRenderer(entry);

        assertNotNull("the column paints with the table default, which shows the entry's toString", renderer);

        Component painted =
            renderer.getTableCellRendererComponent(new JBTable(model), entry, false, false, 0, 0);

        return ((SimpleColoredComponent) painted).getCharSequence(false).toString();
    }
}
