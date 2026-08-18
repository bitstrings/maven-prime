package org.bitstrings.idea.plugins.mavenprime.editor;

import javax.swing.JList;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

final class ArtifactNodeRenderer
    extends ColoredListCellRenderer<MavenArtifactNode>
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void customizeCellRenderer(
        JList<? extends MavenArtifactNode> list,
        MavenArtifactNode node,
        int index,
        boolean selected,
        boolean focused)
    {
        setIcon(AllIcons.Nodes.PpLib);

        append(ExcludeTransitiveIntention.labelOf(node), SimpleTextAttributes.REGULAR_ATTRIBUTES);

        String scope = node.getArtifact().getScope();

        if (StringUtils.isNotBlank(scope))
        {
            append(StringUtils.SPACE + scope, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
        }
    }
}
