package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.dependencies.AnalyzerInspection;
import org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyInserter;
import org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyXml;
import org.bitstrings.idea.plugins.mavenprime.dependencies.PomDependencies;
import org.bitstrings.idea.plugins.mavenprime.dependencies.PomPlugins;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCacheStatus;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCandidates;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactKind;
import org.bitstrings.idea.plugins.mavenprime.repository.LocalRepositoryDoctor;
import org.bitstrings.idea.plugins.mavenprime.repository.LocalRepositoryIndex;
import org.bitstrings.idea.plugins.mavenprime.repository.LocalRepositoryLocator;
import org.bitstrings.idea.plugins.mavenprime.repository.ProjectRepositories;
import org.bitstrings.idea.plugins.mavenprime.repository.RemoteArtifactIndex;
import org.bitstrings.idea.plugins.mavenprime.repository.RemoteProbeResult;
import org.bitstrings.idea.plugins.mavenprime.repository.RemoteRepositoryQuery;
import org.bitstrings.idea.plugins.mavenprime.repository.RepositoryCredentialStore;
import org.bitstrings.idea.plugins.mavenprime.repository.RepositoryCredentials;
import org.bitstrings.idea.plugins.mavenprime.repository.RepositoryDiagnosis;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionData;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionLog;
import org.bitstrings.idea.plugins.mavenprime.ui.ArtifactCoordinatesRenderer;
import org.bitstrings.idea.plugins.mavenprime.ui.BuildSelector;
import org.bitstrings.idea.plugins.mavenprime.ui.CompletenessBanner;
import org.bitstrings.idea.plugins.mavenprime.ui.CommandAction;
import org.bitstrings.idea.plugins.mavenprime.ui.RepositoryCredentialsDialog;
import org.bitstrings.idea.plugins.mavenprime.util.FileReveal;
import org.bitstrings.idea.plugins.mavenprime.util.UiState;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenRemoteRepository;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.intellij.util.xml.DomElement;

