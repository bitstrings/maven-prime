package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.DownloadSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileFacts;
import org.bitstrings.idea.plugins.mavenprime.ui.HintLabel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

public final class ProfileSummaryPopup
{
    static final String DIMENSION_KEY = "MavenPrime.ProfileSummary";

    static final int VISIBLE_STEPS = 12;

    static final int VISIBLE_GAINS = 5;

    private static final int VERDICT_WIDTH = 420;

    private static final int HELP_GAP = 4;

    private ProfileSummaryPopup()
    {
    }

    public static void show(
        Project project, JComponent owner, BuildProfile profile, BuildProfileSummary summary)
    {
        JBPopupFactory
            .getInstance()
            .createComponentPopupBuilder(content(profile, summary), null)
            .setTitle(MavenPrimeBundle.message("mavenprime.profile.summary.title"))
            .setDimensionServiceKey(project, DIMENSION_KEY, false)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()
            .showUnderneathOf(owner);
    }

    static JComponent content(BuildProfile profile, BuildProfileSummary summary)
    {
        FormBuilder form = FormBuilder.createFormBuilder();

        form.addComponent(section("mavenprime.profile.section.build"));

        for (ProfileFacts.Fact fact : ProfileFacts.of(profile, summary))
        {
            form.addLabeledComponent(
                MavenPrimeBundle.message(fact.measure().getBundleKey()),
                explained(Formats.formatDuration(fact.millis()), fact.measure().getHelpKey()));
        }

        if (ranSeveralModules(profile))
        {
            form.addLabeledComponent(
                MavenPrimeBundle.message("mavenprime.profile.measure.parallelism"),
                explained(
                    MavenPrimeBundle.message(
                        "mavenprime.profile.parallelism",
                        Double.valueOf(summary.achievedParallelism()),
                        Double.valueOf(summary.availableParallelism())),
                    "mavenprime.profile.help.parallelism"));
            form.addLabeledComponent(
                MavenPrimeBundle.message("mavenprime.profile.measure.atOnce"),
                explained(String.valueOf(summary.laneCount()), "mavenprime.profile.help.atOnce"));
        }

        form.addComponent(verdict(summary));

        addDownloads(form, profile);

        // A single module is its own critical path and its own best saving, so both sections would
        // restate the wall clock the reader has already read twice above.
        List<ProfileFacts.Step> steps =
            (summary.criticalPath().size() < 2)
                ? List.of()
                : ProfileFacts.criticalPath(profile, summary);
        List<ProfileFacts.Gain> gains =
            ranSeveralModules(profile) ? ProfileFacts.gains(profile, summary, VISIBLE_GAINS) : List.of();

        int rowHeight = addCriticalPath(form, steps);

        addGains(form, gains);

        return scrollerOver(form.getPanel(), steps.size() + gains.size(), rowHeight);
    }

    private static int addCriticalPath(FormBuilder form, List<ProfileFacts.Step> steps)
    {
        if (steps.isEmpty())
        {
            return 0;
        }

        form.addSeparator();
        form.addComponent(section("mavenprime.profile.section.criticalPath"));

        int rowHeight = 0;

        for (ProfileFacts.Step step : steps)
        {
            JComponent timing = stepTiming(step);

            if (rowHeight == 0)
            {
                rowHeight = timing.getPreferredSize().height;
            }

            form.addLabeledComponent(stepName(step), timing);
        }

        return rowHeight;
    }

    private static boolean ranSeveralModules(BuildProfile profile)
    {
        return profile.getModules().size() > 1;
    }

