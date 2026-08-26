package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeIcons;
import org.bitstrings.idea.plugins.mavenprime.actions.MavenPrimeActionContext;
import org.bitstrings.idea.plugins.mavenprime.config.MavenPrimeConfigService;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperty;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;
import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeExecutor;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalScope;
import org.bitstrings.idea.plugins.mavenprime.model.ModelStaleness;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.ui.CommandAction;
import org.bitstrings.idea.plugins.mavenprime.ui.GoalEditorDialog;
import org.bitstrings.idea.plugins.mavenprime.ui.GoalsTextField;
import org.bitstrings.idea.plugins.mavenprime.ui.RegisteredActions;
import org.bitstrings.idea.plugins.mavenprime.util.MavenImports;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.bitstrings.idea.plugins.mavenprime.util.UiState;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ClickListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

public final class MavenPrimeToolWindowPanel
    extends SimpleToolWindowPanel
    implements Disposable
{
    private static final long serialVersionUID = 1L;

    private static final String TOOLBAR_PLACE = "MavenPrimeToolWindow";

    private static final String STATE_PREFIX = "mavenPrime.toolWindow.";

    private static final String TAB_KEY = "selectedTab";

    private static final String CONTEXT_KEY = "showContext";

    private static final String PINNED_MODULE_KEY = "pinnedModule";

    private static final String SEPARATOR = " · ";

    private static final String OFFLINE_ACTION = "Maven.ToggleOffline";

    private static final String SKIP_TESTS_ACTION = "Maven.ToggleSkipTests";

    static final List<String> IDE_TOGGLE_ACTIONS = List.of(OFFLINE_ACTION, SKIP_TESTS_ACTION);

    static final List<String> IDE_PROJECT_ACTIONS =
        List.of("Maven.Reimport", "Maven.DownloadAllGroupPopup", "Maven.AddManagedFiles");

    static final List<String> IDE_SETTINGS_ACTIONS =
        List.of("Maven.OpenSettingsXml", "Maven.ShowSettingsGroup");

    private static final String FORCE_CLEAN_ACTION = "MavenPrime.ForceClean";

    private static final String AUTO_REFRESH_ACTION = "MavenPrime.AutoRefreshModel";

    private static final String COLOR_CONSOLE_ACTION = "MavenPrime.ColorConsole";

    private static final String BUILD_PROFILES_ACTION = "MavenPrime.BuildProfiles";

    private static final String USE_DAEMON_ACTION = "MavenPrime.UseDaemon";

    static final List<String> OWN_TOOLBAR_ACTIONS =
        List.of(
            FORCE_CLEAN_ACTION,
            AUTO_REFRESH_ACTION,
            COLOR_CONSOLE_ACTION,
            BUILD_PROFILES_ACTION,
            USE_DAEMON_ACTION);

    private final transient Project project;

    private final transient UiState state;

    private final SimpleColoredComponent contextLabel = new SimpleColoredComponent();

    private final SimpleColoredComponent engineLabel = new SimpleColoredComponent();

    private final SimpleColoredComponent contextSummaryLabel = new SimpleColoredComponent();

    private final JPanel statusBar = new JPanel(new BorderLayout());

    final transient JBTabs tabs;

    private final transient Map<ToolWindowTab, TabInfo> tabInfos = new EnumMap<>(ToolWindowTab.class);

    private final transient TextFieldWithAutoCompletion<String> goalsField;

    private final transient BuildContextPanel contextPanel;

    private final transient GoalsPanel goalsPanel;

    private final transient NavigatorPanel navigatorPanel;

    private final transient BuildProfilePanel profilePanel;

    private final transient RepositoryPanel repositoryPanel;

    private final transient ModelProvenancePanel modelPanel;

    private final transient DaemonCommandsPanel daemonCommands;

    private transient boolean restoringSelection;

    private transient VirtualFile contextFile;

    private transient MavenProject contextModule;

    private transient ModuleContext moduleContext =
        new ModuleContext(null, ModuleContext.Kind.FOLLOWING);

    private transient String pinnedModulePath;

    private transient DistributionSpec daemonResolvedFor;

    private transient boolean daemonResolved;

    public MavenPrimeToolWindowPanel(Project project)
    {
        super(true, true);

        this.project = project;
        this.state = new UiState(project, STATE_PREFIX);
        this.tabs = JBTabsFactory.createTabs(project, this);
        this.pinnedModulePath = state.getText(PINNED_MODULE_KEY, StringUtils.EMPTY);
        this.goalsField = GoalsTextField.create(project);
        this.contextPanel = new BuildContextPanel(project);
        this.goalsPanel = new GoalsPanel(project, this::getContextModule);
        this.navigatorPanel = new NavigatorPanel(project);

        Disposer.register(this, navigatorPanel);

        this.profilePanel =
            new BuildProfilePanel(
                project,
                new CommandAction(
                    MavenPrimeBundle.message("mavenprime.profile.openInEditor"),
                    MavenPrimeBundle.message("mavenprime.profile.openInEditor.description"),
                    AllIcons.Actions.OpenNewTab,
                    () -> BuildProfileEditor.getInstance(project).open()));

        Disposer.register(this, profilePanel);

        this.repositoryPanel = new RepositoryPanel(project, this::getContextModule);

        Disposer.register(this, repositoryPanel);

        this.modelPanel = new ModelProvenancePanel(project, this::getContextModule);

        this.daemonCommands = new DaemonCommandsPanel(project, this::daemonForBuilds);

        Disposer.register(this, modelPanel);
        Disposer.register(this, new ContextFollower(project, this, this::applyContextFile));

        setToolbar(buildToolbar());
        setContent(buildContent());

        reload();

        subscribeToContextChanges();
    }

    @Override
    public void dispose()
    {
    }

    public void reload()
    {
        updateContext();

        goalsPanel.refresh();
        contextPanel.refresh();
    }

    private void subscribeToContextChanges()
    {
        project
            .getMessageBus()
            .connect(this)
            .subscribe(BuildContext.TOPIC, (BuildContext.BuildContextListener) this::buildContextChanged);

        project
            .getMessageBus()
            .connect(this)
            .subscribe(MavenPrimeSettings.TOPIC, (MavenPrimeSettings.SettingsListener) this::refreshTabs);

        project
            .getMessageBus()
            .connect(this)
            .subscribe(
                MavenPrimeConfigService.TOPIC, (MavenPrimeConfigService.ConfigListener) this::reload);

        project
            .getMessageBus()
            .connect(this)
            .subscribe(
                RepositoryInspection.TOPIC,
                (RepositoryInspection.InspectionListener) coordinates ->
                    tabs.select(tabInfoOf(ToolWindowTab.REPOSITORY), true));

        project
            .getMessageBus()
            .connect(this)
            .subscribe(
                ModelStaleness.TOPIC,
                (ModelStaleness.StalenessListener) this::updateStaleMarker);

        MavenImports.onImported(project, this, this::reload);
    }

    private void clearOverrides()
    {
        BuildContext.getInstance(project).clearOverrides();

        contextPanel.refresh();
    }

    private void applyContextFile(VirtualFile file)
    {
        if (file.equals(contextFile))
        {
            return;
        }

        contextFile = file;

        updateContext();
    }

    private void updateContext()
    {
        moduleContext =
            ModuleContext.resolve(
                pinnedModulePath,
                MavenProjects.all(project),
                MavenPrimeActionContext.resolveModule(project, contextFile()));

        contextModule = moduleContext.module();

        updateContextLabel();
        updateEngineLabel();

        navigatorPanel.refresh();
        modelPanel.refresh();
        repositoryPanel.refresh();
    }

    private boolean isPinned()
    {
        return moduleContext.isPinned();
    }

    private void chooseModule()
    {
        List<ModuleChoice> choices = new ArrayList<>();

        choices.add(new ModuleChoice(null, MavenPrimeBundle.message("mavenprime.toolwindow.module.follow")));

        for (MavenProject candidate : MavenProjects.all(project))
        {
            choices.add(new ModuleChoice(candidate, candidate.getDisplayName()));
        }

        JBPopupFactory
            .getInstance()
            .createPopupChooserBuilder(choices)
            .setTitle(MavenPrimeBundle.message("mavenprime.toolwindow.module.choose"))
            .setItemChosenCallback(this::pinModule)
            .createPopup()
            .showUnderneathOf(contextLabel);
    }

    private void pinModule(ModuleChoice choice)
    {
        pinnedModulePath =
            (choice.module() == null) ? StringUtils.EMPTY : choice.module().getFile().getPath();

        state.setText(PINNED_MODULE_KEY, pinnedModulePath, StringUtils.EMPTY);

        updateContext();
    }

    private record ModuleChoice(MavenProject module, String label)
    {
        @Override
        public String toString()
        {
            return label;
        }
    }

    private VirtualFile contextFile()
    {
        if (contextFile != null)
        {
            return contextFile;
        }

        VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();

        return (selected.length == 0) ? null : selected[0];
    }

    private void updateContextLabel()
    {
        contextLabel.clear();

        if (contextModule == null)
        {
            contextLabel.setIcon(isPinned() ? AllIcons.General.Pin_tab : AllIcons.General.Add);
            contextLabel.append(
                MavenPrimeBundle.message(
                    isPinned()
                        ? "mavenprime.toolwindow.pinnedModuleUnavailable"
                        : "mavenprime.toolwindow.noModule"),
                SimpleTextAttributes.GRAYED_ATTRIBUTES);

            return;
        }

        MavenId id = contextModule.getMavenId();

        contextLabel.setIcon(isPinned() ? AllIcons.General.Pin_tab : AllIcons.Nodes.Module);
        contextLabel.append(contextModule.getDisplayName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        contextLabel.append(SEPARATOR + id.getGroupId(), SimpleTextAttributes.GRAYED_ATTRIBUTES);

        if (StringUtils.isNotBlank(id.getVersion()))
        {
            contextLabel.append(SEPARATOR + id.getVersion(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
        }
    }

    private void buildContextChanged()
    {
        refreshTabs();

        contextPanel.refresh();

        updateEngineLabel();
        updateContextSummaryLabel();
    }

    private void updateContextSummaryLabel()
    {
        BuildContext context = BuildContext.getInstance(project);

        int properties = 0;

        for (BuildContextProperty property : context.getProperties())
        {
            if (property.isDefined())
            {
                properties++;
            }
        }

        contextSummaryLabel.clear();
        contextSummaryLabel.append(
            MavenPrimeBundle.message(
                "mavenprime.toolwindow.context.summary",
                Integer.valueOf(context.getProfiles().size()),
                Integer.valueOf(properties)),
            SimpleTextAttributes.GRAYED_ATTRIBUTES);
        contextSummaryLabel.setToolTipText(MavenPrimeBundle.message("mavenprime.toolwindow.context.show"));
    }

    private void showContextTab()
    {
        tabs.select(tabInfoOf(ToolWindowTab.CONTEXT), true);
    }

    private DistributionSpec daemonForBuilds()
    {
        DistributionSpec configured = BuildContext.getInstance(project).getDistribution();

        return configured.isDaemon() ? configured : null;
    }

    private void updateEngineLabel()
    {
        DistributionSpec requested = BuildContext.getInstance(project).getDistribution();

        ApplicationManager
            .getApplication()
            .executeOnPooledThread(
                () ->
                {
                    MavenInstallation resolved =
                        MavenInstallationService.getInstance(project).resolve(requested, null);

                    ApplicationManager
                        .getApplication()
                        .invokeLater(() -> showEngine(resolved), project.getDisposed());
                });
    }

    private void showEngine(MavenInstallation installation)
    {
        engineLabel.clear();
        engineLabel.setIcon(installation.isDaemon() ? MavenPrimeIcons.DAEMON : null);
        engineLabel.append(
            installation.getDisplayName(),
            installation.isDaemon()
                ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    private JComponent buildContent()
    {
        JPanel content = new JPanel(new BorderLayout());

        content.setBorder(JBUI.Borders.emptyTop(UIUtil.DEFAULT_VGAP));
        content.add(buildTabs(), BorderLayout.CENTER);
        content.add(buildStatusBar(), BorderLayout.SOUTH);

        return content;
    }

    private JComponent buildStatusBar()
    {
        contextLabel.setBorder(JBUI.Borders.empty(4, 8));
        contextLabel.setToolTipText(MavenPrimeBundle.message("mavenprime.toolwindow.module.tooltip"));
        contextLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        new ClickListener()
        {
            @Override
            public boolean onClick(MouseEvent event, int clickCount)
            {
                chooseModule();

                return true;
            }
        }.installOn(contextLabel);

        engineLabel.setBorder(JBUI.Borders.empty(4, 8));
        engineLabel.setToolTipText(MavenPrimeBundle.message("mavenprime.toolwindow.engine.tooltip"));
        engineLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        new ClickListener()
        {
            @Override
            public boolean onClick(MouseEvent event, int clickCount)
            {
                return contextPanel.editEnvironment();
            }
        }.installOn(engineLabel);

        contextSummaryLabel.setBorder(JBUI.Borders.empty(4, 8));
        contextSummaryLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        new ClickListener()
        {
            @Override
            public boolean onClick(MouseEvent event, int clickCount)
            {
                showContextTab();

                return true;
            }
        }.installOn(contextSummaryLabel);

        updateContextSummaryLabel();

        statusBar.setBorder(JBUI.Borders.customLineTop(JBColor.border()));
        statusBar.add(contextLabel, BorderLayout.WEST);
        statusBar.add(contextSummaryLabel, BorderLayout.CENTER);
        statusBar.add(engineLabel, BorderLayout.EAST);
        statusBar.setVisible(state.isEnabled(CONTEXT_KEY, true));

        return statusBar;
    }

    private JComponent buildTabs()
    {
        for (ToolWindowTab tab : ToolWindowTab.values())
        {
            tabs.addTab(tabInfoOf(tab));
        }

        restoreTabs();

        tabs.addListener(
            new TabsListener()
            {
                @Override
                public void selectionChanged(TabInfo oldSelection, TabInfo newSelection)
                {
                    if (!restoringSelection)
                    {
                        persistSelectedTab();
                    }
                }
            },
            this);

        updateStaleMarker();

        return tabs.getComponent();
    }

    private void restoreTabs()
    {
        List<ToolWindowTab> visible = visibleTabs();

        ToolWindowTab persisted = state.getMode(TAB_KEY, ToolWindowTab.GOALS);

        applyTabs(visible, visible.contains(persisted) ? persisted : visible.get(0));
    }

    void updateStaleMarker()
    {
        boolean pending = ModelStaleness.getInstance(project).isStale();

        TabInfo model = tabInfoOf(ToolWindowTab.MODEL);

        model.setIcon(pending ? AllIcons.General.Modified : null);
        model.setTooltipText(pending ? MavenPrimeBundle.message("mavenprime.model.stale") : null);
    }

    private void refreshTabs()
    {
        updateStaleMarker();

        List<ToolWindowTab> visible = visibleTabs();

        ToolWindowTab current = selectedTab();

        applyTabs(visible, ((current != null) && visible.contains(current)) ? current : visible.get(0));
    }

    private void applyTabs(List<ToolWindowTab> visible, ToolWindowTab wanted)
    {
        restoringSelection = true;

        try
        {
            for (ToolWindowTab tab : ToolWindowTab.values())
            {
                tabInfoOf(tab).setHidden(!visible.contains(tab));
            }

            tabs.select(tabInfoOf(wanted), false);
        }
        finally
        {
            restoringSelection = false;
        }
    }

    private TabInfo tabInfoOf(ToolWindowTab tab)
    {
        return tabInfos.computeIfAbsent(
            tab,
            absent -> new TabInfo(componentOf(absent)).setText(absent.getTitle()).setObject(absent));
    }

    private List<ToolWindowTab> visibleTabs()
    {
        return ToolWindowTab.visible(buildsOnADaemon());
    }

    private boolean buildsOnADaemon()
    {
        DistributionSpec declared = BuildContext.getInstance(project).getDistribution();

        if (declared.isDaemon())
        {
            return true;
        }

        if (!declared.equals(daemonResolvedFor))
        {
            resolveDaemonInBackground(declared);
        }

        return daemonResolved;
    }

    private void resolveDaemonInBackground(DistributionSpec declared)
    {
        daemonResolvedFor = declared;

        ApplicationManager
            .getApplication()
            .executeOnPooledThread(
                () ->
                {
                    boolean daemon =
                        MavenInstallationService.getInstance(project).resolve(declared, null).isDaemon();

                    ApplicationManager
                        .getApplication()
                        .invokeLater(
                            () -> applyDaemonResolution(declared, daemon),
                            ModalityState.any(),
                            project.getDisposed());
                });
    }

    private void applyDaemonResolution(DistributionSpec declared, boolean daemon)
    {
        if (declared.equals(daemonResolvedFor) && (daemon != daemonResolved))
        {
            daemonResolved = daemon;

            refreshTabs();
        }
    }

    private void persistSelectedTab()
    {
        ToolWindowTab selected = selectedTab();

        if (selected != null)
        {
            state.setMode(TAB_KEY, selected);
        }
    }

    private ToolWindowTab selectedTab()
    {
        TabInfo selected = tabs.getSelectedInfo();

        return (selected == null) ? null : (ToolWindowTab) selected.getObject();
    }

    private JComponent componentOf(ToolWindowTab tab)
    {
        return switch (tab)
        {
            case CONTEXT -> contextPanel;
            case GOALS -> goalsPanel;
            case NAVIGATOR -> navigatorPanel;
            case MODEL -> modelPanel;
            case PROFILER -> profilePanel;
            case REPOSITORY -> repositoryPanel;
            case DAEMONS -> daemonCommands;
        };
    }

    private JComponent buildToolbar()
    {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(executeAction(ExecutionMode.RUN));
        group.add(executeAction(ExecutionMode.DEBUG));
        group.addSeparator();
        group.add(new ResetToDefaultsAction());
        group.addSeparator();
        RegisteredActions.add(group, OFFLINE_ACTION);
        RegisteredActions.add(group, FORCE_CLEAN_ACTION);
        RegisteredActions.add(group, SKIP_TESTS_ACTION);
        RegisteredActions.add(group, BUILD_PROFILES_ACTION);
        group.add(new ShowContextAction());
        group.addSeparator();
        RegisteredActions.addAll(group, IDE_PROJECT_ACTIONS);
        group.add(new RefreshAction());
        RegisteredActions.add(group, AUTO_REFRESH_ACTION);
        RegisteredActions.add(group, COLOR_CONSOLE_ACTION);
        RegisteredActions.add(group, USE_DAEMON_ACTION);
        group.addSeparator();
        RegisteredActions.addAll(group, IDE_SETTINGS_ACTIONS);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, group, true);

        toolbar.setTargetComponent(this);

        JPanel bar = new JPanel(new BorderLayout());

        bar.add(toolbar.getComponent(), BorderLayout.NORTH);
        bar.add(buildGoalsBar(), BorderLayout.CENTER);

        return bar;
    }

    private JComponent buildGoalsBar()
    {
        JPanel bar = new JPanel(new BorderLayout(JBUI.scale(6), 0));

        bar.setBorder(JBUI.Borders.empty(2, 6));
        bar.add(new JBLabel(MavenPrimeBundle.message("mavenprime.field.goals")), BorderLayout.WEST);
        bar.add(goalsField, BorderLayout.CENTER);
        bar.add(buildCreateGoalButton(), BorderLayout.EAST);

        return bar;
    }

    private JComponent buildCreateGoalButton()
    {
        ActionToolbar toolbar =
            ActionManager
                .getInstance()
                .createActionToolbar(TOOLBAR_PLACE, new DefaultActionGroup(new CreateGoalAction()), true);

        toolbar.setTargetComponent(goalsField);

        return toolbar.getComponent();
    }

    private MavenProject getContextModule()
    {
        return contextModule;
    }

    private MavenPrimeRequest currentRequest()
    {
        MavenPrimeRequest current = new MavenPrimeRequest();

        current.setGoalLine(goalsField.getText());
        current.workingDirectory = (contextModule == null) ? null : contextModule.getDirectory();

        return current;
    }

    private void execute(ExecutionMode mode)
    {
        MavenPrimeExecutor.getInstance(project).execute(currentRequest(), mode);
    }

    private void saveAsGoal()
    {
        GoalDefinition definition = new GoalDefinition();

        definition.template = currentRequest();
        definition.template.workingDirectory = null;

        GoalEditorDialog dialog = new GoalEditorDialog(project, definition, GoalScope.PROJECT);

        if (dialog.showAndGet())
        {
            String added = GoalRegistry.getInstance(project).add(dialog.getScope(), dialog.getDefinition());

            goalsPanel.refresh();
            goalsPanel.reveal(added);
        }
    }

    private ExecuteGoalAction executeAction(ExecutionMode mode)
    {
        return new ExecuteGoalAction(mode, () -> getContextModule() != null, this::execute);
    }

    private final class CreateGoalAction
        extends DumbAwareAction
    {
        CreateGoalAction()
        {
            super(
                MavenPrimeBundle.message("mavenprime.action.createGoal"),
                MavenPrimeBundle.message("mavenprime.action.createGoal.description"),
                AllIcons.General.Add);
        }

        @Override
        public void actionPerformed(AnActionEvent event)
        {
            saveAsGoal();
        }

        @Override
        public ActionUpdateThread getActionUpdateThread()
        {
            return ActionUpdateThread.EDT;
        }
    }

    private final class ShowContextAction
        extends ToggleAction
        implements DumbAware
    {
        ShowContextAction()
        {
            super(
                MavenPrimeBundle.message("mavenprime.action.showContext"),
                MavenPrimeBundle.message("mavenprime.action.showContext.description"),
                AllIcons.Nodes.Module);
        }

        @Override
        public boolean isSelected(AnActionEvent event)
        {
            return statusBar.isVisible();
        }

        @Override
        public void setSelected(AnActionEvent event, boolean selected)
        {
            state.setEnabled(CONTEXT_KEY, selected, true);

            statusBar.setVisible(selected);
        }

        @Override
        public ActionUpdateThread getActionUpdateThread()
        {
            return ActionUpdateThread.EDT;
        }
    }

    private final class ResetToDefaultsAction
        extends DumbAwareAction
    {
        ResetToDefaultsAction()
        {
            super(
                MavenPrimeBundle.message("mavenprime.action.resetDefaults"),
                MavenPrimeBundle.message(
                    "mavenprime.action.resetDefaults.description", MavenPrimeConfigService.FILE_NAME),
                AllIcons.General.Reset);
        }

        @Override
        public void update(AnActionEvent event)
        {
            event.getPresentation().setEnabled(BuildContext.getInstance(project).hasSharedContext());
        }

        @Override
        public void actionPerformed(AnActionEvent event)
        {
            MavenPrimeConfigService.getInstance(project).invalidate();

            clearOverrides();
        }

        @Override
        public ActionUpdateThread getActionUpdateThread()
        {
            return ActionUpdateThread.BGT;
        }
    }

    private final class RefreshAction
        extends DumbAwareAction
    {
        RefreshAction()
        {
            super(MavenPrimeBundle.message("mavenprime.action.refresh"), null, AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(AnActionEvent event)
        {
            MavenInstallationService.getInstance(project).invalidate();

            BuildContext.getInstance(project).forceReimport();

            daemonResolvedFor = null;

            refreshTabs();

            reload();
        }

        @Override
        public ActionUpdateThread getActionUpdateThread()
        {
            return ActionUpdateThread.EDT;
        }
    }
}
