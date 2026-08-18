package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.Dimension;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileFacts;
import org.bitstrings.idea.plugins.mavenprime.ui.HintLabel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

public final class ProfileSummaryPopup
{
    static final String DIMENSION_KEY = "MavenPrime.ProfileSummary";

    static final int VISIBLE_STEPS = 12;

    private static final int VERDICT_WIDTH = 420;

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

        for (ProfileFacts.Fact fact : ProfileFacts.of(summary))
        {
            form.addLabeledComponent(
                MavenPrimeBundle.message(fact.measure().getBundleKey()),
                new JBLabel(Formats.formatDuration(fact.millis())));
        }

        form.addLabeledComponent(
            MavenPrimeBundle.message("mavenprime.profile.measure.parallelism"),
            new JBLabel(
                MavenPrimeBundle.message(
                    "mavenprime.profile.parallelism",
                    Double.valueOf(summary.achievedParallelism()),
                    Double.valueOf(summary.availableParallelism()))));
        form.addLabeledComponent(
            MavenPrimeBundle.message("mavenprime.profile.measure.atOnce"),
            new JBLabel(String.valueOf(summary.laneCount())));

        form.addComponent(verdict(summary));

        if (summary.criticalPath().isEmpty())
        {
            return new JBScrollPane(form.getPanel());
        }

        form.addSeparator();
        form.addComponent(section("mavenprime.profile.section.criticalPath"));

        List<ProfileFacts.Step> steps = ProfileFacts.criticalPath(profile, summary);

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

        return scrollerOver(form.getPanel(), steps.size(), rowHeight);
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