    private static JComponent explained(String value, String helpKey)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(HELP_GAP), 0));

        row.setOpaque(false);
        row.add(new JBLabel(value));
        row.add(ContextHelpLabel.create(MavenPrimeBundle.message(helpKey)));

        return row;
    }

    private static void addDownloads(FormBuilder form, BuildProfile profile)
    {
        DownloadSummary downloads = profile.summarizeDownloads();

        if (downloads.isEmpty())
        {
            return;
        }

        form.addSeparator();
        form.addComponent(section("mavenprime.profile.section.downloads"));
        form.addLabeledComponent(
            MavenPrimeBundle.message("mavenprime.profile.measure.downloading"),
            explained(
                Formats.formatDuration(downloads.wallClockMillis()),
                "mavenprime.profile.help.downloading"));
        form.addLabeledComponent(
            MavenPrimeBundle.message("mavenprime.profile.measure.transferred"),
            explained(
                MavenPrimeBundle.message(
                    "mavenprime.profile.transferred",
                    Formats.formatFileSize(downloads.bytes()),
                    Integer.valueOf(downloads.count())),
                "mavenprime.profile.help.transferred"));
    }

    private static void addGains(FormBuilder form, List<ProfileFacts.Gain> gains)
    {
        if (gains.isEmpty())
        {
            return;
        }

        form.addSeparator();
        form.addComponent(section("mavenprime.profile.section.gains"));

        for (ProfileFacts.Gain gain : gains)
        {
            form.addLabeledComponent(gainName(gain), gainTiming(gain));
        }
    }

    private static JComponent gainName(ProfileFacts.Gain gain)
    {
        JBLabel name =
            new JBLabel(shortNameOf(gain.module()), AllIcons.Actions.Rerun, SwingConstants.LEADING);

        name.setToolTipText(gain.module());

        return name;
    }

    private static JComponent gainTiming(ProfileFacts.Gain gain)
    {
        return new JBLabel(
            MavenPrimeBundle.message(
                "mavenprime.profile.stepTiming",
                Formats.formatDuration(gain.millis()),
                Double.valueOf(gain.sharePercent())));
    }

    private static JComponent scrollerOver(JComponent form, int stepCount, int rowHeight)
    {
        JBScrollPane scroller = new JBScrollPane(form);

        if (stepCount > VISIBLE_STEPS)
        {
            Dimension content = form.getPreferredSize();

            scroller.setPreferredSize(
                new Dimension(content.width, content.height - ((stepCount - VISIBLE_STEPS) * rowHeight)));
        }

        return scroller;
    }

    private static JComponent section(String bundleKey)
    {
        JBLabel title = new JBLabel(MavenPrimeBundle.message(bundleKey));

        title.setFont(UIUtil.getFont(UIUtil.FontSize.SMALL, title.getFont()));
        title.setForeground(UIUtil.getContextHelpForeground());

        return title;
    }

    private static JComponent stepName(ProfileFacts.Step step)
    {
        JBLabel name =
            new JBLabel(shortNameOf(step.module()), AllIcons.Actions.Lightning, SwingConstants.LEADING);

        name.setToolTipText(step.module());

        return name;
    }

    private static JComponent stepTiming(ProfileFacts.Step step)
    {
        return new JBLabel(
            MavenPrimeBundle.message(
                "mavenprime.profile.stepTiming",
                Formats.formatDuration(step.durationMillis()),
                Double.valueOf(step.sharePercent())));
    }

    private static String shortNameOf(String module)
    {
        int lastSeparator = module.lastIndexOf(':');

        return (lastSeparator < 0) ? module : module.substring(lastSeparator + 1);
    }

    private static JComponent verdict(BuildProfileSummary summary)
    {
        JBLabel sentence =
            new JBLabel(HintLabel.bounded(JBUI.scale(VERDICT_WIDTH), ProfileVerdict.sentenceOf(summary)));

        sentence.setIcon(iconOf(summary));

        return sentence;
    }

    private static Icon iconOf(BuildProfileSummary summary)
    {
        return switch (summary.concurrency())
        {
            case SEQUENTIAL -> AllIcons.General.BalloonInformation;
            case DEPENDENCY_BOUND -> AllIcons.General.ShowWarning;
            case THREAD_STARVED -> AllIcons.General.BalloonWarning;
        };
    }
}
