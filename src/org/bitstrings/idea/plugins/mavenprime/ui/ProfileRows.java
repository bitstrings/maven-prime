package org.bitstrings.idea.plugins.mavenprime.ui;

import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.SimpleTextAttributes;

public final class ProfileRows
{
    private ProfileRows()
    {
    }

    public static ProfileEntry render(CheckboxTreeBase.CheckboxTreeCellRendererBase renderer, Object node)
    {
        ProfileEntry entry = ProfileEntries.in(node);

        if (entry == null)
        {
            return null;
        }

        renderer.myCheckbox.setVisible(true);
        renderer.myCheckbox.setOpaque(false);
        renderer.myCheckbox.setBackground(null);
        renderer.myCheckbox.setState(entry.getState().getCheckboxState());

        renderer.setBackground(null);

        renderer.getTextRenderer().append(entry.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

        return entry;
    }
}
