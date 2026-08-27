package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileGroup;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileGrouping;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileGroups;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileView;
import org.bitstrings.idea.plugins.mavenprime.profile.WorkerLanes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;

public final class ProfileTable
    extends TreeTableView
{
    private static final long serialVersionUID = 1L;

    private static final int COLUMN_PADDING = 18;

    private static final int NAME_COLUMN = 0;

    private static final double PERCENT = 100.0D;

    private static final SimpleTextAttributes BOLD = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

    private static final SimpleTextAttributes POINTED_AT =
        new SimpleTextAttributes(
            SimpleTextAttributes.STYLE_BOLD | SimpleTextAttributes.STYLE_UNDERLINE, null);

    private final transient DefaultMutableTreeNode root = new DefaultMutableTreeNode();

    private transient ProfileGrouping grouping = ProfileGrouping.MODULE;

    private transient WorkerLanes lanes = WorkerLanes.of(List.of());

    private transient boolean columnsSized;

    private transient int nameWidth;

    private transient long wallClockMillis;

    private transient Set<String> criticalPath = Set.of();

    private transient String highlighted;

    private transient Consumer<String> onModuleSelected = module -> { };

    private final transient TreeSelectionListener selectionListener =
        event -> onModuleSelected.accept(selectedModule());

    public ProfileTable()
    {
        super(new ListTreeTableModelOnColumns(new DefaultMutableTreeNode(), new ColumnInfo<?, ?>[0]));

        installModel();
    }

    public void setOnModuleSelected(Consumer<String> listener)
    {
        onModuleSelected = listener;
    }

    @Override
    public void doLayout()
    {
        TableColumn dragged = resizingColumn();

        if (dragged == null)
        {
            stretchNameColumn();
        }
        else if (dragged == getColumnModel().getColumn(NAME_COLUMN))
        {
            nameWidth = dragged.getWidth();
        }

        super.doLayout();
    }

    @Override
    public boolean getScrollableTracksViewportWidth()
    {
        Container parent = getParent();

        return (resizingColumn() == null)
            && (parent instanceof JViewport)
            && (totalPreferredWidth() <= parent.getWidth());
    }

    private TableColumn resizingColumn()
    {
        JTableHeader header = getTableHeader();

        return (header == null) ? null : header.getResizingColumn();
    }

    private void stretchNameColumn()
    {
        Container parent = getParent();

        if (!(parent instanceof JViewport))
        {
            return;
        }

        TableColumn name = getColumnModel().getColumn(NAME_COLUMN);

        name.setPreferredWidth(
            Math.max(nameWidth, parent.getWidth() - (totalPreferredWidth() - name.getPreferredWidth())));
    }

    private int totalPreferredWidth()
    {
        int total = 0;

        for (int column = 0; column < getColumnModel().getColumnCount(); column++)
        {
            total += getColumnModel().getColumn(column).getPreferredWidth();
        }

        return total;
    }

    private boolean sizeColumns()
    {
        Font font = getFont();

        if ((font == null) || (getTree().getRowCount() == 0))
        {
            return false;
        }

        FontMetrics metrics = getFontMetrics(font);

        nameWidth = getTree().getPreferredSize().width + JBUI.scale(COLUMN_PADDING);

        getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(nameWidth);

        for (int column = NAME_COLUMN + 1; column < getColumnModel().getColumnCount(); column++)
        {
            getColumnModel().getColumn(column).setPreferredWidth(widthOf(metrics, column));
        }

        return true;
    }

    private int widthOf(FontMetrics metrics, int column)
    {
        int width = metrics.stringWidth(textOf(getColumnModel().getColumn(column).getHeaderValue()));

        for (TreeNode node : TreeUtil.treeNodeTraverser(root).traverse())
        {
            width =
                Math.max(width, metrics.stringWidth(textOf(getTableModel().getValueAt(node, column))));
        }

        return width + JBUI.scale(COLUMN_PADDING);
    }

    private static String textOf(Object value)
    {
        return (value == null) ? StringUtils.EMPTY : value.toString();
    }

    public void highlight(String module)
    {
        if (!Objects.equals(highlighted, module))
        {
            highlighted = module;

            repaint();
        }
    }

    public void select(String module)
    {
        for (int row = 0; row < getTree().getRowCount(); row++)
        {
            if (module.equals(moduleOf(getTree().getPathForRow(row))))
            {
                setRowSelectionInterval(row, row);

                scrollRectToVisible(getCellRect(row, NAME_COLUMN, true));

                return;
            }
        }
    }

    private String selectedModule()
    {
        TreePath path = getTree().getSelectionPath();

        return (path == null) ? null : moduleOf(path);
    }

    private String moduleOf(TreePath path)
    {
        Object row = rowOf((DefaultMutableTreeNode) path.getLastPathComponent());

        if (row instanceof MojoTiming mojo)
        {
            return mojo.module();
        }

        return ((row instanceof ProfileGroup group) && grouping.isByModule()) ? group.name() : null;
    }

    public void setProfile(BuildProfile profile, BuildProfileSummary summary, ProfileView view)
    {
        highlight(null);

        wallClockMillis = summary.wallClockMillis();
        criticalPath = Set.copyOf(summary.criticalPath());
        lanes = profile.getWorkerLanes();

        boolean regrouped = grouping != view.grouping();

        grouping = view.grouping();

        TreeState expansion = regrouped ? null : TreeState.createOn(getTree(), root);

        root.removeAllChildren();

        for (ProfileGroup group : ProfileGroups.of(profile, view))
        {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);

            for (MojoTiming mojo : group.mojos())
            {
                groupNode.add(new DefaultMutableTreeNode(mojo));
            }

            root.add(groupNode);
        }

        if (regrouped)
        {
            installModel();
        }
        else
        {
            ((ListTreeTableModelOnColumns) getTableModel()).reload();

            expansion.applyTo(getTree(), root);
        }

        if (!columnsSized)
        {
            columnsSized = sizeColumns();
        }

        repaint();
    }

    private void installModel()
    {
        setModel(new ListTreeTableModelOnColumns(root, columns()));

        setRootVisible(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setTreeCellRenderer(new TimingRenderer());

        getTree().setShowsRootHandles(true);
        getTree().removeTreeSelectionListener(selectionListener);
        getTree().addTreeSelectionListener(selectionListener);

        columnsSized = false;
    }

    private ColumnInfo<?, ?>[] columns()
    {
        List<ColumnInfo<?, ?>> columns = new ArrayList<>();

        columns.add(new TreeColumnInfo(grouping.getColumnTitle()));
        columns.add(
            new TimingColumn(
                MavenPrimeBundle.message("mavenprime.profile.column.start"),
                "mavenprime.profile.help.column.start",
                true));
        columns.add(
            new TimingColumn(
                MavenPrimeBundle.message("mavenprime.profile.column.duration"),
                "mavenprime.profile.help.column.duration",
                false));
        columns.add(new CountColumn());
        columns.add(new ShareColumn());

        if (grouping.isByModule())
        {
            columns.add(new PayoffColumn());
        }

        return columns.toArray(new ColumnInfo<?, ?>[0]);
    }

    private static Object rowOf(DefaultMutableTreeNode node)
    {
        return node.getUserObject();
    }

    private static long durationOf(Object row)
    {
        if (row instanceof ProfileGroup group)
        {
            return group.durationMillis();
        }

        return (row instanceof MojoTiming mojo) ? mojo.durationMillis() : -1L;
    }

    private final class TimingColumn
        extends ColumnInfo<DefaultMutableTreeNode, String>
    {
        private final boolean start;

        private final transient String helpKey;

        TimingColumn(String name, String helpKey, boolean start)
        {
            super(name);

            this.helpKey = helpKey;
            this.start = start;
        }

        @Override
        public String getTooltipText()
        {
            return MavenPrimeBundle.message(helpKey);
        }

        @Override
        public String valueOf(DefaultMutableTreeNode node)
        {
            Object row = rowOf(node);

            if (row instanceof ProfileGroup group)
            {
                return Formats.formatDuration(start ? group.startMillis() : group.durationMillis());
            }

            if (row instanceof MojoTiming mojo)
            {
                return Formats.formatDuration(start ? mojo.startMillis() : mojo.durationMillis());
            }

            return StringUtils.EMPTY;
        }
    }

    private final class CountColumn
        extends ColumnInfo<DefaultMutableTreeNode, String>
    {
        CountColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.profile.column.goals"));
        }

        @Override
        public String getTooltipText()
        {
            return MavenPrimeBundle.message("mavenprime.profile.help.column.goals");
        }

        @Override
        public String valueOf(DefaultMutableTreeNode node)
        {
            Object row = rowOf(node);

            return (row instanceof ProfileGroup group)
                ? String.valueOf(group.mojos().size())
                : StringUtils.EMPTY;
        }
    }

    private final class ShareColumn
        extends ColumnInfo<DefaultMutableTreeNode, String>
    {
        ShareColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.profile.column.share"));
        }

        @Override
        public String getTooltipText()
        {
            return MavenPrimeBundle.message("mavenprime.profile.help.column.share");
        }

        @Override
        public String valueOf(DefaultMutableTreeNode node)
        {
            long duration = durationOf(rowOf(node));

            return ((duration < 0L) || (wallClockMillis == 0L))
                ? StringUtils.EMPTY
                : MavenPrimeBundle.message(
                    "mavenprime.profile.share",
                    Double.valueOf((duration * PERCENT) / wallClockMillis));
        }
    }

    private final class PayoffColumn
        extends ColumnInfo<DefaultMutableTreeNode, String>
    {
        PayoffColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.profile.column.payoff"));
        }

        @Override
        public String getTooltipText()
        {
            return MavenPrimeBundle.message("mavenprime.profile.help.column.payoff");
        }

        @Override
        public String valueOf(DefaultMutableTreeNode node)
        {
            Object row = rowOf(node);

            if (!(row instanceof ProfileGroup group) || !group.hasPayoff())
            {
                return StringUtils.EMPTY;
            }

            return (group.payoffMillis() == 0L)
                ? MavenPrimeBundle.message("mavenprime.profile.payoff.none")
                : Formats.formatDuration(group.payoffMillis());
        }
    }

    private final class TimingRenderer
        extends ColoredTreeCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void customizeCellRenderer(
            JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focus)
        {
            Object node = rowOf((DefaultMutableTreeNode) value);

            if (node instanceof ProfileGroup group)
            {
                showGroup(group);
            }
            else if (node instanceof MojoTiming mojo)
            {
                showMojo(mojo);
            }
        }

        private void showGroup(ProfileGroup group)
        {
            boolean critical = grouping.isByModule() && criticalPath.contains(group.name());
            String name = nameOf(group);

            setIcon(critical ? AllIcons.Actions.Lightning : iconOf(grouping));
            append(name, name.equals(highlighted) ? POINTED_AT : BOLD);

            if (critical)
            {
                append(
                    ' ' + MavenPrimeBundle.message("mavenprime.profile.column.critical"),
                    SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            }
        }

        // A builder thread carries the name of whichever project it is running, so its id is the only
        // stable identity, and an id is not something a reader can act on.
        private String nameOf(ProfileGroup group)
        {
            int ordinal =
                (grouping == ProfileGrouping.WORKER) ? lanes.ordinalOf(group.name()) : Integer.MIN_VALUE;

            if (ordinal >= 0)
            {
                return MavenPrimeBundle.message(
                    "mavenprime.profile.worker", Integer.valueOf(ordinal + 1));
            }

            return StringUtils.defaultIfBlank(
                group.name(), MavenPrimeBundle.message("mavenprime.profile.group.none"));
        }

        private void showMojo(MojoTiming mojo)
        {
            setIcon(AllIcons.Nodes.Method);

            if (grouping.isByModule())
            {
                append(mojo.goal(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                appendExecution(mojo);

                return;
            }

            append(mojo.module(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            append(' ' + mojo.goal(), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            appendExecution(mojo);
        }

        private void appendExecution(MojoTiming mojo)
        {
            if (StringUtils.isNotBlank(mojo.executionId()))
            {
                append('@' + mojo.executionId(), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            }
        }

        private Icon iconOf(ProfileGrouping key)
        {
            return switch (key)
            {
                case MODULE -> AllIcons.Nodes.Module;
                case PLUGIN -> AllIcons.Nodes.Artifact;
                case GOAL -> AllIcons.Nodes.Method;
                case PHASE -> AllIcons.Nodes.Folder;
                case WORKER -> AllIcons.Debugger.Threads;
            };
        }
    }
}
