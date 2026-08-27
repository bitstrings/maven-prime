package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;

public class BuildProfileNotifierPlatformTest
    extends BasePlatformTestCase
{
    private static final long HALF_MINUTE = 30_000L;

    private static final long MINUTE = 60_000L;

    private final List<Notification> raised = new ArrayList<>();

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        getProject()
            .getMessageBus()
            .connect(getTestRootDisposable())
            .subscribe(
                Notifications.TOPIC,
                new Notifications()
                {
                    @Override
                    public void notify(Notification notification)
                    {
                        raised.add(notification);
                    }
                });
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            MavenPrimeSettings.getInstance(getProject()).profileNotifications = false;
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testBuildFinished_theSettingLeftOffAndAStarvedBuild_saysNothing()
    {
        starvedBuild();

        BuildProfileNotifier.getInstance(getProject()).buildFinished();

        assertEquals(
            "a notification nobody asked for is the reason the setting exists", List.of(), ours());
    }

    public void testBuildFinished_theSettingOnAndAStarvedBuild_raisesOneNotification()
    {
        MavenPrimeSettings.getInstance(getProject()).profileNotifications = true;

        starvedBuild();

        BuildProfileNotifier.getInstance(getProject()).buildFinished();

        assertEquals(1, ours().size());
    }

    public void testBuildFinished_theSettingOnAndAStarvedBuild_offersTheProfilerAsTheWayIn()
    {
        MavenPrimeSettings.getInstance(getProject()).profileNotifications = true;

        starvedBuild();

        BuildProfileNotifier.getInstance(getProject()).buildFinished();

        assertEquals(
            "a verdict with nowhere to act on it leaves the reader hunting through the menus",
            List.of(MavenPrimeBundle.message("mavenprime.profile.notification.open")),
            ours().get(0).getActions().stream().map(AnAction::getTemplateText).toList());
    }

    public void testBuildFinished_aStarvedBuildWhoseEventStreamWasTruncated_saysNothing()
    {
        MavenPrimeSettings.getInstance(getProject()).profileNotifications = true;

        starvedBuild().getCompleteness().recordTruncated();

        BuildProfileNotifier.getInstance(getProject()).buildFinished();

        assertEquals(
            "the timings the verdict rests on are the ones that went missing, and a balloon carries "
                + "no banner to say so",
            List.of(),
            ours());
    }

    public void testBuildFinished_aStarvedBuildWhoseReactorStoppedOnAFailedModule_saysNothing()
    {
        MavenPrimeSettings.getInstance(getProject()).profileNotifications = true;

        starvedBuild().accept(new ProfileEvent.ModuleFailed("g:c"));

        BuildProfileNotifier.getInstance(getProject()).buildFinished();

        assertEquals(
            "a reactor that stopped early measured part of a build, and advising -T from the part "
                + "that ran claims to have watched the whole thing",
            List.of(),
            ours());
    }

    public void testBuildFinished_aSecondBuildWorthReporting_expiresTheVerdictItSupersedes()
    {
        MavenPrimeSettings.getInstance(getProject()).profileNotifications = true;

        starvedBuild();

        BuildProfileNotifier.getInstance(getProject()).buildFinished();

        Notification first = ours().get(0);

        starvedBuild();

        BuildProfileNotifier.getInstance(getProject()).buildFinished();

        assertTrue(
            "the log would otherwise stack one verdict per build, all but the last describing a run "
                + "the reader has moved past",
            first.isExpired());
    }

    public void testBuildFinished_theSettingOnAndABuildAlreadyAtItsFloor_saysNothing()
    {
        MavenPrimeSettings.getInstance(getProject()).profileNotifications = true;

        BuildProfile profile = BuildProfileService.getInstance(getProject()).startBuild(getName(), "");

        profile.accept(new ModuleTiming("g:a", 0L, MINUTE));
        profile.accept(new ModuleTiming("g:b", MINUTE, MINUTE));
        profile.accept(new ModuleTiming("g:c", 0L, HALF_MINUTE));
        profile.accept(new ProfileEvent.ReactorEdge("g:b", "g:a"));

        BuildProfileNotifier.getInstance(getProject()).buildFinished();

        assertEquals(
            "nothing was lost to scheduling and no thread count beats the chain, so there is no "
                + "action to offer",
            List.of(),
            ours());
    }

    public void testBuildFinished_aBuildThatFinished_reachesTheNotifierThroughTheTopic()
    {
        MavenPrimeSettings.getInstance(getProject()).profileNotifications = true;

        starvedBuild();

        BuildProfileService.getInstance(getProject()).buildFinished();

        UIUtil.dispatchAllInvocationEvents();

        assertEquals(
            "the notifier is subscribed at startup and nowhere else, so a build that ends without "
                + "reaching it leaves the setting doing nothing",
            1,
            ours().size());
    }

    private BuildProfile starvedBuild()
    {
        BuildProfile profile = BuildProfileService.getInstance(getProject()).startBuild(getName(), "");

        profile.accept(new ModuleTiming("g:a", 0L, HALF_MINUTE));
        profile.accept(new ModuleTiming("g:b", 0L, HALF_MINUTE));
        profile.accept(new ModuleTiming("g:c", HALF_MINUTE, HALF_MINUTE));
        profile.accept(new ModuleTiming("g:d", HALF_MINUTE, HALF_MINUTE));

        return profile;
    }

    private List<Notification> ours()
    {
        return raised
            .stream()
            .filter(notification -> MavenPrimeNotifications.GROUP_ID.equals(notification.getGroupId()))
            .toList();
    }
}
