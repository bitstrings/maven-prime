package org.bitstrings.idea.plugins.mavenprime;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public final class MavenPrimeNotifications
{
    public static final String GROUP_ID = "MavenPrime";

    private MavenPrimeNotifications()
    {
    }

    public static void error(Project project, String content)
    {
        notify(project, content, NotificationType.ERROR);
    }

    public static void warning(Project project, String content)
    {
        notify(project, content, NotificationType.WARNING);
    }

    public static void info(Project project, String content)
    {
        notify(project, content, NotificationType.INFORMATION);
    }

    private static void notify(Project project, String content, NotificationType type)
    {
        NotificationGroupManager
            .getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(MavenPrimeBundle.message("mavenprime.name"), content, type)
            .notify(project);
    }
}