public final class RepositoryPanel
    extends JPanel
    implements Disposable, UiDataProvider
{
    private static final long serialVersionUID = 1L;

    private static final String TOOLBAR_PLACE = "MavenPrimeRepository";

    private static final String POPUP_PLACE = "MavenPrimeRepositoryPopup";

    private static final float DEFAULT_SPLIT = 0.45f;

    private static final String SPLITTER_KEY = "MavenPrime.Repository.Splitter";

    private static final String STATE_PREFIX = "mavenPrime.repository.";

    private static final String SOURCE_KEY = "source";

    private static final String DIAGNOSIS_TASK = "diagnosis";

    private static final int SEARCH_DELAY_MILLIS = 250;

    private static final int SEARCH_COLUMNS = 24;

    private final transient Project project;

    private final transient Supplier<MavenProject> contextModule;

    private final transient UiState state;

    private final ComboBox<RepositorySource> sourceCombo = new ComboBox<>(RepositorySource.values());

    private final transient BuildSelector<ResolutionData> buildSelector;

    private final SimpleColoredComponent incomplete = new SimpleColoredComponent();

    private final SearchTextField searchField = new SearchTextField();

    private final transient CollectionListModel<ArtifactCoordinates> results = new CollectionListModel<>();

    private final JBList<ArtifactCoordinates> resultList = new JBList<>(results);

    private final transient RepositoryDetailsView details;

    private final transient Timer searchTimer = new Timer(SEARCH_DELAY_MILLIS, event -> reloadResults());

    private transient volatile Path localRepository;

    public RepositoryPanel(Project project, Supplier<MavenProject> contextModule)
    {
        super(new BorderLayout());

        this.project = project;
        this.contextModule = contextModule;
        this.state = new UiState(project, STATE_PREFIX);
        this.buildSelector =
            new BuildSelector<>(entry -> ResolutionLog.getInstance(project).select(entry));
        this.details = new RepositoryDetailsView(project);

        searchTimer.setRepeats(false);

        sourceCombo.setSelectedItem(state.getMode(SOURCE_KEY, RepositorySource.SEARCH));

        syncBuildSelector();

        add(buildNorth(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        subscribe();

        sourceCombo.addActionListener(event -> applySource());

        locateRepository(false);
    }

    @Override
    public void dispose()
    {
        searchTimer.stop();
    }

    @Override
    public void uiDataSnapshot(DataSink sink)
    {
        VirtualFile file = selectedFile();

        if (file == null)
        {
            return;
        }

        sink.set(CommonDataKeys.VIRTUAL_FILE, file);
        sink.set(CommonDataKeys.VIRTUAL_FILE_ARRAY, new VirtualFile[] { file });

        VirtualFile pom = fileAt(pomFile());

        if (pom != null)
        {
            sink.set(CommonDataKeys.NAVIGATABLE, new OpenFileDescriptor(project, pom));
        }
    }

    public void refresh()
    {
        if ((source() == RepositorySource.MODULE) || (source() == RepositorySource.PLUGINS))
        {
            reloadResults();
        }
    }

    private void subscribe()
    {
        MessageBusConnection connection = project.getMessageBus().connect(this);

        connection.subscribe(
            ResolutionLog.TOPIC, (ResolutionLog.ResolutionListener) this::onResolutionUpdated);
        connection.subscribe(
            LocalRepositoryIndex.TOPIC, (LocalRepositoryIndex.IndexListener) this::reloadResults);
        connection.subscribe(
            RepositoryInspection.TOPIC, (RepositoryInspection.InspectionListener) this::inspect);
    }

    private JComponent buildNorth()
    {
        incomplete.setBorder(JBUI.Borders.empty(0, 8, 4, 8));

        JPanel north = new JPanel(new BorderLayout());

        north.add(buildSearchBar(), BorderLayout.NORTH);
        north.add(incomplete, BorderLayout.CENTER);

        return north;
    }

    private JComponent buildSearchBar()
    {
        searchField.getTextEditor().setColumns(SEARCH_COLUMNS);
        searchField
            .getTextEditor()
            .getEmptyText()
            .setText(MavenPrimeBundle.message("mavenprime.repository.coordinates.empty"));
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

        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));

        modes.add(sourceCombo);
        modes.add(buildSelector.getComponent());

        JPanel bar = new JPanel(new BorderLayout(JBUI.scale(6), 0));

        bar.setBorder(JBUI.Borders.empty(4, 6));
        bar.add(modes, BorderLayout.WEST);
        bar.add(searchField, BorderLayout.CENTER);
        bar.add(buildActions(), BorderLayout.EAST);

        return bar;
    }

    private JComponent buildActions()
    {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(checkRemoteAction());
        group.add(setCredentialsAction());
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.repository.reindex"),
                MavenPrimeBundle.message("mavenprime.repository.reindex.description"),
                AllIcons.Actions.Refresh,
                () -> locateRepository(true),
                () -> !LocalRepositoryIndex.getInstance(project).isScanning()));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.repository.purge.title"),
                MavenPrimeBundle.message("mavenprime.repository.purge.description"),
                AllIcons.Actions.GC,
                this::purge,
                this::hasNegativeCache));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, group, true);

        toolbar.setTargetComponent(this);

        return toolbar.getComponent();
    }

    private DefaultActionGroup buildContextMenu()
    {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.repository.action.openPom"),
                MavenPrimeBundle.message("mavenprime.repository.action.openPom.description"),
                AllIcons.FileTypes.Xml,
                this::openPom,
                () -> pomFile() != null));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.action.navigate"),
                MavenPrimeBundle.message("mavenprime.repository.action.declaration.description"),
                AllIcons.Actions.EditSource,
                this::goToDeclaration,
                () -> (selection() != null) && (contextModule.get() != null)));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.repository.action.add"),
                MavenPrimeBundle.message("mavenprime.repository.action.add.description"),
                AllIcons.General.Add,
                this::addDependency,
                () ->
                    (selection() != null)
                        && (contextModule.get() != null)
                        && (source() != RepositorySource.PLUGINS)));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.repository.action.analyze"),
                MavenPrimeBundle.message("mavenprime.repository.action.analyze.description"),
                AllIcons.Actions.ShowAsTree,
                this::analyze,
                () -> (selection() != null) && AnalyzerInspection.isAvailable(contextModule.get())));
        group.addSeparator();
        group.add(checkRemoteAction());
        group.add(setCredentialsAction());
        group.add(
            new CommandAction(
                RevealFileAction.getActionName(),
                MavenPrimeBundle.message("mavenprime.repository.action.reveal.description"),
                AllIcons.Actions.MenuOpen,
                this::reveal,
                () -> RevealFileAction.isSupported() && (details.getDiagnosis() != null)));
        group.addSeparator();
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.action.copy"),
                null,
                AllIcons.Actions.Copy,
                this::copyCoordinates,
                () -> selection() != null));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.dependencies.action.copyXml"),
                MavenPrimeBundle.message("mavenprime.dependencies.action.copyXml.description"),
                AllIcons.Actions.ListFiles,
                this::copyAsXml,
                () -> selection() != null));

        return group;
    }

    private CommandAction checkRemoteAction()
    {
        return new CommandAction(
            MavenPrimeBundle.message("mavenprime.repository.action.remote"),
            MavenPrimeBundle.message("mavenprime.repository.action.remote.description"),
            AllIcons.Actions.Download,
            this::checkRemote,
            () -> selection() != null);
    }

    private CommandAction setCredentialsAction()
    {
        return new CommandAction(
            MavenPrimeBundle.message("mavenprime.repository.action.credentials"),
            MavenPrimeBundle.message("mavenprime.repository.action.credentials.description"),
            AllIcons.General.User,
            this::setCredentials,
            () -> details.firstUnauthorized() != null);
    }

    private boolean hasNegativeCache()
    {
        RepositoryDiagnosis current = details.getDiagnosis();

        return (current != null) && !current.status().negativeCacheFiles().isEmpty();
    }

    private Path pomFile()
    {
        RepositoryDiagnosis current = details.getDiagnosis();

        return (current == null) ? null : current.files().pom();
    }

    private void addDependency()
    {
        ArtifactCoordinates selected = selection();

        MavenProject module = contextModule.get();

        if ((selected != null) && (module != null))
        {
            DependencyInserter.insert(project, module, selected);
        }
    }

    private void analyze()
    {
        ArtifactCoordinates selected = selection();

        if (selected != null)
        {
            AnalyzerInspection.analyze(project, contextModule.get(), selected);
        }
    }

    private void reveal()
    {
        RepositoryDiagnosis current = details.getDiagnosis();

        FileReveal.reveal((current == null) ? null : current.files().presentableFile());
    }

    private void copyCoordinates()
    {
        ArtifactCoordinates selected = selection();

        if (selected != null)
        {
            CopyPasteManager.copyTextToClipboard(selected.shortForm());
        }
    }

    private void copyAsXml()
    {
        ArtifactCoordinates selected = selection();

        if (selected != null)
        {
            CopyPasteManager.copyTextToClipboard(DependencyXml.of(selected));
        }
    }

    private OnePixelSplitter buildBody()
    {
        resultList.setCellRenderer(new ArtifactCoordinatesRenderer());
        resultList.addListSelectionListener(
            event ->
            {
                if (!event.getValueIsAdjusting())
                {
                    diagnoseSelection();
                }
            });

        ListSpeedSearch.installOn(resultList, ArtifactCoordinates::shortForm);

        PopupHandler.installPopupMenu(resultList, buildContextMenu(), POPUP_PLACE);

        new DoubleClickListener()
        {
            @Override
            protected boolean onDoubleClick(MouseEvent event)
            {
                return openPom();
            }
        }.installOn(resultList);

        OnePixelSplitter splitter = new OnePixelSplitter(true, SPLITTER_KEY, DEFAULT_SPLIT);

        splitter.setHonorComponentsMinimumSize(false);

        splitter.setFirstComponent(new JBScrollPane(resultList));
        splitter.setSecondComponent(new JBScrollPane(details.getComponent()));

        return splitter;
    }

    private void applySource()
    {
        state.setMode(SOURCE_KEY, source());

        syncBuildSelector();

        reloadResults();
    }

    private void syncBuildSelector()
    {
        buildSelector.setApplicable(source().isFromResolutionLog());
        buildSelector.reload(resolutionLog().getBuilds(), resolutionLog().getSelectedBuild());

        if (source().isFromResolutionLog())
        {
            CompletenessBanner.render(incomplete, resolutionLog().getCompleteness());
        }
        else
        {
            CompletenessBanner.hide(incomplete);
        }
    }

    private void onResolutionUpdated()
    {
        syncBuildSelector();

        if (source().isFromResolutionLog())
        {
            reloadResults();
        }
    }

    private void inspect(ArtifactCoordinates coordinates)
    {
        searchField.setText(coordinates.shortForm());
        sourceCombo.setSelectedItem(RepositorySource.SEARCH);

        searchTimer.stop();

        reloadResults();
    }

    private void locateRepository(boolean reindex)
    {
        ReadAction
            .nonBlocking(() -> LocalRepositoryLocator.locate(project))
            .expireWith(this)
            .finishOnUiThread(ModalityState.any(), located -> applyRepository(located, reindex))
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void applyRepository(Path located, boolean reindex)
    {
        localRepository = located;

        LocalRepositoryIndex index = LocalRepositoryIndex.getInstance(project);

        if (reindex)
        {
            index.rebuild(located);
        }
        else
        {
            index.ensureIndexed(located);
        }

        reloadResults();
    }

    private void reloadResults()
    {
        switch (source())
        {
            case SEARCH -> reloadFromLocalIndex();
            case INDEX -> reloadFromMavenIndex();
            case MODULE -> showResults(filteredBySearch(moduleDependencies()));
            case PLUGINS -> showResults(filteredBySearch(modulePlugins()));
            case FAILURES, DOWNLOADS ->
                showResults(
                    filteredBySearch(
                        ArtifactCandidates.ofResolutionEvents(source().recordedIn(resolutionLog()))));
        }
    }

    private void reloadFromLocalIndex()
    {
        String needle = searchField.getText();

        reloadAsync(
            () -> LocalRepositoryIndex.getInstance(project).search(needle, LocalRepositoryIndex.DEFAULT_LIMIT),
            RepositorySource.SEARCH);
    }

    private void reloadFromMavenIndex()
    {
        String needle = StringUtils.trimToEmpty(searchField.getText());

        if (needle.isEmpty())
        {
            showResults(List.of());

            return;
        }

        reloadAsync(
            () -> RemoteArtifactIndex.search(project, needle, LocalRepositoryIndex.DEFAULT_LIMIT),
            RepositorySource.INDEX);
    }

    private void reloadAsync(Supplier<List<ArtifactCoordinates>> query, RepositorySource key)
    {
        resultList.getEmptyText().setText(MavenPrimeBundle.message("mavenprime.repository.searching"));

        ReadAction
            .nonBlocking(query::get)
            .expireWith(this)
            .coalesceBy(this, key)
            .finishOnUiThread(ModalityState.any(), found -> showResultsOf(key, found))
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void showResultsOf(RepositorySource key, List<ArtifactCoordinates> found)
    {
        if (key != source())
        {
            return;
        }

        showResults(found);
    }

    private void showResults(List<ArtifactCoordinates> found)
    {
        ArtifactCoordinates previous = selection();

        results.replaceAll(found);

        resultList.getEmptyText().setText(emptyText());

        if (found.isEmpty())
        {
            showHint();

            return;
        }

        resultList.setSelectedIndex(selectionIndexOf(found, previous));
    }

    static int selectionIndexOf(List<ArtifactCoordinates> found, ArtifactCoordinates previous)
    {
        return (previous == null) ? 0 : Math.max(0, found.indexOf(previous));
    }

    private List<ArtifactCoordinates> moduleDependencies()
    {
        MavenProject module = contextModule.get();

        return (module == null) ? List.of() : ArtifactCandidates.ofArtifacts(module.getDependencies());
    }

    private List<ArtifactCoordinates> modulePlugins()
    {
        MavenProject module = contextModule.get();

        return (module == null) ? List.of() : ArtifactCandidates.ofPlugins(module.getPlugins());
    }

    private ResolutionLog resolutionLog()
    {
        return ResolutionLog.getInstance(project);
    }

    private List<ArtifactCoordinates> filteredBySearch(List<ArtifactCoordinates> candidates)
    {
        return ArtifactCandidates.matching(candidates, searchField.getText());
    }

    private String emptyText()
    {
        LocalRepositoryIndex index = LocalRepositoryIndex.getInstance(project);

        if ((source() == RepositorySource.SEARCH) && index.isScanning())
        {
            return MavenPrimeBundle.message("mavenprime.repository.indexing");
        }

        return source().getEmptyText();
    }

    private void diagnoseSelection()
    {
        ArtifactCoordinates selected = selection();

        Path repository = localRepository;

        if ((selected == null) || (repository == null))
        {
            showHint();

            return;
        }

        if (!selected.equals(details.getRemoteSubject()))
        {
            details.clearRemote();
        }

        ReadAction
            .nonBlocking(
                () ->
                    RepositoryDiagnosis.of(
                        repository, selected, RemoteArtifactIndex.versionsOf(project, selected)))
            .expireWith(this)
            .coalesceBy(this, DIAGNOSIS_TASK)
            .finishOnUiThread(ModalityState.any(), inspected -> showDiagnosis(selected, inspected))
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void showHint()
    {
        details.showHint();
    }

    private void showDiagnosis(ArtifactCoordinates subject, RepositoryDiagnosis inspected)
    {
        if (subject.equals(selection()))
        {
            details.show(inspected);
        }
    }

    private void setCredentials()
    {
        RemoteProbeResult unauthorized = details.firstUnauthorized();

        if (unauthorized == null)
        {
            return;
        }

        RepositoryCredentialsDialog dialog =
            new RepositoryCredentialsDialog(project, unauthorized.repositoryId());

        if (!dialog.showAndGet())
        {
            return;
        }

        RepositoryCredentialStore
            .getInstance(project)
            .store(unauthorized.repositoryId(), dialog.getUsername(), dialog.getPassword());

        checkRemote();
    }

    private RepositorySource source()
    {
        RepositorySource selected = (RepositorySource) sourceCombo.getSelectedItem();

        return (selected == null) ? RepositorySource.SEARCH : selected;
    }

    private ArtifactCoordinates selection()
    {
        return resultList.getSelectedValue();
    }

    private VirtualFile selectedFile()
    {
        RepositoryDiagnosis current = details.getDiagnosis();

        return fileAt((current == null) ? null : current.files().presentableFile());
    }

    private VirtualFile fileAt(Path path)
    {
        return (path == null) ? null : LocalFileSystem.getInstance().findFileByNioFile(path);
    }

    private boolean openPom()
    {
        VirtualFile pom = fileAt(pomFile());

        if (pom == null)
        {
            return false;
        }

        new OpenFileDescriptor(project, pom).navigate(true);

        return true;
    }

    private void checkRemote()
    {
        ArtifactCoordinates selected = selection();

        if (selected == null)
        {
            return;
        }

        ArtifactKind kind = source().getKind();

        MavenProject module = contextModule.get();

        ProgressManager
            .getInstance()
            .run(
                new Task.Backgroundable(
                    project, MavenPrimeBundle.message("mavenprime.repository.remote.progress"), true)
                {
                    @Override
                    public void run(ProgressIndicator indicator)
                    {
                        queryRemote(selected, module, kind);
                    }
                });
    }

    private void queryRemote(ArtifactCoordinates selected, MavenProject module, ArtifactKind kind)
    {
        List<MavenRemoteRepository> repositories = ProjectRepositories.effective(project, module, kind);

        RepositoryCredentialStore store = RepositoryCredentialStore.getInstance(project);

        Map<String, RepositoryCredentials> resolved = new HashMap<>();

        Function<MavenRemoteRepository, RepositoryCredentials> credentials =
            repository ->
                resolved.computeIfAbsent(
                    StringUtils.defaultString(repository.getId()), store::forServer);

        List<RemoteProbeResult> probes = RemoteRepositoryQuery.probe(repositories, selected, credentials);
        List<String> versions = RemoteRepositoryQuery.versions(repositories, selected, credentials);

        ApplicationManager
            .getApplication()
            .invokeLater(
                () ->
                {
                    if (selected.equals(selection()))
                    {
                        details.setRemote(selected, probes, versions);

                        diagnoseSelection();
                    }
                },
                project.getDisposed());
    }

    private MavenDomProjectModel contextModel()
    {
        MavenProject module = contextModule.get();

        return (module == null) ? null : PomDependencies.modelOf(project, module);
    }

    private void goToDeclaration()
    {
        ArtifactCoordinates selected = selection();

        MavenDomProjectModel model = contextModel();

        if ((selected == null) || (model == null))
        {
            return;
        }

        if (!PomDependencies.navigateTo(declarationOf(model, selected)))
        {
            MavenPrimeNotifications.warning(
                project,
                MavenPrimeBundle.message("mavenprime.dependencies.notDeclared", selected.shortForm()));
        }
    }

    private DomElement declarationOf(MavenDomProjectModel model, ArtifactCoordinates selected)
    {
        return (source().getKind() == ArtifactKind.PLUGIN)
            ? PomPlugins.findDeclaration(model, selected.groupId(), selected.artifactId())
            : PomDependencies.findDeclaration(model, selected.groupId(), selected.artifactId());
    }

    private void purge()
    {
        RepositoryDiagnosis current = details.getDiagnosis();

        if (current == null)
        {
            return;
        }

        ArtifactCacheStatus status = current.status();

        List<Path> markers = status.negativeCacheFiles();

        if (markers.isEmpty())
        {
            MavenPrimeNotifications.info(
                project, MavenPrimeBundle.message("mavenprime.repository.purge.nothing"));

            return;
        }

        int confirmation =
            Messages.showOkCancelDialog(
                project,
                MavenPrimeBundle.message(
                    "mavenprime.repository.purge.confirm",
                    Integer.valueOf(markers.size()),
                    status.directory()),
                MavenPrimeBundle.message("mavenprime.repository.purge.title"),
                MavenPrimeBundle.message("mavenprime.repository.purge.ok"),
                MavenPrimeBundle.message("mavenprime.repository.purge.cancel"),
                Messages.getWarningIcon());

        if (confirmation != Messages.OK)
        {
            return;
        }

        ProgressManager
            .getInstance()
            .run(
                new Task.Backgroundable(
                    project, MavenPrimeBundle.message("mavenprime.repository.purge.title"))
                {
                    @Override
                    public void run(ProgressIndicator indicator)
                    {
                        purgeMarkers(status);
                    }
                });
    }

    private void purgeMarkers(ArtifactCacheStatus status)
    {
        try
        {
            MavenPrimeNotifications.info(
                project,
                MavenPrimeBundle.message(
                    "mavenprime.repository.purge.done",
                    Integer.valueOf(LocalRepositoryDoctor.purge(status))));

            ApplicationManager
                .getApplication()
                .invokeLater(this::diagnoseSelection, project.getDisposed());
        }
        catch (IOException failure)
        {
            MavenPrimeNotifications.error(
                project,
                MavenPrimeBundle.message("mavenprime.repository.purge.failed", failure.getMessage()));
        }
    }
}
