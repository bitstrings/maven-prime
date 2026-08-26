package org.bitstrings.idea.plugins.mavenprime.settings;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.config.MavenPrimeConfigService;
import org.bitstrings.idea.plugins.mavenprime.config.SharedConfigTemplate;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalScope;
import org.bitstrings.idea.plugins.mavenprime.ui.HintLabel;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.FormBuilder;

public final class MavenPrimeConfigurable
    implements Configurable
{
    private final Project project;

    private GoalTablePanel globalPanel;

    private GoalTablePanel projectPanel;

    private JBCheckBox autoRefreshBox;

    private JBCheckBox colorConsoleBox;

    private JBCheckBox sharedContextBox;

    private JBCheckBox profileNotificationsBox;

    public MavenPrimeConfigurable(Project project)
    {
        this.project = project;
    }

    private void relocate(GoalScope target, GoalDefinition definition)
    {
        ((target == GoalScope.GLOBAL) ? globalPanel : projectPanel).append(definition);
    }

    @Override
    public String getDisplayName()
    {
        return MavenPrimeBundle.message("mavenprime.name");
    }

    @Override
    public JComponent createComponent()
    {
        globalPanel = new GoalTablePanel(project, GoalScope.GLOBAL, this::relocate);
        projectPanel = new GoalTablePanel(project, GoalScope.PROJECT, this::relocate);

        globalPanel.setBorder(
            IdeBorderFactory.createTitledBorder(MavenPrimeBundle.message("mavenprime.settings.global")));
        projectPanel.setBorder(
            IdeBorderFactory.createTitledBorder(MavenPrimeBundle.message("mavenprime.settings.project")));

        JBTabbedPane tabs = new JBTabbedPane();

        tabs.addTab(MavenPrimeBundle.message("mavenprime.settings.global"), globalPanel);
        tabs.addTab(MavenPrimeBundle.message("mavenprime.settings.project"), projectPanel);

        autoRefreshBox = new JBCheckBox(MavenPrimeBundle.message("action.MavenPrime.AutoRefreshModel.text"));
        colorConsoleBox = new JBCheckBox(MavenPrimeBundle.message("action.MavenPrime.ColorConsole.text"));
        sharedContextBox =
            new JBCheckBox(
                MavenPrimeBundle.message("mavenprime.sharedContext", MavenPrimeConfigService.FILE_NAME));
        profileNotificationsBox =
            new JBCheckBox(MavenPrimeBundle.message("mavenprime.profileNotifications"));

        JPanel header =
            FormBuilder
                .createFormBuilder()
                .setVerticalGap(0)
                .addComponent(sharedContextBox)
                .addComponent(
                    HintLabel.under(
                        sharedContextBox, "mavenprime.sharedContext.hint", MavenPrimeConfigService.FILE_NAME))
                .addComponent(createSharedConfigLink())
                .addComponent(autoRefreshBox)
                .addComponent(HintLabel.under(autoRefreshBox, "mavenprime.autoRefresh.hint"))
                .addComponent(colorConsoleBox)
                .addComponent(HintLabel.under(colorConsoleBox, "mavenprime.colorConsole.hint"))
                .addComponent(profileNotificationsBox)
                .addComponent(
                    HintLabel.under(profileNotificationsBox, "mavenprime.profileNotifications.hint"))
                .getPanel();

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(header, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);

        reset();

        return panel;
    }

    private JComponent createSharedConfigLink()
    {
        ActionLink link = new ActionLink();

        link.setText(MavenPrimeBundle.message("action.MavenPrime.CreateSharedConfig.text"));
        link.setHorizontalAlignment(SwingConstants.LEFT);
        link.setEnabled(MavenPrimeConfigService.getInstance(project).findConfigFile() == null);
        link.addActionListener(
            event ->
            {
                apply();

                link.setEnabled(
                    MavenPrimeConfigService
                        .getInstance(project)
                        .createSharedFile(SharedConfigTemplate.of(project)) == null);
            });

        return HintLabel.alignUnder(sharedContextBox, link);
    }

    @Override
    public boolean isModified()
    {
        if (!isShowing())
        {
            return false;
        }

        GoalRegistry registry = GoalRegistry.getInstance(project);

        return !registry.global().equals(globalPanel.getGoals())
            || !registry.projectScoped().equals(projectPanel.getGoals())
            || (autoRefreshBox.isSelected() != settings().autoRefresh)
            || (colorConsoleBox.isSelected() != settings().colorConsole)
            || (sharedContextBox.isSelected() != settings().sharedContext)
            || (profileNotificationsBox.isSelected() != settings().profileNotifications);
    }

    @Override
    public void apply()
    {
        if (!isShowing())
        {
            return;
        }

        GoalRegistry registry = GoalRegistry.getInstance(project);

        if (!registry.global().equals(globalPanel.getGoals()))
        {
            registry.updateGlobal(globalPanel.getGoals());
        }

        if (!registry.projectScoped().equals(projectPanel.getGoals()))
        {
            registry.updateProject(projectPanel.getGoals());
        }

        boolean sharedContextChanged = sharedContextBox.isSelected() != settings().sharedContext;

        settings().autoRefresh = autoRefreshBox.isSelected();
        settings().colorConsole = colorConsoleBox.isSelected();
        settings().sharedContext = sharedContextBox.isSelected();
        settings().profileNotifications = profileNotificationsBox.isSelected();

        if (sharedContextChanged)
        {
            BuildContext.getInstance(project).sharedContextChanged();
        }

        UiTopics.publish(
            project, MavenPrimeSettings.TOPIC, MavenPrimeSettings.SettingsListener::settingsChanged);
    }

    @Override
    public void reset()
    {
        if (!isShowing())
        {
            return;
        }

        GoalRegistry registry = GoalRegistry.getInstance(project);

        globalPanel.setGoals(registry.global());
        projectPanel.setGoals(registry.projectScoped());

        autoRefreshBox.setSelected(settings().autoRefresh);
        colorConsoleBox.setSelected(settings().colorConsole);
        sharedContextBox.setSelected(settings().sharedContext);
        profileNotificationsBox.setSelected(settings().profileNotifications);
    }

    private boolean isShowing()
    {
        return (globalPanel != null)
            && (projectPanel != null)
            && (autoRefreshBox != null)
            && (colorConsoleBox != null)
            && (sharedContextBox != null)
            && (profileNotificationsBox != null);
    }

    @Override
    public void disposeUIResources()
    {
        globalPanel = null;
        projectPanel = null;
        autoRefreshBox = null;
        colorConsoleBox = null;
        sharedContextBox = null;
        profileNotificationsBox = null;
    }

    private MavenPrimeSettings settings()
    {
        return MavenPrimeSettings.getInstance(project);
    }
}
