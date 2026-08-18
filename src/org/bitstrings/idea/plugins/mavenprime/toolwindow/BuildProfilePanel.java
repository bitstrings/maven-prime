package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.ui.BuildSelector;
import org.bitstrings.idea.plugins.mavenprime.ui.CommandAction;
import org.bitstrings.idea.plugins.mavenprime.ui.CompletenessBanner;
import org.bitstrings.idea.plugins.mavenprime.ui.ViewToggleAction;
import org.bitstrings.idea.plugins.mavenprime.util.UiState;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.OnePixelSplitter;
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

    private static final float DEFAULT_SPLIT = 0.5f;

    private static final String STATE_PREFIX = "mavenPrime.profiler.";

    static final String SUMMARY_KEY = "showSummary";

    static final String TIMELINE_KEY = "showTimeline";

    static final String DETAILS_KEY = "showDetails";

    private final transient Project project;

    private final SimpleColoredComponent incomplete = new SimpleColoredComponent();

    final SimpleColoredComponent metrics = new SimpleColoredComponent();

    private transient BuildProfile shownProfile = new BuildProfile();

    private transient BuildProfileSummary shownSummary = BuildProfileSummary.empty();

    private final BuildTimeline timeline = new BuildTimeline();

    private final ProfileTable table = new ProfileTable();

    private final transient UiState state;

    private final OnePixelSplitter splitter = new OnePixelSplitter(true, SPLITTER_KEY, DEFAULT_SPLIT);

    final JPanel chart = new JPanel(new BorderLayout());

    final JBScrollPane details = new JBScrollPane(table);

    final JPanel header = buildHeader();

    final JComponent toolbar = buildToolbar();

    private final transient BuildSelector<BuildProfile> buildSelector;

    public BuildProfilePanel(Project project)
    {
        super(new BorderLayout());

        this.project = project;
        this.state = new UiState(project, STATE_PREFIX);
        this.buildSelector =
            new BuildSelector<>(entry -> BuildProfileService.getInstance(project).select(entry));

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

    // The toolbar owns the toggles, so it cannot live inside a part those toggles hide.
    private JComponent buildNorth()
    {
        JPanel north = new JPanel(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());

        top.add(toolbar, BorderLayout.WEST);
        top.add(buildSelector.getComponent(), BorderLayout.EAST);

        north.add(top, BorderLayout.NORTH);
        north.add(header, BorderLayout.CENTER);

        return north;
    }

    private void reloadBuilds()
    {
        BuildProfileService service = BuildProfileService.getInstance(project);

        buildSelector.reload(service.getBuilds(), service.getSelectedBuild());
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

        splitter.setFirstComponent(isShown(TIMELINE_KEY) ? chart : null);
        splitter.setSecondComponent(isShown(DETAILS_KEY) ? details : null);

        revalidate();
        repaint();
    }

    boolean isShown(String key)
    {
        return state.isEnabled(key, true);
    }

    void show(String key, boolean shown)
    {
        state.setEnabled(key, shown, true);

        applyVisibility();
    }

    private JComponent buildToolbar()
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
        table.setProfile(profile, summary);

        CompletenessBanner.render(incomplete, profile.getCompleteness());

        shownProfile = profile;
        shownSummary = summary;

        showMetrics(profile, summary);
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
