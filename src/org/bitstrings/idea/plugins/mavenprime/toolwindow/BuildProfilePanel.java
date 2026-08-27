package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.DownloadSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileGrouping;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileSort;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileView;
import org.bitstrings.idea.plugins.mavenprime.ui.BuildSelector;
import org.bitstrings.idea.plugins.mavenprime.ui.CommandAction;
import org.bitstrings.idea.plugins.mavenprime.ui.CompletenessBanner;
import org.bitstrings.idea.plugins.mavenprime.ui.ViewToggleAction;
import org.bitstrings.idea.plugins.mavenprime.util.UiState;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

public final class BuildProfilePanel
    extends JPanel
    implements Disposable
{
    private static final long serialVersionUID = 1L;

    private static final String SEPARATOR = " · ";

    private static final SimpleTextAttributes EXPLAIN =
        new SimpleTextAttributes(SimpleTextAttributes.STYLE_UNDERLINE, null);

    private static final String TOOLBAR_PLACE = "MavenPrimeProfiler";

    private static final String SPLITTER_KEY = "MavenPrime.Profiler.Splitter";

    private static final String BOTTOM_SPLITTER_KEY = "MavenPrime.Profiler.BottomSplitter";

    private static final float DEFAULT_SPLIT = 0.5f;

    private static final float DEFAULT_BOTTOM_SPLIT = 0.65f;

    private static final int FILTER_COLUMNS = 18;

    private static final String STATE_PREFIX = "mavenPrime.profiler.";

    static final String SUMMARY_KEY = "showSummary";

    static final String TIMELINE_KEY = "showTimeline";

    static final String DETAILS_KEY = "showDetails";

    static final String DOWNLOADS_KEY = "showDownloads";

    static final String GROUPING_KEY = "grouping";

    static final String SORT_KEY = "sort";

    static final String DESCENDING_KEY = "descending";

    private final transient Project project;

    private final SimpleColoredComponent incomplete = new SimpleColoredComponent();

    final SimpleColoredComponent metrics = new SimpleColoredComponent();

    private transient BuildProfile shownProfile = new BuildProfile();

    private transient BuildProfileSummary shownSummary = BuildProfileSummary.empty();

    private transient DownloadSummary shownDownloads = DownloadSummary.empty();

    private transient ProfileView view;

    private transient boolean updating;

    private final BuildTimeline timeline = new BuildTimeline();

    private final ProfileTable table = new ProfileTable();

    private final DownloadTable downloadTable = new DownloadTable();

    private final ComboBox<ProfileGrouping> groupCombo = new ComboBox<>(ProfileGrouping.values());

    private final ComboBox<ProfileSort> sortCombo = new ComboBox<>();

    private final SearchTextField filterField = new SearchTextField(false);

    private final transient UiState state;

    private final OnePixelSplitter splitter = new OnePixelSplitter(true, SPLITTER_KEY, DEFAULT_SPLIT);

    private final OnePixelSplitter bottom =
        new OnePixelSplitter(true, BOTTOM_SPLITTER_KEY, DEFAULT_BOTTOM_SPLIT);

    final JPanel chart = new JPanel(new BorderLayout());

    final JBScrollPane details = new JBScrollPane(table);

    final JBScrollPane downloads = new JBScrollPane(downloadTable);

    final JPanel header = buildHeader();

    final JPanel filters;

    final JComponent toolbar;

    private final transient BuildSelector<BuildProfile> buildSelector;

    public BuildProfilePanel(Project project, AnAction... hostActions)
    {
        super(new BorderLayout());

        this.project = project;
        this.state = new UiState(project, STATE_PREFIX);
        this.view = restoreView();
        this.buildSelector =
            new BuildSelector<>(entry -> BuildProfileService.getInstance(project).select(entry));
        this.toolbar = buildToolbar(hostActions);
        this.filters = buildFilters();

        add(buildNorth(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        timeline.setOnModulePointedAt(table::highlight);
        timeline.setOnModuleChosen(table::select);
        table.setOnModuleSelected(timeline::setSelectedModule);

        project
            .getMessageBus()
            .connect(this)
            .subscribe(BuildProfileService.TOPIC, (BuildProfileService.BuildProfileListener) this::refresh);

        refresh();
    }

    @Override
    public void dispose()
    {
    }

    private ProfileView restoreView()
    {
        ProfileView defaults = ProfileView.defaults();

        ProfileGrouping grouping = state.getMode(GROUPING_KEY, defaults.grouping());
        ProfileSort sort = state.getMode(SORT_KEY, defaults.sort());

        return defaults
            .withGrouping(grouping)
            .withSort(sort.appliesTo(grouping) ? sort : defaults.sort())
            .withDescending(state.isEnabled(DESCENDING_KEY, defaults.descending()));
    }

    // The toolbar owns the toggles, so it cannot live inside a part those toggles hide.
    private JComponent buildNorth()
    {
        JPanel north = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));

        JPanel top = new JPanel(new BorderLayout());

        top.add(toolbar, BorderLayout.WEST);
        top.add(buildSelector.getComponent(), BorderLayout.EAST);

        north.add(top);
        north.add(filters);
        north.add(header);

        return north;
    }

    private JPanel buildFilters()
    {
        groupCombo.setToolTipText(MavenPrimeBundle.message("mavenprime.profile.groupBy.description"));
        sortCombo.setToolTipText(MavenPrimeBundle.message("mavenprime.profile.sortBy.description"));

        applyView();

        groupCombo.addActionListener(event -> onGroupingChosen());
        sortCombo.addActionListener(event -> onSortChosen());

        filterField.getTextEditor().setColumns(FILTER_COLUMNS);
        filterField
            .getTextEditor()
            .getEmptyText()
            .setText(MavenPrimeBundle.message("mavenprime.profile.filter"));
        filterField
            .getTextEditor()
            .getDocument()
            .addDocumentListener(
                new DocumentAdapter()
                {
                    @Override
                    protected void textChanged(DocumentEvent event)
                    {
                        onFilterTyped();
                    }
                });

        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));

        modes.add(groupCombo);
        modes.add(sortCombo);
        modes.add(buildDirectionToolbar());

        JPanel bar = new JPanel(new BorderLayout(JBUI.scale(6), 0));

        bar.setBorder(JBUI.Borders.empty(0, 6, 4, 6));
        bar.add(modes, BorderLayout.WEST);
        bar.add(filterField, BorderLayout.CENTER);

        return bar;
    }

    private JComponent buildDirectionToolbar()
    {
        ActionToolbar direction =
            ActionManager
                .getInstance()
                .createActionToolbar(
                    TOOLBAR_PLACE,
                    new DefaultActionGroup(
                        new ViewToggleAction(
                            MavenPrimeBundle.message("mavenprime.profile.descending"),
                            MavenPrimeBundle.message("mavenprime.profile.descending.description"),
                            AllIcons.General.ArrowDown,
                            () -> view.descending(),
                            descending -> onDirectionChosen(descending.booleanValue()))),
                    true);

        direction.setTargetComponent(this);

        return direction.getComponent();
    }

    private void onGroupingChosen()
    {
        if (updating)
        {
            return;
        }

        view = view.withGrouping(grouping());

        state.setMode(GROUPING_KEY, view.grouping());
        state.setMode(SORT_KEY, view.sort());

        applyView();
        reapplyView();
    }

    private void onSortChosen()
    {
        if (updating)
        {
            return;
        }

        view = view.withSort(sort());

        state.setMode(SORT_KEY, view.sort());
        state.setEnabled(DESCENDING_KEY, view.descending(), true);

        applyView();
        reapplyView();
    }

    private void onDirectionChosen(boolean descending)
    {
        view = view.withDescending(descending);

        state.setEnabled(DESCENDING_KEY, descending, true);

        reapplyView();
    }

    private void onFilterTyped()
    {
        view = view.withFilter(filterField.getText());

        reapplyView();
    }

    private ProfileGrouping grouping()
    {
        Object selected = groupCombo.getSelectedItem();

        return (selected instanceof ProfileGrouping chosen) ? chosen : view.grouping();
    }

    private ProfileSort sort()
    {
        Object selected = sortCombo.getSelectedItem();

        return (selected instanceof ProfileSort chosen) ? chosen : view.sort();
    }

    private void applyView()
    {
        updating = true;

        try
        {
            groupCombo.setSelectedItem(view.grouping());
            sortCombo.setModel(
                new DefaultComboBoxModel<>(
                    Arrays
                        .stream(ProfileSort.values())
                        .filter(candidate -> candidate.appliesTo(view.grouping()))
                        .toArray(ProfileSort[]::new)));
            sortCombo.setSelectedItem(view.sort());
        }
        finally
        {
            updating = false;
        }
    }

    private void reloadBuilds()
    {
        BuildProfileService service = BuildProfileService.getInstance(project);

        buildSelector.reload(service.getBuilds(), service.getSelectedBuild());

        metrics.setToolTipText(BuildSelector.goalsOf(service.getSelectedBuild()));
    }

    private JComponent buildContent()
    {
        chart.add(new JBScrollPane(timeline), BorderLayout.CENTER);

        applyVisibility();

        return splitter;
    }

    private void applyVisibility()
    {
        header.setVisible(isShown(SUMMARY_KEY));
        filters.setVisible(isShown(DETAILS_KEY));

        bottom.setFirstComponent(isShown(DETAILS_KEY) ? details : null);
        bottom.setSecondComponent(isShown(DOWNLOADS_KEY) ? downloads : null);

        splitter.setFirstComponent(isShown(TIMELINE_KEY) ? chart : null);
        splitter.setSecondComponent(
            (isShown(DETAILS_KEY) || isShown(DOWNLOADS_KEY)) ? bottom : null);

        revalidate();
        repaint();
    }

    boolean isShown(String key)
    {
        return state.isEnabled(key, shownByDefault(key));
    }

    void show(String key, boolean shown)
    {
        state.setEnabled(key, shown, shownByDefault(key));

        applyVisibility();
    }

    private static boolean shownByDefault(String key)
    {
        return !DOWNLOADS_KEY.equals(key);
    }

    private JComponent buildToolbar(AnAction... hostActions)
    {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(
            new ViewToggleAction(
                MavenPrimeBundle.message("mavenprime.profile.showSummary"),
                MavenPrimeBundle.message("mavenprime.profile.showSummary.description"),
                AllIcons.General.InspectionsEye,
                () -> isShown(SUMMARY_KEY),
                shown -> show(SUMMARY_KEY, shown.booleanValue())));
        group.add(
            new ViewToggleAction(
                MavenPrimeBundle.message("mavenprime.profile.showTimeline"),
                MavenPrimeBundle.message("mavenprime.profile.showTimeline.description"),
                AllIcons.Actions.ProfileCPU,
                () -> isShown(TIMELINE_KEY),
                shown -> show(TIMELINE_KEY, shown.booleanValue())));
        group.add(
            new ViewToggleAction(
                MavenPrimeBundle.message("mavenprime.profile.showDetails"),
                MavenPrimeBundle.message("mavenprime.profile.showDetails.description"),
                AllIcons.Actions.ShowAsTree,
                () -> isShown(DETAILS_KEY),
                shown -> show(DETAILS_KEY, shown.booleanValue())));
        group.add(
            new ViewToggleAction(
                MavenPrimeBundle.message("mavenprime.profile.showDownloads"),
                MavenPrimeBundle.message("mavenprime.profile.showDownloads.description"),
                AllIcons.Actions.Download,
                () -> isShown(DOWNLOADS_KEY),
                shown -> show(DOWNLOADS_KEY, shown.booleanValue())));
        group.addSeparator();
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.profile.zoomIn"),
                MavenPrimeBundle.message("mavenprime.profile.zoomIn.description"),
                AllIcons.General.ZoomIn,
                timeline::zoomIn,
                () -> isShown(TIMELINE_KEY)));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.profile.zoomOut"),
                MavenPrimeBundle.message("mavenprime.profile.zoomOut.description"),
                AllIcons.General.ZoomOut,
                timeline::zoomOut,
                () -> isShown(TIMELINE_KEY) && (timeline.getZoom() > BuildTimeline.FIT)));
        group.add(
            new CommandAction(
                MavenPrimeBundle.message("mavenprime.profile.zoomFit"),
                MavenPrimeBundle.message("mavenprime.profile.zoomFit.description"),
                AllIcons.General.FitContent,
                timeline::zoomToFit,
                () -> isShown(TIMELINE_KEY) && (timeline.getZoom() > BuildTimeline.FIT)));

        if (hostActions.length > 0)
        {
            group.addSeparator();
            group.addAll(hostActions);
        }

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, group, true);

        toolbar.setTargetComponent(this);

        return toolbar.getComponent();
    }

    private JPanel buildHeader()
    {
        JPanel header =
            new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, JBUI.scale(2), true, false));

        header.setBorder(JBUI.Borders.empty(6, 8));

        metrics.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        metrics.addMouseListener(
            new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent event)
                {
                    explainProfile();
                }
            });

        header.add(incomplete);
        header.add(metrics);

        return header;
    }

    public void refresh()
    {
        reloadBuilds();

        BuildProfile profile = BuildProfileService.getInstance(project).getCurrent();

        BuildProfileSummary summary = profile.summarize();

        timeline.setProfile(profile, Set.copyOf(summary.criticalPath()));

        CompletenessBanner.render(incomplete, profile.getCompleteness());

        shownProfile = profile;
        shownSummary = summary;
        shownDownloads = profile.summarizeDownloads();

        reapplyView();
        showMetrics(profile, summary);
    }

    private void reapplyView()
    {
        table.setProfile(shownProfile, shownSummary, view);
        downloadTable.setSummary(shownDownloads, shownSummary.wallClockMillis());
    }

    private void showMetrics(BuildProfile profile, BuildProfileSummary summary)
    {
        metrics.clear();

        if (profile.isEmpty())
        {
            metrics.setIcon(null);
            metrics.append(
                MavenPrimeBundle.message("mavenprime.profile.empty"), SimpleTextAttributes.GRAYED_ATTRIBUTES);

            return;
        }

        metrics.setIcon(AllIcons.General.InspectionsEye);
        metrics.append(
            MavenPrimeBundle.message(
                "mavenprime.profile.wallClock", Formats.formatDuration(summary.wallClockMillis())),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

        if (!profile.hasModuleTimings())
        {
            return;
        }

        metrics.append(
            SEPARATOR + ProfileVerdict.badgeOf(summary), SimpleTextAttributes.GRAYED_ATTRIBUTES);

        if (!shownDownloads.isEmpty())
        {
            metrics.append(
                SEPARATOR
                    + MavenPrimeBundle.message(
                        "mavenprime.profile.downloading",
                        Formats.formatDuration(shownDownloads.wallClockMillis())),
                SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        metrics.append(
            SEPARATOR + MavenPrimeBundle.message("mavenprime.profile.summary.explain"), EXPLAIN);
    }

    private void explainProfile()
    {
        if (shownProfile.hasModuleTimings())
        {
            ProfileSummaryPopup.show(project, metrics, shownProfile, shownSummary);
        }
    }
}
