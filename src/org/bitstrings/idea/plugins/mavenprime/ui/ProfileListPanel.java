package org.bitstrings.idea.plugins.mavenprime.ui;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;

public final class ProfileListPanel
    extends JPanel
{
    private static final long serialVersionUID = 1L;

    private static final int MIN_VISIBLE_ROWS = 1;

    private static final int MAX_VISIBLE_ROWS = 15;

    private final transient DefaultMutableTreeNode root = new DefaultMutableTreeNode();

    private final transient DefaultTreeModel model = new DefaultTreeModel(root);

    final Tree tree = new Tree(model);

    private final transient ProfileStateChooser chooser = new ProfileStateChooser(tree, this::apply);

    private final transient Consumer<ProfileEntry> changed;

    public ProfileListPanel()
    {
        this(entry -> { });
    }

    public ProfileListPanel(Consumer<ProfileEntry> changed)
    {
        super(new BorderLayout());

        this.changed = changed;

        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);
        tree.setCellRenderer(new ProfileRenderer());
        tree.getEmptyText().setText(MavenPrimeBundle.message("mavenprime.profiles.empty"));

        chooser.install();

        JBScrollPane scroll = new JBScrollPane(tree);

        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scroll, BorderLayout.CENTER);
    }

    public void setProfiles(Collection<String> availableProfiles, Map<String, Boolean> selected)
    {
        String wasSelected = selectedProfileName();

        root.removeAllChildren();

        for (ProfileEntry entry : ProfileEntries.merge(availableProfiles, selected))
        {
            root.add(new DefaultMutableTreeNode(entry));
        }

        model.reload();

        tree.setVisibleRowCount(
            Math.min(Math.max(root.getChildCount(), MIN_VISIBLE_ROWS), MAX_VISIBLE_ROWS));

        selectProfile(wasSelected);
    }

    private String selectedProfileName()
    {
        ProfileEntry entry = ProfileEntries.at(tree.getSelectionPath());

        return (entry == null) ? null : entry.getName();
    }

    private void selectProfile(String name)
    {
        if (name == null)
        {
            return;
        }

        for (int child = 0; child < root.getChildCount(); child++)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(child);

            if (name.equals(((ProfileEntry) node.getUserObject()).getName()))
            {
                TreeUtil.selectPath(tree, new TreePath(node.getPath()));

                return;
            }
        }
    }

    public Map<String, Boolean> getSelectedProfiles()
    {
        return ProfileEntries.selectedIn(root);
    }

    public JComponent getPreferredFocusedComponent()
    {
        return tree;
    }

    public void apply(TreePath path, ProfileState state)
    {
        ProfileEntry entry = ProfileEntries.at(path);

        if (entry != null)
        {
            entry.setState(state);

            model.nodeChanged((DefaultMutableTreeNode) path.getLastPathComponent());

            changed.accept(entry);
        }
    }

    private static final class ProfileRenderer
        extends CheckboxTreeBase.CheckboxTreeCellRendererBase
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void customizeRenderer(
            JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            ProfileRows.render(this, value);
        }
    }
}
