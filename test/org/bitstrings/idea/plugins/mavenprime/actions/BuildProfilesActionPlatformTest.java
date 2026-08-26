package org.bitstrings.idea.plugins.mavenprime.actions;

import java.awt.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileListPanel;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BuildProfilesActionPlatformTest
    extends BasePlatformTestCase
{
    private static final String PROFILE = "release";

    private static final String STALE = "docs";

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            BuildContext.getInstance(getProject()).clearOverrides();
        }
        finally
        {
            super.tearDown();
        }
    }

    private static final Dimension REMEMBERED = new Dimension(300, 400);

    public void testFitted_aListThatFitsTheRememberedSize_leavesItExactlyAsItWas()
    {
        assertEquals(
            "a size the user dragged to is theirs, and content that fits has no claim on it",
            REMEMBERED,
            BuildProfilesAction.fitted(REMEMBERED, new Dimension(280, 380), new Dimension(200, 100)));
    }

    public void testFitted_aNameWiderThanTheRememberedSize_growsByExactlyTheShortfall()
    {
        assertEquals(
            new Dimension(REMEMBERED.width + 40, REMEMBERED.height),
            BuildProfilesAction.fitted(REMEMBERED, new Dimension(280, 380), new Dimension(320, 100)));
    }

    public void testFitted_moreRowsThanTheRememberedSizeShows_growsTallerByTheShortfall()
    {
        assertEquals(
            new Dimension(REMEMBERED.width, REMEMBERED.height + 25),
            BuildProfilesAction.fitted(REMEMBERED, new Dimension(280, 380), new Dimension(200, 405)));
    }

    public void testFitted_aShorterListThanBefore_neverShrinksBelowWhatWasRemembered()
    {
        assertEquals(
            "shrinking to the content throws away the size the user chose",
            REMEMBERED,
            BuildProfilesAction.fitted(REMEMBERED, new Dimension(280, 380), new Dimension(10, 10)));
    }

    public void testUpdate_aProjectMavenNeverTookOver_disablesTheActionAsWellAsHidingIt()
    {
        BuildContext.getInstance(getProject()).setProfile(PROFILE, Boolean.TRUE);

        BuildProfilesAction action = new BuildProfilesAction();

        AnActionEvent event =
            TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject()));

        action.update(event);

        assertFalse(
            "a hidden action still fires from its keymap shortcut, so leaving it enabled opens the "
                + "profiles popup in a project Maven does not manage",
            event.getPresentation().isEnabled());
    }

    public void testReload_theProfilesTheContextCarries_replaceWhateverTheListWasShowing()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(STALE), Map.of(STALE, Boolean.TRUE));

        BuildContext.getInstance(getProject()).setProfile(PROFILE, Boolean.FALSE);

        BuildProfilesAction.reload(getProject(), panel);

        assertEquals(
            "a popup that keeps the snapshot it opened with hides profiles that the profile the user "
                + "just set has activated or deactivated",
            Map.of(PROFILE, Boolean.FALSE),
            panel.getSelectedProfiles());
    }

    public void testSubscribeWhileVisible_aPopupThatNeverReachedTheScreen_disposesIt()
    {
        List<JBPopup> disposed = new ArrayList<>();

        ProfileListPanel profiles = new ProfileListPanel();

        JBPopup popup = unshownPopupOver(profiles);

        Disposer.register(popup, () -> disposed.add(popup));

        BuildProfilesAction.subscribeWhileVisible(getProject(), popup, profiles);

        assertEquals(
            "a popup that never showed is never disposed by the platform either, so it keeps its panel "
                + "and the project alive for the rest of the session",
            List.of(popup),
            disposed);
    }

    public void testSubscribeWhileVisible_aPopupThatNeverReachedTheScreen_wiresNoContextListener()
    {
        ProfileListPanel profiles = new ProfileListPanel();

        profiles.setProfiles(List.of(PROFILE), Map.of(PROFILE, Boolean.TRUE));

        BuildProfilesAction.subscribeWhileVisible(getProject(), unshownPopupOver(profiles), profiles);

        getProject().getMessageBus().syncPublisher(BuildContext.TOPIC).buildContextChanged();

        assertEquals(
            "a listener outliving the popup it was meant for reloads a panel nobody is looking at",
            Map.of(PROFILE, Boolean.TRUE),
            profiles.getSelectedProfiles());
    }

    private static JBPopup unshownPopupOver(ProfileListPanel profiles)
    {
        return JBPopupFactory
            .getInstance()
            .createComponentPopupBuilder(profiles, profiles.getPreferredFocusedComponent())
            .createPopup();
    }
}
