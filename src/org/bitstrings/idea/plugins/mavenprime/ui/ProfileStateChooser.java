package org.bitstrings.idea.plugins.mavenprime.ui;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckboxTreeHelper;
import com.intellij.ui.ClickListener;
import com.intellij.ui.awt.RelativePoint;

public final class ProfileStateChooser
{
    private final JTree tree;

    private final BiConsumer<TreePath, ProfileState> chosen;

    public ProfileStateChooser(JTree tree, BiConsumer<TreePath, ProfileState> chosen)
    {
        this.tree = tree;
        this.chosen = chosen;
    }

    public void install()
    {
        new ClickListener()
        {
            @Override
            public boolean onClick(MouseEvent event, int clickCount)
            {
                return (event.getButton() == MouseEvent.BUTTON1)
                    && (clickCount == 1)
                    && isOverCheckbox(event)
                    && chooseAt(event);
            }
        }.installOn(tree);

        tree.addKeyListener(
            new KeyAdapter()
            {
                @Override
                public void keyPressed(KeyEvent event)
                {
                    if (CheckboxTreeHelper.isToggleEvent(event, tree) && chooseForSelection())
                    {
                        event.consume();
                    }
                }
            });
    }

    public boolean isOverCheckbox(MouseEvent event)
    {
        int row = tree.getRowForLocation(event.getX(), event.getY());

        Rectangle rowBounds = (row < 0) ? null : tree.getRowBounds(row);

        if (rowBounds == null)
        {
            return false;
        }

        Rectangle checkbox = checkboxBoundsAt(row, rowBounds);

        return (checkbox != null) && checkbox.contains(event.getPoint());
    }

    private Rectangle checkboxBoundsAt(int row, Rectangle rowBounds)
    {
        if (!(tree.getCellRenderer() instanceof CheckboxTreeBase.CheckboxTreeCellRendererBase))
        {
            return null;
        }

        CheckboxTreeBase.CheckboxTreeCellRendererBase renderer =
            (CheckboxTreeBase.CheckboxTreeCellRendererBase) tree.getCellRenderer();

        Component painted =
            renderer.getTreeCellRendererComponent(
                tree, tree.getPathForRow(row).getLastPathComponent(), false, false, false, row, false);

        painted.setBounds(rowBounds);
        painted.validate();

        if (!renderer.myCheckbox.isVisible())
        {
            return null;
        }

        Rectangle checkbox = renderer.myCheckbox.getBounds();

        if (checkbox.isEmpty())
        {
            checkbox.setSize(rowBounds.height, rowBounds.height);
        }

        checkbox.setLocation(rowBounds.getLocation());

        return checkbox;
    }

    public boolean chooseAt(MouseEvent event)
    {
        return show(tree.getPathForLocation(event.getX(), event.getY()), new RelativePoint(event));
    }

    public boolean chooseForSelection()
    {
        TreePath path = tree.getSelectionPath();

        Rectangle row = (path == null) ? null : tree.getPathBounds(path);

        return (row != null) && show(path, new RelativePoint(tree, new Point(row.x, row.y + row.height)));
    }

    boolean show(TreePath path, RelativePoint at)
    {
        ProfileEntry entry = ProfileEntries.at(path);

        if (entry == null)
        {
            return false;
        }

        tree.setSelectionPath(path);

        JBPopupFactory
            .getInstance()
            .createListPopup(new ProfileStatePopupStep(entry.getState(), state -> chosen.accept(path, state)))
            .show(at);

        return true;
    }
}
