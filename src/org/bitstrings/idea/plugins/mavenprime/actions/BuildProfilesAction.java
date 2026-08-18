package org.bitstrings.idea.plugins.mavenprime.actions;

import java.awt.Component;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileListPanel;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;

public final class BuildProfilesAction
    extends AnAction
    implements DumbAware
{
    private static final String DIMENSION_KEY = "MavenPrime.BuildProfiles";

    @Override
    public void update(AnActionEvent event)
    {
        Project project = event.getProject();

        boolean mavenized = (project != null) && MavenProjects.isMavenized(project);

        event.getPresentation().setVisible(mavenized);
        event.getPresentation().setEnabled(mavenized && hasProfiles(project));
    }

    static boolean hasProfiles(Project project)
    {
        BuildContext context = BuildContext.getInstance(project);

        return !context.getAvailableProfiles().isEmpty() || !context.getProfiles().isEmpty();
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        Project project = event.getProject();

        if (project == null)
        {
            return;
        }

        ProfileListPanel profiles = panelFor(project);

        JBPopup popup = popupFor(project, profiles);

        showFor(popup, event);

        subscribeWhileVisible(project, popup, profiles);
    }

    static void showFor(JBPopup popup, AnActionEvent event)
    {
        Component owner = (event.getInputEvent() == null) ? null : event.getInputEvent().getComponent();

        if ((owner != null) && owner.isShowing())
        {
            popup.showUnderneathOf(owner);
        }
        else
        {
            popup.showInBestPositionFor(event.getDataContext());
        }
    }

    static boolean subscribeWhileVisible(Project project, JBPopup popup, ProfileListPanel profiles)
    {
        if (!popup.isVisible())
        {
            Disposer.dispose(popup);

            return false;
        }

        project
            .getMessageBus()
            .connect(popup)
            .subscribe(
                BuildContext.TOPIC,
                (BuildContext.BuildContextListener) () -> reload(project, profiles));

        return true;
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }

    private static ProfileListPanel panelFor(Project project)
    {
        BuildContext context = BuildContext.getInstance(project);

        ProfileListPanel profiles =
            new ProfileListPanel(
                entry -> context.setProfile(entry.getName(), entry.getState().toEnabled()));

        reload(project, profiles);

        return profiles;
    }

    static void reload(Project project, ProfileListPanel profiles)
    {
        BuildContext context = BuildContext.getInstance(project);

        profiles.setProfiles(context.getAvailableProfiles(), context.getProfiles());
    }

    private static JBPopup popupFor(Project project, ProfileListPanel profiles)
    {
        return JBPopupFactory
            .getInstance()
            .createComponentPopupBuilder(profiles, profiles.getPreferredFocusedComponent())
            .setTitle(MavenPrimeBundle.message("mavenprime.profiles.popup.title"))
            .setDimensionServiceKey(project, DIMENSION_KEY, false)
            .setRequestFocus(true)
            .setMovable(true)
            .setResizable(true)
            .createPopup();
    }
}
