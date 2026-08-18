package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionListener;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public final class ContextFollower
    implements Disposable
{
    private static final String FOCUS_OWNER_PROPERTY = "focusOwner";

    private final Project project;

    private final Component owner;

    private final Consumer<VirtualFile> onFile;

    private final Set<JTree> trackedTrees = Collections.newSetFromMap(new WeakHashMap<>());

    private final TreeSelectionListener treeSelection = event -> follow((Component) event.getSource());

    private final PropertyChangeListener focusMoved = event -> follow((Component) event.getNewValue());

    public ContextFollower(Project project, Component owner, Consumer<VirtualFile> onFile)
    {
        this.project = project;
        this.owner = owner;
        this.onFile = onFile;

        KeyboardFocusManager
            .getCurrentKeyboardFocusManager()
            .addPropertyChangeListener(FOCUS_OWNER_PROPERTY, focusMoved);
    }

    @Override
    public void dispose()
    {
        KeyboardFocusManager
            .getCurrentKeyboardFocusManager()
            .removePropertyChangeListener(FOCUS_OWNER_PROPERTY, focusMoved);

        for (JTree tree : trackedTrees)
        {
            tree.removeTreeSelectionListener(treeSelection);
        }

        trackedTrees.clear();
    }

    void follow(Component source)
    {
        if ((source == null) || SwingUtilities.isDescendingFrom(source, owner))
        {
            return;
        }

        DataContext data = DataManager.getInstance().getDataContext(source);

        if (!project.equals(CommonDataKeys.PROJECT.getData(data)))
        {
            return;
        }

        trackTree(source);

        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(data);

        if (file != null)
        {
            onFile.accept(file);
        }
    }

    private void trackTree(Component source)
    {
        if (!(source instanceof JTree))
        {
            return;
        }

        JTree tree = (JTree) source;

        if (trackedTrees.add(tree))
        {
            tree.addTreeSelectionListener(treeSelection);
        }
    }
}
