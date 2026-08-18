package org.bitstrings.idea.plugins.mavenprime.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.treeStructure.Tree;

public class ProfileStateChooserPlatformTest
    extends BasePlatformTestCase
{
    public void testShow_aRowThatIsNotAProfile_opensNothing()
    {
        Tree tree = new Tree();

        assertFalse(
            "consuming the click on every row swallows the double-click that opens the property dialog",
            chooserOn(tree).show(pathTo("not a profile"), new RelativePoint(tree, new Point())));
    }

    public void testShow_aRowWithNoPathAtAll_opensNothing()
    {
        Tree tree = new Tree();

        assertFalse(
            "clicking below the last row would otherwise open a dropdown bound to no profile",
            chooserOn(tree).show(null, new RelativePoint(tree, new Point())));
    }

    public void testChooseForSelection_noSelectedRow_opensNothing()
    {
        Tree tree = new Tree();

        tree.clearSelection();

        assertFalse(
            "the toggle key with nothing selected would open a dropdown over an empty tree",
            chooserOn(tree).chooseForSelection());
    }

    public void testIsOverCheckbox_theLeadingEdgeOfAProfileRow_opensTheStateDropdown()
    {
        Tree tree = profileTree();

        assertTrue(
            "with no toggle zone the checkbox is decoration and the row cannot be changed by mouse",
            chooserOn(tree).isOverCheckbox(clickAt(tree, leadingEdgeOf(tree))));
    }

    public void testIsOverCheckbox_theProfileNameBesideTheCheckbox_leavesTheRowAlone()
    {
        Tree tree = profileTree();

        Rectangle row = tree.getRowBounds(0);

        assertFalse(
            "a zone that reaches the name opens the state dropdown when the user clicks the label",
            chooserOn(tree)
                .isOverCheckbox(
                    clickAt(tree, new Point((row.x + row.width) - 1, row.y + (row.height / 2)))));
    }

    public void testIsOverCheckbox_aRendererThatDrawsNoCheckbox_leavesTheRowAlone()
    {
        Tree tree = profileTree();

        tree.setCellRenderer(new DefaultTreeCellRenderer());

        assertFalse(
            "a row-height square opens the state dropdown on rows that paint no checkbox at all",
            chooserOn(tree).isOverCheckbox(clickAt(tree, leadingEdgeOf(tree))));
    }

    private static ProfileStateChooser chooserOn(Tree tree)
    {
        return new ProfileStateChooser(tree, (path, state) -> { });
    }

    private static Tree profileTree()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of("release"), Map.of());
        panel.setSize(panel.getPreferredSize());
        panel.doLayout();

        return panel.tree;
    }

    private static Point leadingEdgeOf(Tree tree)
    {
        Rectangle row = tree.getRowBounds(0);

        return new Point(row.x + 1, row.y + (row.height / 2));
    }

    private static MouseEvent clickAt(Tree tree, Point point)
    {
        return new MouseEvent(
            tree, MouseEvent.MOUSE_RELEASED, 0L, 0, point.x, point.y, 1, false, MouseEvent.BUTTON1);
    }

    private static TreePath pathTo(Object row)
    {
        return new TreePath(new DefaultMutableTreeNode(row));
    }
}
