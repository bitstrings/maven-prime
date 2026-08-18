package org.bitstrings.idea.plugins.mavenprime.ui;

import javax.swing.JList;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

public final class ArtifactCoordinatesRenderer
    extends ColoredListCellRenderer<ArtifactCoordinates>
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void customizeCellRenderer(
        JList<? extends ArtifactCoordinates> list,
        ArtifactCoordinates value,
        int index,
        boolean selected,
        boolean hasFocus)
    {
        append(value.artifactId(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        append(':' + value.version(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        append(StringUtils.SPACE + value.groupId(), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
    }
}
