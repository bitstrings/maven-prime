package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeExecutor;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.RepositoryInspection;
import org.bitstrings.idea.plugins.mavenprime.ui.CommandAction;
import org.bitstrings.idea.plugins.mavenprime.ui.CommandToggleAction;
import org.bitstrings.idea.plugins.mavenprime.util.FileReveal;
import org.bitstrings.idea.plugins.mavenprime.util.MavenImports;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.bitstrings.idea.plugins.mavenprime.util.UiState;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.Formats;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.ClickListener;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import com.intellij.util.ui.tree.TreeUtil;

public final class DependencyAnalyzerPanel
    extends JPanel
    implements Disposable
{
    private static final long serialVersionUID = 1L;

    private static final String POPUP_PLACE = "MavenPrimeDependencyAnalyzer";

    private static final String TOOLBAR_PLACE = "MavenPrimeDependencyAnalyzerToolbar";

    private static final String STATE_PREFIX = "mavenPrime.dependencyAnalyzer.";

    private static final String PROPORTION_KEY = "MavenPrime.DependencyAnalyzer.proportion";

    private static final String VIEW_KEY = "view";

    private static final String SORT_KEY = "sort";

    private static final String DEPTH_KEY = "depth";

    private static final String FILTER_KEY = "filter";

    private static final String SCOPE_KEY = "scope";

    private static final String GROUP_ID_KEY = "showGroupId";

    private static final String SIZE_KEY = "showSize";

    private static final String VERTICAL_KEY = "verticalSplit";

    private static final String COLOR_KEY = "colored";

    private static final String PURGE_GOAL = "dependency:purge-local-repository";

    private static final boolean DEFAULT_GROUP_ID = false;

    private static final boolean DEFAULT_SIZE = false;

    private static final boolean DEFAULT_COLOR = true;

    private static final boolean DEFAULT_VERTICAL = false;

    private static final float SPLITTER_PROPORTION = 0.5F;

    private static final int SEARCH_DELAY_MILLIS = 250;

    private static final int SEARCH_COLUMNS = 18;

    private static final String SEPARATOR = " · ";

    private final transient Project project;

    private final transient VirtualFile file;

    private final transient UiState state;

    private final transient ArtifactSizeCache sizes = new ArtifactSizeCache();

    private transient ArtifactSizes sizeSnapshot = ArtifactSizes.EMPTY;

    private transient DependencyCriteria criteria = DependencyCriteria.UNFILTERED;

    private final ComboBox<DependencyViewMode> viewCombo = new ComboBox<>(DependencyViewMode.values());

    private final ComboBox<DependencySortMode> sortCombo = new ComboBox<>(DependencySortMode.values());

    private final ComboBox<DependencyDepth> depthCombo = new ComboBox<>(DependencyDepth.values());

    private final ComboBox<DependencyFilterMode> filterCombo = new ComboBox<>(DependencyFilterMode.values());

    private final ComboBox<DependencyScopeFilter> scopeCombo = new ComboBox<>(DependencyScopeFilter.values());

    private final SearchTextField searchField = new SearchTextField();

    private final SimpleColoredComponent summaryLabel = new SimpleColoredComponent();

    private final DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());

    private final Tree tree = new Tree(treeModel);

    private final DefaultTreeModel pathsModel = new DefaultTreeModel(new DefaultMutableTreeNode());

    private final Tree pathsTree = new Tree(pathsModel);

    private final SimpleColoredComponent pathsVerdict = new SimpleColoredComponent();

    private transient Runnable verdictNavigation;

    private final OnePixelSplitter splitter =
        new OnePixelSplitter(DEFAULT_VERTICAL, PROPORTION_KEY, SPLITTER_PROPORTION);

    private final transient Timer searchTimer = new Timer(SEARCH_DELAY_MILLIS, event -> rebuildTree());

    private transient MavenProject mavenProject;

    private transient DependencyAnalysis analysis;

    private transient int matchedCount;

    private transient long matchedSize;

    private transient volatile boolean disposed;

    public DependencyAnalyzerPanel(Project project, VirtualFile file)
    {
        super(new BorderLayout());

        this.project = project;
        this.file = file;
        this.state = new UiState(project, STATE_PREFIX);

        searchTimer.setRepeats(false);

        tree.setRootVisible(true);
        tree.setCellRenderer(new DependencyRenderer());
        tree.getSelectionModel().addTreeSelectionListener(event -> updatePaths());

        TreeSpeedSearch.installOn(tree);

        PopupHandler.installPopupMenu(tree, buildContextMenu(), POPUP_PLACE);

        pathsTree.setRootVisible(false);
        pathsTree.setCellRenderer(new PathRenderer());
        pathsTree.getEmptyText().setText(MavenPrimeBundle.message("mavenprime.dependencies.paths.empty"));

        registerTooltips(tree);
        registerTooltips(pathsTree);

        restoreState();

        project
            .getMessageBus()
            .connect(this)
            .subscribe(
                AnalyzerInspection.TOPIC, (AnalyzerInspection.AnalysisListener) this::narrowTo);

        MavenImports.onImported(project, this, this::refresh);
        MavenImports.onModelAvailable(project, this, this::refreshIfModelMissing);

        Disposer.register(
            this,
            UiNotifyConnector.installOn(
                this,
                new Activatable()
                {
                    @Override
                    public void showNotify()
                    {
                        refreshIfModelMissing();
                    }
                }));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(summaryLabel, BorderLayout.SOUTH);

        refresh();
    }

    private void registerTooltips(JComponent component)
    {
        ToolTipManager.sharedInstance().registerComponent(component);

        Disposer.register(this, () -> ToolTipManager.sharedInstance().unregisterComponent(component));
    }

    private void narrowTo(VirtualFile pom, ArtifactCoordinates coordinates)
    {
        if (!file.equals(pom))
        {
            return;
        }

        searchTimer.stop();
        searchField.setText(coordinates.artifactId());

        rebuildTree();
    }

    private void refreshIfModelMissing()
    {
        if (mavenProject == null)
        {
            refresh();
        }
    }

    public void refresh()
    {
        mavenProject = MavenProjects.byPomFile(project, file);
        analysis = DependencyAnalysis.of(mavenProject);

        primeSizes();

        rebuildTree();
    }

    private void primeSizes()
    {
        List<MavenArtifactNode> roots = analysis.getRoots();

        ApplicationManager
            .getApplication()
            .executeOnPooledThread(
                () ->
                {
                    sizes.prime(roots);

                    ApplicationManager
                        .getApplication()
                        .invokeLater(this::rebuildTree, ignored -> disposed || project.isDisposed());
                });
    }

    public JComponent getPreferredFocusedComponent()
    {
        return tree;
    }

    @Override
    public void dispose()
    {
        disposed = true;

        searchTimer.stop();
    }

    private void restoreState()
    {
        viewCombo.setSelectedItem(state.getMode(VIEW_KEY, DependencyViewMode.TREE));
        sortCombo.setSelectedItem(state.getMode(SORT_KEY, DependencySortMode.RESOLUTION));
        depthCombo.setSelectedItem(state.getMode(DEPTH_KEY, DependencyDepth.ALL));
        filterCombo.setSelectedItem(state.getMode(FILTER_KEY, DependencyFilterMode.ALL));
        scopeCombo.setSelectedItem(state.getMode(SCOPE_KEY, DependencyScopeFilter.ALL));

        splitter.setOrientation(state.isEnabled(VERTICAL_KEY, DEFAULT_VERTICAL));
    }

    private JComponent buildContent()
    {
        splitter.setFirstComponent(new JBScrollPane(tree));
        splitter.setSecondComponent(buildPathsPane());

        return splitter;
    }

    private JComponent buildToolbar()
    {
        searchField.getTextEditor().setColumns(SEARCH_COLUMNS);
        searchField
            .getTextEditor()
            .getDocument()
            .addDocumentListener(
                new DocumentAdapter()
                {
                    @Override
                    protected void textChanged(DocumentEvent event)
                    {
                        searchTimer.restart();
                    }
                });

        viewCombo.addActionListener(event -> applyMode(VIEW_KEY, viewMode()));
        sortCombo.addActionListener(event -> applyMode(SORT_KEY, sortMode()));
        depthCombo.addActionListener(event -> applyMode(DEPTH_KEY, depth()));
        filterCombo.addActionListener(event -> applyMode(FILTER_KEY, filterMode()));
        scopeCombo.addActionListener(event -> applyMode(SCOPE_KEY, scopeFilter()));

        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));

        modes.add(viewCombo);
        modes.add(sortCombo);
        modes.add(depthCombo);
        modes.add(filterCombo);
        modes.add(scopeCombo);

        JPanel search = new JPanel(new BorderLayout(JBUI.scale(6), 0));

        search.add(searchField, BorderLayout.CENTER);
        search.add(buildActions(), BorderLayout.EAST);

        JPanel toolbar = new JPanel(new BorderLayout());

        toolbar.add(modes, BorderLayout.NORTH);
        toolbar.add(search, BorderLayout.CENTER);

        return toolbar;
    }

    private JComponent buildActions()
    {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(
            new CommandToggleAction(
                MavenPrimeBundle.message("mavenprime.dependencies.showGroupId"),
                null,
                AllIcons.Actions.GroupByPackage,
                () -> state.isEnabled(GROUP_ID_KEY, DEFAULT_GROUP_ID),
                selected -> applyGroupId(selected.booleanValue())));
        group.add(
            new CommandToggleAction(
                MavenPrimeBundle.message("mavenprime.dependencies.showSize"),
                MavenPrimeBundle.message("mavenprime.dependencies.showSize.description"),
                AllIcons.Actions.Show,
                this::showSize,
                selected -> applySize(selected.booleanValue())));
        group.add(
            new CommandToggleAction(
                MavenPrimeBundle.message("mavenprime.dependencies.colored"),
                MavenPrimeBundle.message("mavenprime.dependencies.colored.description"),
                AllIcons.Actions.Colors,
                this::isColored,
                selected -> applyColor(selected.booleanValue())));
        group.add(
            new CommandToggleAction(
                MavenPrimeBundle.message("mavenprime.dependencies.splitHorizontally"),
                MavenPrimeBundle.message("mavenprime.dependencies.splitHorizontally.description"),
                AllIcons.Actions.SplitHorizontally,
                () -> state.isEnabled(VERTICAL_KEY, DEFAULT_VERTICAL),
                selected -> applyOrientation(selected.booleanValue())));
        group.addSeparator();
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.expandAll"),
                null,
                AllIcons.Actions.Expandall,
                () -> expandAll(tree)));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.collapseAll"),
                null,
                AllIcons.Actions.Collapseall,
                this::collapseAll));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.action.refresh"),
                null,
                AllIcons.Actions.Refresh,
                this::refresh));

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, group, true);

        actionToolbar.setTargetComponent(this);

        return actionToolbar.getComponent();
    }

    private DefaultActionGroup buildContextMenu()
    {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.action.exclude"),
                MavenPrimeBundle.message("mavenprime.dependencies.action.exclude.description"),
                AllIcons.Actions.Cancel,
                this::excludeSelected,
                this::isExcludable));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.action.navigate"),
                null,
                AllIcons.Actions.EditSource,
                this::navigateToDeclaration,
                () -> getSelectedNode() != null));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.action.goToModule"),
                MavenPrimeBundle.message("mavenprime.dependencies.action.goToModule.description"),
                AllIcons.Nodes.Module,
                this::goToSelectedModule,
                () -> getSelectedModule() != null));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.repository.inspect"),
                MavenPrimeBundle.message("mavenprime.repository.inspect.description"),
                AllIcons.Actions.Search,
                this::inspectInRepository,
                () -> getSelectedNode() != null));
        group.add(
            new CommandAction(
                RevealFileAction.getActionName(),
                MavenPrimeBundle.message("mavenprime.dependencies.action.reveal.description"),
                AllIcons.Actions.MenuOpen,
                () -> FileReveal.reveal(selectedArtifactLocation()),
                () -> RevealFileAction.isSupported() && (selectedArtifactPath() != null)));
        group.addSeparator();
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.purge"),
                MavenPrimeBundle.message("mavenprime.dependencies.purge.description"),
                AllIcons.Actions.GC,
                this::purgeLocalRepository,
                () -> mavenProject != null));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.action.copy"),
                null,
                AllIcons.Actions.Copy,
                this::copyCoordinates,
                () -> getSelectedNode() != null));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.action.copyXml"),
                MavenPrimeBundle.message("mavenprime.dependencies.action.copyXml.description"),
                AllIcons.Actions.ListFiles,
                this::copyAsXml,
                () -> getSelectedNode() != null));

        return group;
    }

    private void applyGroupId(boolean selected)
    {
        state.setEnabled(GROUP_ID_KEY, selected, DEFAULT_GROUP_ID);

        rebuildTree();
        pathsTree.repaint();
    }

    private void purgeLocalRepository()
    {
        if (mavenProject == null)
        {
            return;
        }

        int confirmation =
            Messages.showOkCancelDialog(
                project,
                MavenPrimeBundle.message(
                    "mavenprime.dependencies.purge.confirm", mavenProject.getDisplayName()),
                MavenPrimeBundle.message("mavenprime.dependencies.purge.title"),
                MavenPrimeBundle.message("mavenprime.dependencies.purge.ok"),
                MavenPrimeBundle.message("mavenprime.dependencies.purge.cancel"),
                Messages.getWarningIcon());

        if (confirmation != Messages.OK)
        {
            return;
        }

        MavenPrimeExecutor
            .getInstance(project)
            .execute(
                MavenPrimeRequest.in(mavenProject.getDirectory(), List.of(PURGE_GOAL)), ExecutionMode.RUN);
    }

    private boolean isColored()
    {
        return state.isEnabled(COLOR_KEY, DEFAULT_COLOR);
    }

    private void applyColor(boolean selected)
    {
        state.setEnabled(COLOR_KEY, selected, DEFAULT_COLOR);

        tree.repaint();
        pathsTree.repaint();
    }

    private void applySize(boolean selected)
    {
        state.setEnabled(SIZE_KEY, selected, DEFAULT_SIZE);

        rebuildTree();
    }

    private void applyOrientation(boolean selected)
    {
        state.setEnabled(VERTICAL_KEY, selected, DEFAULT_VERTICAL);

        splitter.setOrientation(selected);
    }

    private void applyMode(String key, Enum<?> value)
    {
        state.setMode(key, value);

        rebuildTree();
    }

    private void rebuildTree()
    {
        Object previous = treeModel.getRoot();

        boolean restorable = (previous instanceof TreeNode) && (((TreeNode) previous).getChildCount() > 0);

        TreeState state =
            restorable ? TreeState.createOn(tree, (DefaultMutableTreeNode) previous) : null;

        sizeSnapshot = sizes.snapshot();
        criteria = new DependencyCriteria(searchText(), filterMode(), scopeFilter());

        DependencyTreeBuilder builder =
            new DependencyTreeBuilder(analysis, criteria, sizeSnapshot, currentView());

        DefaultMutableTreeNode root = builder.build();

        matchedCount = builder.getMatchedArtifacts().size();
        matchedSize = builder.totalMatchedSize();

        treeModel.setRoot(root);

        showSummary();

        if (isNarrowed())
        {
            expandAll(tree);

            return;
        }

        if (state == null)
        {
            tree.expandRow(0);

            return;
        }

        state.applyTo(tree, root);
    }

    private void showSummary()
    {
        summaryLabel.clear();

        appendMetric("nodes", String.valueOf(analysis.getTotalCount()));
        appendMetric("conflicts", String.valueOf(analysis.getConflictedKeys().size()));

        if (hasSearch())
        {
            appendMetric("matches", String.valueOf(matchedCount));
        }

        if (showSize())
        {
            appendMetric("size", Formats.formatFileSize(matchedSize));
        }
    }

    private void appendMetric(String key, String value)
    {
        if (summaryLabel.getFragmentCount() > 0)
        {
            summaryLabel.append(SEPARATOR, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        summaryLabel.append(
            MavenPrimeBundle.message("mavenprime.dependencies.summary." + key) + ' ',
            SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
        summaryLabel.append(value, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    private boolean hasSearch()
    {
        return criteria.hasSearch();
    }

    private String searchText()
    {
        return searchField.getText().trim();
    }

    private boolean showSize()
    {
        return state.isEnabled(SIZE_KEY, DEFAULT_SIZE);
    }

    private boolean isNarrowed()
    {
        return criteria.isNarrowed();
    }

    private DependencyView currentView()
    {
        return new DependencyView(viewMode(), sortMode(), depth());
    }

    private DependencyViewMode viewMode()
    {
        return Objects.requireNonNullElse((DependencyViewMode) viewCombo.getSelectedItem(), DependencyViewMode.TREE);
    }

    private DependencySortMode sortMode()
    {
        return Objects.requireNonNullElse(
            (DependencySortMode) sortCombo.getSelectedItem(), DependencySortMode.RESOLUTION);
    }

    private DependencyDepth depth()
    {
        return Objects.requireNonNullElse((DependencyDepth) depthCombo.getSelectedItem(), DependencyDepth.ALL);
    }

    private DependencyFilterMode filterMode()
    {
        return Objects.requireNonNullElse(
            (DependencyFilterMode) filterCombo.getSelectedItem(), DependencyFilterMode.ALL);
    }

    private DependencyScopeFilter scopeFilter()
    {
        return Objects.requireNonNullElse(
            (DependencyScopeFilter) scopeCombo.getSelectedItem(), DependencyScopeFilter.ALL);
    }

    private static void expandAll(JTree target)
    {
        TreeUtil.expandAll(target);
    }

    private void collapseAll()
    {
        TreeUtil.collapseAll(tree, 1);
    }

    private JComponent buildPathsPane()
    {
        JPanel pane = new JPanel(new BorderLayout());

        pathsVerdict.setBorder(JBUI.Borders.empty(4, 8));

        new ClickListener()
        {
            @Override
            public boolean onClick(MouseEvent event, int clickCount)
            {
                if (verdictNavigation == null)
                {
                    return false;
                }

                verdictNavigation.run();

                return true;
            }
        }.installOn(pathsVerdict);

        pane.add(pathsVerdict, BorderLayout.NORTH);
        pane.add(new JBScrollPane(pathsTree), BorderLayout.CENTER);

        return pane;
    }

    private void updatePaths()
    {
        MavenArtifactNode selected = getSelectedNode();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        showVerdict(selected);

        if (selected != null)
        {
            for (DependencyPaths.PathHead head : DependencyPaths.headsFor(analysis, selected))
            {
                addPath(root, head);
            }
        }

        pathsModel.setRoot(root);

        expandAll(pathsTree);
    }

    private void showVerdict(MavenArtifactNode selected)
    {
        pathsVerdict.clear();

        verdictNavigation = null;

        DependencyPaths.ResolutionVerdict verdict =
            (selected == null) ? null : DependencyPaths.verdictFor(analysis, selected);

        if (verdict == null)
        {
            pathsVerdict.append(
                MavenPrimeBundle.message("mavenprime.dependencies.paths.noSelection"),
                SimpleTextAttributes.GRAYED_ATTRIBUTES);

            return;
        }

        pathsVerdict.append(
            MavenPrimeBundle.message("mavenprime.dependencies.paths.verdict", verdict.selectedVersion()),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        boolean navigable = (verdict.rule() == DependencyPaths.Rule.MANAGED) && (mavenProject != null);

        verdictNavigation = navigable ? this::navigateToManagedDeclaration : null;

        pathsVerdict.setCursor(
            navigable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        pathsVerdict.append(
            SEPARATOR + ruleTextOf(verdict),
            navigable ? SimpleTextAttributes.LINK_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES);
        pathsVerdict.append(
            SEPARATOR + MavenPrimeBundle.message(
                "mavenprime.dependencies.paths.count", Integer.valueOf(verdict.pathCount())),
            SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    private void navigateToManagedDeclaration()
    {
        MavenArtifactNode selected = getSelectedNode();

        if ((selected == null) || (mavenProject == null))
        {
            return;
        }

        MavenArtifact artifact = selected.getArtifact();

        WriteIntentReadAction.run(
            (Runnable) () -> PomDependencies.navigateTo(
                PomDependencies.findManagedDeclaration(
                    PomDependencies.modelOf(project, mavenProject),
                    artifact.getGroupId(),
                    artifact.getArtifactId())));
    }

    private static String ruleTextOf(DependencyPaths.ResolutionVerdict verdict)
    {
        return switch (verdict.rule())
        {
            case SINGLE -> MavenPrimeBundle.message("mavenprime.dependencies.paths.rule.single");
            case NEAREST_WINS ->
                MavenPrimeBundle.message(
                    "mavenprime.dependencies.paths.rule.nearest", Integer.valueOf(verdict.winnerDepth()));
            case MANAGED ->
                MavenPrimeBundle.message(
                    "mavenprime.dependencies.paths.rule.managed",
                    verdict.declaredVersion(),
                    verdict.selectedVersion());
        };
    }

    private static void addPath(DefaultMutableTreeNode root, DependencyPaths.PathHead head)
    {
        DefaultMutableTreeNode branch = new DefaultMutableTreeNode(head);

        DefaultMutableTreeNode parent = branch;

        for (MavenArtifactNode ancestor : DependencyPaths.ancestorsOf(head.occurrence()))
        {
            DefaultMutableTreeNode step = new DefaultMutableTreeNode(ancestor);

            parent.add(step);

            parent = step;
        }

        root.add(branch);
    }

    private MavenArtifactNode getSelectedNode()
    {
        TreePath selection = tree.getSelectionPath();

        if (selection == null)
        {
            return null;
        }

        Object userObject = ((DefaultMutableTreeNode) selection.getLastPathComponent()).getUserObject();

        return (userObject instanceof MavenArtifactNode) ? (MavenArtifactNode) userObject : null;
    }

    private MavenProject getSelectedModule()
    {
        MavenArtifactNode selected = getSelectedNode();

        return (selected == null)
            ? null
            : MavenProjectsManager.getInstance(project).findProject(selected.getArtifact());
    }

    private final class DependencyRenderer
        extends ColoredTreeCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void customizeCellRenderer(
            JTree renderedTree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

            if (userObject instanceof String)
            {
                setIcon(null);
                append((String) userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

                return;
            }

            if (!(userObject instanceof MavenArtifactNode))
            {
                setIcon(AllIcons.Nodes.Module);
                append(
                    (mavenProject == null) ? file.getName() : mavenProject.getDisplayName(),
                    SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

                return;
            }

            MavenArtifactNode node = (MavenArtifactNode) userObject;

            MavenArtifact artifact = node.getArtifact();

            ResolutionReason reason = ResolutionReason.of(node);

            boolean conflicted = analysis.isConflicted(node);

            setIcon(conflicted ? AllIcons.General.Warning : AllIcons.Nodes.PpLib);
            setToolTipText((reason == ResolutionReason.INCLUDED) ? null : reason.describe(node));

            DependencyRowText.appendName(
                artifact,
                state.isEnabled(GROUP_ID_KEY, DEFAULT_GROUP_ID),
                reason.isOmitted()
                    ? SimpleTextAttributes.GRAYED_ATTRIBUTES
                    : SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
                this::appendSearchable);

            DependencyRowText.appendVersion(
                node,
                reason,
                DependencyRowText.versionAttributes(reason, conflicted, isColored()),
                this::appendSearchable);
            DependencyRowText.appendScope(this, artifact, isColored());

            if (showSize())
            {
                DependencyRowText.appendSize(this, sizeSnapshot, node);
            }

            DependencyRowText.appendMarker(this, reason, isColored());
        }

        private void appendSearchable(String text, SimpleTextAttributes attributes)
        {
            String search = searchText();

            if (search.isEmpty())
            {
                append(text, attributes);

                return;
            }

            int from = 0;

            for (
                int match = StringUtil.indexOfIgnoreCase(text, search, from);
                match >= 0;
                match = StringUtil.indexOfIgnoreCase(text, search, from)
            )
            {
                append(text.substring(from, match), attributes);
                append(text.substring(match, match + search.length()), highlighted(attributes));

                from = match + search.length();
            }

            append(text.substring(from), attributes);
        }

        private SimpleTextAttributes highlighted(SimpleTextAttributes attributes)
        {
            return new SimpleTextAttributes(
                attributes.getStyle() | SimpleTextAttributes.STYLE_SEARCH_MATCH, attributes.getFgColor());
        }
    }

    private final class PathRenderer
        extends ColoredTreeCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void customizeCellRenderer(
            JTree renderedTree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

            if (userObject instanceof DependencyPaths.PathHead)
            {
                showHead((DependencyPaths.PathHead) userObject);

                return;
            }

            if (!(userObject instanceof MavenArtifactNode))
            {
                setIcon(null);

                return;
            }

            showAncestor((MavenArtifactNode) userObject);
        }

        private void showHead(DependencyPaths.PathHead head)
        {
            MavenArtifactNode node = head.occurrence();

            showAncestor(node);

            append(
                DependencyRowText.SEPARATOR + MavenPrimeBundle.message(
                    "mavenprime.dependencies.paths.depth", Integer.valueOf(head.depth())),
                SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);

            if (head.winner())
            {
                append(
                    DependencyRowText.SEPARATOR
                        + MavenPrimeBundle.message("mavenprime.dependencies.paths.winner"),
                    DependencyColors.winnerAttributes(isColored()));
            }
        }

        private void showAncestor(MavenArtifactNode node)
        {
            MavenArtifact artifact = node.getArtifact();

            ResolutionReason reason = ResolutionReason.of(node);

            boolean conflicted = analysis.isConflicted(node);

            setIcon(conflicted ? AllIcons.General.Warning : AllIcons.Nodes.PpLib);
            setToolTipText((reason == ResolutionReason.INCLUDED) ? null : reason.describe(node));

            DependencyRowText.appendName(
                artifact,
                state.isEnabled(GROUP_ID_KEY, DEFAULT_GROUP_ID),
                SimpleTextAttributes.REGULAR_ATTRIBUTES,
                this::append);

            DependencyRowText.appendVersion(
                node,
                reason,
                DependencyRowText.versionAttributes(reason, conflicted, isColored()),
                this::append);
            DependencyRowText.appendScope(this, artifact, isColored());
            DependencyRowText.appendMarker(this, reason, isColored());
        }
    }

    private void excludeSelected()
    {
        MavenArtifactNode selected = getSelectedNode();

        if ((selected != null) && (mavenProject != null))
        {
            DependencyExcluder.exclude(project, mavenProject, selected);
        }
    }

    private boolean isExcludable()
    {
        MavenArtifactNode selected = getSelectedNode();

        return (mavenProject != null) && (selected != null) && DependencyExcluder.isExcludable(selected);
    }

    private void navigateToDeclaration()
    {
        MavenArtifactNode selected = getSelectedNode();

        if ((selected == null) || (mavenProject == null))
        {
            return;
        }

        WriteIntentReadAction.run(
            (Runnable) () -> PomDependencies.navigateTo(
                PomDependencies.declarationOf(project, mavenProject, selected)));
    }

    private void goToSelectedModule()
    {
        MavenProject module = getSelectedModule();

        if (module != null)
        {
            FileEditorManager.getInstance(project).openFile(module.getFile(), true);
        }
    }

    private void inspectInRepository()
    {
        MavenArtifactNode selected = getSelectedNode();

        if (selected != null)
        {
            RepositoryInspection.inspect(project, ArtifactCoordinates.of(selected.getArtifact()));
        }
    }

    private Path selectedArtifactLocation()
    {
        MavenArtifactNode selected = getSelectedNode();

        if (selected == null)
        {
            return null;
        }

        Path file = ArtifactLocation.fileOf(selected.getArtifact());

        return (file != null) ? file : ArtifactLocation.directoryOf(selected.getArtifact());
    }

    private Path selectedArtifactPath()
    {
        MavenArtifactNode selected = getSelectedNode();

        return (selected == null) ? null : ArtifactLocation.pathOf(selected.getArtifact());
    }

    private void copyCoordinates()
    {
        MavenArtifactNode selected = getSelectedNode();

        if (selected != null)
        {
            CopyPasteManager.copyTextToClipboard(selected.getArtifact().getDisplayStringFull());
        }
    }

    private void copyAsXml()
    {
        MavenArtifactNode selected = getSelectedNode();

        if (selected != null)
        {
            CopyPasteManager.copyTextToClipboard(DependencyXml.of(selected.getArtifact()));
        }
    }
}
