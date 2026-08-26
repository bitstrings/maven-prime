package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.model.ModelOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ModelRefresher;
import org.bitstrings.idea.plugins.mavenprime.model.ModelStaleness;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceData;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceLog;
import org.bitstrings.idea.plugins.mavenprime.ui.BuildSelector;
import org.bitstrings.idea.plugins.mavenprime.ui.CompletenessBanner;
import org.bitstrings.idea.plugins.mavenprime.ui.RegisteredActions;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.bitstrings.idea.plugins.mavenprime.util.UiState;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;

public final class ModelProvenancePanel
    extends JPanel
    implements Disposable
{
    private static final long serialVersionUID = 1L;

    private static final List<Float> COLUMN_RATIOS = List.of(0.60F, 0.15F, 0.25F);

    private static final String COLUMN_WIDTH_KEY = "columnWidth.";

    private static final String WIDTH_PROPERTY = "width";

    private static final String TOOLBAR_PLACE = "MavenPrimeModel";

    private static final String STATE_PREFIX = "mavenPrime.model.";

    private static final String AUTO_REFRESH_ACTION = "MavenPrime.AutoRefreshModel";

    private final transient Project project;

    private final transient UiState state;

    private final transient Supplier<MavenProject> contextModule;

    private final transient DefaultMutableTreeNode root = new DefaultMutableTreeNode();

    private final transient DefaultMutableTreeNode dependenciesNode =
        new DefaultMutableTreeNode(ModelSection.DEPENDENCIES);

    private final transient DefaultMutableTreeNode pluginsNode =
        new DefaultMutableTreeNode(ModelSection.PLUGINS);

    private final transient DefaultMutableTreeNode propertiesNode =
        new DefaultMutableTreeNode(ModelSection.PROPERTIES);

    private final SimpleColoredComponent incomplete = new SimpleColoredComponent();

    final SimpleColoredComponent stale = new SimpleColoredComponent();

    private final transient ListTreeTableModelOnColumns model;

    private final TreeTableView table;

    private final JBScrollPane scroll;

    private final transient BuildSelector<ProvenanceData> buildSelector;

    private transient String module = StringUtils.EMPTY;

    private transient boolean columnsSized;

    public ModelProvenancePanel(Project project, Supplier<MavenProject> contextModule)
    {
        super(new BorderLayout());

        this.project = project;
        this.contextModule = contextModule;
        this.state = new UiState(project, STATE_PREFIX);
        this.buildSelector =
            new BuildSelector<>(entry -> ProvenanceLog.getInstance(project).select(entry));
        this.model = new ListTreeTableModelOnColumns(root, columns());
        this.table = new TreeTableView(model);
        this.scroll = new JBScrollPane(table);

        root.add(dependenciesNode);
        root.add(pluginsNode);
        root.add(propertiesNode);

        configureTable();

        add(buildNorth(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        MessageBusConnection connection = project.getMessageBus().connect(this);

        connection.subscribe(ProvenanceLog.TOPIC, (ProvenanceLog.ProvenanceListener) this::refresh);
        connection.subscribe(ModelRefresher.TOPIC, (ModelRefresher.RefreshListener) this::refresh);
        connection.subscribe(
            ModelStaleness.TOPIC, (ModelStaleness.StalenessListener) this::renderStale);

        refresh();

        TreeUtil.expandAll(table.getTree());

        Disposer.register(
            this,
            UiNotifyConnector.installOn(
                this,
                new Activatable()
                {
                    @Override
                    public void showNotify()
                    {
                        ModelRefresher.getInstance(project).refreshUnread();
                    }
                }));
    }

    @Override
    public void dispose()
    {
    }

    private JComponent buildNorth()
    {
        incomplete.setBorder(JBUI.Borders.empty(0, 8, 4, 8));
        stale.setBorder(JBUI.Borders.empty(0, 8, 4, 8));

        JPanel banners = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));

        banners.add(stale);
        banners.add(incomplete);

        JPanel north = new JPanel(new BorderLayout());

        north.add(buildToolbar(), BorderLayout.NORTH);
        north.add(banners, BorderLayout.CENTER);

        return north;
    }

    private JComponent buildToolbar()
    {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new RefreshModelAction());
        RegisteredActions.add(group, AUTO_REFRESH_ACTION);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, group, true);

        toolbar.setTargetComponent(this);

        JPanel bar = new JPanel(new BorderLayout());

        bar.add(toolbar.getComponent(), BorderLayout.WEST);
        bar.add(buildSelector.getComponent(), BorderLayout.EAST);

        return bar;
    }

    private ColumnInfo<?, ?>[] columns()
    {
        return new ColumnInfo<?, ?>[]
        {
            new TreeColumnInfo(MavenPrimeBundle.message("mavenprime.model.column.name")),
            new ValueColumn(),
            new OriginColumn()
        };
    }

    private void configureTable()
    {
        table.setRootVisible(false);
        table.getTree().setShowsRootHandles(true);
        table.setTreeCellRenderer(new ModelRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        columnsSized = restoreColumnWidths();

        for (int column = 0; column < COLUMN_RATIOS.size(); column++)
        {
            table.getColumnModel().getColumn(column).addPropertyChangeListener(this::columnChanged);
        }

        scroll.getViewport().addComponentListener(
            new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent event)
                {
                    sizeColumnsToViewport(scroll.getViewport().getWidth());
                }
            });

        new DoubleClickListener()
        {
            @Override
            protected boolean onDoubleClick(MouseEvent event)
            {
                return navigateToSelection();
            }
        }.installOn(table);
    }

    private boolean restoreColumnWidths()
    {
        int[] widths = new int[COLUMN_RATIOS.size()];

        for (int column = 0; column < widths.length; column++)
        {
            widths[column] = state.getInt(columnKey(column), 0);

            if (widths[column] <= 0)
            {
                return false;
            }
        }

        for (int column = 0; column < widths.length; column++)
        {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }

        return true;
    }

    void sizeColumnsToViewport(int available)
    {
        if (columnsSized || (available <= 0))
        {
            return;
        }

        columnsSized = true;

        for (int column = 0; column < COLUMN_RATIOS.size(); column++)
        {
            table
                .getColumnModel()
                .getColumn(column)
                .setPreferredWidth(Math.round(available * COLUMN_RATIOS.get(column).floatValue()));
        }
    }

    private void columnChanged(PropertyChangeEvent event)
    {
        if (columnsSized && WIDTH_PROPERTY.equals(event.getPropertyName()))
        {
            for (int column = 0; column < COLUMN_RATIOS.size(); column++)
            {
                state.setInt(columnKey(column), table.getColumnModel().getColumn(column).getWidth(), 0);
            }
        }
    }

    private static String columnKey(int column)
    {
        return COLUMN_WIDTH_KEY + column;
    }

    void renderStale()
    {
        boolean pending = ModelStaleness.getInstance(project).isStale();

        stale.clear();
        stale.setVisible(pending);

        if (!pending)
        {
            return;
        }

        stale.setIcon(AllIcons.General.Warning);
        stale.append(
            MavenPrimeBundle.message("mavenprime.model.stale"), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    public void refresh()
    {
        module = MavenProjects.moduleKeyOf(contextModule.get());

        TreeState state = TreeState.createOn(table.getTree(), root);

        ProvenanceLog log = ProvenanceLog.getInstance(project);

        CompletenessBanner.render(incomplete, log.getCompleteness());

        renderStale();

        buildSelector.reload(log.getBuilds(), log.getSelectedBuild());

        rebuild(dependenciesNode, log.getDependencies(module));
        rebuild(pluginsNode, log.getPlugins(module));
        rebuild(propertiesNode, log.getProperties(module));

        model.reload();

        state.applyTo(table.getTree(), root);

        table.repaint();
    }

    private static void rebuild(DefaultMutableTreeNode parent, List<? extends ProvenanceEvent> entries)
    {
        parent.removeAllChildren();

        for (ProvenanceEvent entry : entries)
        {
            parent.add(new DefaultMutableTreeNode(entry));
        }
    }

    private boolean navigateToSelection()
    {
        ProvenanceEvent selected = selectedEntry();

        if ((selected == null) || !selected.origin().isNavigable())
        {
            return false;
        }

        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(selected.origin().file());

        if (file == null)
        {
            return false;
        }

        new OpenFileDescriptor(project, file, selected.origin().line() - 1, 0).navigate(true);

        return true;
    }

    private ProvenanceEvent selectedEntry()
    {
        TreePath path = table.getTree().getPathForRow(table.getSelectedRow());

        if (path == null)
        {
            return null;
        }

        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();

        return (userObject instanceof ProvenanceEvent) ? (ProvenanceEvent) userObject : null;
    }

    private final class RefreshModelAction
        extends DumbAwareAction
    {
        RefreshModelAction()
        {
            super(
                MavenPrimeBundle.message("mavenprime.model.refresh"),
                MavenPrimeBundle.message("mavenprime.model.refresh.description"),
                AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(AnActionEvent event)
        {
            ModelRefresher.getInstance(project).refresh();
        }

        @Override
        public ActionUpdateThread getActionUpdateThread()
        {
            return ActionUpdateThread.EDT;
        }
    }

    enum ModelSection
    {
        DEPENDENCIES("dependencies"),
        PLUGINS("plugins"),
        PROPERTIES("properties");

        private final String bundleKey;

        ModelSection(String bundleKey)
        {
            this.bundleKey = bundleKey;
        }

        String getTitle()
        {
            return MavenPrimeBundle.message("mavenprime.model." + bundleKey);
        }
    }

    private final class ValueColumn
        extends ColumnInfo<DefaultMutableTreeNode, String>
    {
        ValueColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.model.column.value"));
        }

        private String summaryOf(DefaultMutableTreeNode node)
        {
            if (ModelRefresher.getInstance(project).isRunning())
            {
                return MavenPrimeBundle.message("mavenprime.model.reading");
            }

            if (ProvenanceLog.getInstance(project).getModules().isEmpty())
            {
                return MavenPrimeBundle.message("mavenprime.model.notRead");
            }

            return MavenPrimeBundle.message("mavenprime.model.count", Integer.valueOf(node.getChildCount()));
        }

        @Override
        public String valueOf(DefaultMutableTreeNode node)
        {
            Object row = node.getUserObject();

            return (row instanceof ProvenanceEvent) ? ((ProvenanceEvent) row).value() : summaryOf(node);
        }
    }

    private final class OriginColumn
        extends ColumnInfo<DefaultMutableTreeNode, String>
    {
        OriginColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.model.column.origin"));
        }

        @Override
        public String valueOf(DefaultMutableTreeNode node)
        {
            Object row = node.getUserObject();

            if (!(row instanceof ProvenanceEvent))
            {
                return StringUtils.EMPTY;
            }

            ModelOrigin origin = ((ProvenanceEvent) row).origin();

            if (!origin.isKnown())
            {
                return MavenPrimeBundle.message("mavenprime.model.origin.unknown");
            }

            return origin.declaredBy(module)
                ? MavenPrimeBundle.message("mavenprime.model.origin.own")
                : origin.modelId();
        }
    }

    private static final class ModelRenderer
        extends ColoredTreeCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void customizeCellRenderer(
            JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

            if (userObject instanceof ModelSection)
            {
                append(((ModelSection) userObject).getTitle(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            }
            else if (userObject instanceof ProvenanceEvent)
            {
                append(((ProvenanceEvent) userObject).name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }
}
