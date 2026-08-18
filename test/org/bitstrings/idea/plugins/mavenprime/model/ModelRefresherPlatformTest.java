package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.ArrayList;
import java.util.List;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ModelRefresherPlatformTest
    extends BasePlatformTestCase
{
    private final List<Notification> reported = new ArrayList<>();

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
                        reported.add(notification);
                    }
                });
    }

    public void testRefresh_aProjectWithoutMavenRoots_tellsTheUserWhyNothingHappened()
    {
        ModelRefresher.getInstance(getProject()).refresh();

        assertFalse("an explicit refresh must explain itself", reported.isEmpty());
    }

    public void testRefreshUnread_aProjectWithoutMavenRoots_staysSilent()
    {
        ModelRefresher.getInstance(getProject()).refreshUnread();

        assertEquals("opening the Model tab must never raise an error balloon", List.of(), reported);
    }
}
