package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

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

    private transient boolean columnsSized;

    private transient int nameWidth;

    private transient long wallClockMillis;

    private transient Set<String> criticalPath = Set.of();

    private transient String highlighted;

    private transient Consumer<String> onModuleSelected = module -> { };

    public ProfileTable()
    {
        super(new ListTreeTableModelOnColumns(new DefaultMutableTreeNode(), new ColumnInfo<?, ?>[0]));

        setModel(new ListTreeTableModelOnColumns(root, columns()));

        setRootVisible(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        getTree().setShowsRootHandles(true);
        getTree().addTreeSelectionListener(event -> onModuleSelected.accept(selectedModule()));
        setTreeCellRenderer(new TimingRenderer());
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
            TreePath path = getTree().getPathForRow(row);
            Object timing = timingOf((DefaultMutableTreeNode) path.getLastPathComponent());

            if ((timing instanceof ModuleTiming) && ((ModuleTiming) timing).module().equals(module))
            {
                TreeUtil.selectPath(getTree(), path);

                return;
            }
        }
    }

    private String selectedModule()
    {
        TreePath path = getTree().getSelectionPath();

        return (path == null) ? null : moduleOf(path);
    }

    private static String moduleOf(TreePath path)
    {
        Object timing = timingOf((DefaultMutableTreeNode) path.getLastPathComponent());

        if (timing instanceof ModuleTiming)
        {
            return ((ModuleTiming) timing).module();
        }

        return (timing instanceof MojoTiming) ? ((MojoTiming) timing).module() : null;
    }

    public void setProfile(BuildProfile profile, BuildProfileSummary summary)
    {
        highlight(null);

        wallClockMillis = summary.wallClockMillis();
        criticalPath = Set.copyOf(summary.criticalPath());

        TreeState expansion = TreeState.createOn(getTree(), root);

        root.removeAllChildren();

        for (ModuleTiming module : profile.getModulesByDuration())
        {
            DefaultMutableTreeNode moduleNode = new DefaultMutableTreeNode(module);

            for (MojoTiming mojo : profile.getMojos(module.module()))
            {
                moduleNode.add(new DefaultMutableTreeNode(mojo));
            }

            root.add(moduleNode);
        }

        ((ListTreeTableModelOnColumns) getTableModel()).reload();

        expansion.applyTo(getTree(), root);

        if (!columnsSized)
        {
            columnsSized = sizeColumns();
        }

        repaint();
    }

    private ColumnInfo<?, ?>[] columns()
    {
        return new ColumnInfo<?, ?>[]
        {
            new TreeColumnInfo(MavenPrimeBundle.message("mavenprime.profile.column.name")),
            new TimingColumn(MavenPrimeBundle.message("mavenprime.profile.column.start"), true),
            new TimingColumn(MavenPrimeBundle.message("mavenprime.profile.column.duration"), false),
            new ShareColumn()
        };
    }

    private static Object timingOf(DefaultMutableTreeNode node)
    {
        return node.getUserObject();
    }

    private final class TimingColumn
        extends ColumnInfo<DefaultMutableTreeNode, String>
    {
        private final boolean start;

        TimingColumn(String name, boolean start)
        {
            super(name);

            this.start = start;
        }

        @Override
        public String valueOf(DefaultMutableTreeNode node)
        {
            Object timing = timingOf(node);

            if (timing instanceof ModuleTiming)
            {
                ModuleTiming module = (ModuleTiming) timing;

                return format(start ? module.startMillis() : module.durationMillis());
            }

            if (timing instanceof MojoTiming)
            {
                MojoTiming mojo = (MojoTiming) timing;

                return format(start ? mojo.startMillis() : mojo.durationMillis());
            }

            return StringUtils.EMPTY;
        }

        private String format(long millis)
        {
            return Formats.formatDuration(millis);
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
        public String valueOf(DefaultMutableTreeNode node)
        {
            Object timing = timingOf(node);

            long duration = (timing instanceof ModuleTiming)
                ? ((ModuleTiming) timing).durationMillis()
                : ((timing instanceof MojoTiming) ? ((MojoTiming) timing).durationMillis() : -1L);

            return ((duration < 0L) || (wallClockMillis == 0L))
                ? StringUtils.EMPTY
                : MavenPrimeBundle.message(
                    "mavenprime.profile.share",
                    Double.valueOf((duration * PERCENT) / wallClockMillis));
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
            Object timing = timingOf((DefaultMutableTreeNode) value);

            if (timing instanceof ModuleTiming)
            {
                showModule((ModuleTiming) timing);
            }
            else if (timing instanceof MojoTiming)
            {
                showMojo((MojoTiming) timing);
            }
        }

        private void showModule(ModuleTiming module)
        {
            boolean critical = criticalPath.contains(module.module());

            setIcon(critical ? AllIcons.Actions.Lightning : AllIcons.Nodes.Module);
            append(module.module(), module.module().equals(highlighted) ? POINTED_AT : BOLD);

            if (critical)
            {
                append(
                    ' ' + MavenPrimeBundle.message("mavenprime.profile.column.critical"),
                    SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            }
        }

        private void showMojo(MojoTiming mojo)
        {
            setIcon(AllIcons.Nodes.Method);
            append(mojo.goal(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

            if (StringUtils.isNotBlank(mojo.executionId()))
            {
                append('@' + mojo.executionId(), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            }
        }
    }
}
