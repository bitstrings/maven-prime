package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;

public final class PropertiesTableModel
    extends ListTableModel<PropertyEntry>
{
    private static final long serialVersionUID = 1L;

    public PropertiesTableModel()
    {
        super(new EntryColumn());
    }

    public void setProperties(Map<String, String> properties)
    {
        List<PropertyEntry> entries = new ArrayList<>(properties.size());

        for (Map.Entry<String, String> property : properties.entrySet())
        {
            entries.add(new PropertyEntry(property.getKey(), property.getValue()));
        }

        setItems(entries);
    }

    public Map<String, String> getProperties()
    {
        Map<String, String> properties = new LinkedHashMap<>();

        for (PropertyEntry entry : getItems())
        {
            if (StringUtils.isNotBlank(entry.getKey()))
            {
                properties.put(entry.getKey().trim(), entry.getValue());
            }
        }

        return properties;
    }

    public void addProperty(String key, String value)
    {
        addRow(new PropertyEntry(key, value));
    }

    public void removeProperty(int row)
    {
        if ((row >= 0) && (row < getRowCount()))
        {
            removeRow(row);
        }
    }

    private static final class EntryRenderer
        extends ColoredTableCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void customizeCellRenderer(
            JTable table, Object value, boolean selected, boolean focused, int row, int column)
        {
            if (!(value instanceof PropertyEntry))
            {
                return;
            }

            PropertyEntry entry = (PropertyEntry) value;

            append(entry.getKey(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

            if (StringUtils.isNotBlank(entry.getValue()))
            {
                append(PropertyEntry.VALUE_SEPARATOR, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                append(entry.getValue(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }

    private static final class EntryColumn
        extends ColumnInfo<PropertyEntry, PropertyEntry>
    {
        private final TableCellRenderer renderer = new EntryRenderer();

        EntryColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.properties.column.key"));
        }

        @Override
        public TableCellRenderer getRenderer(PropertyEntry entry)
        {
            return renderer;
        }

        @Override
        public PropertyEntry valueOf(PropertyEntry entry)
        {
            return entry;
        }
    }
}
