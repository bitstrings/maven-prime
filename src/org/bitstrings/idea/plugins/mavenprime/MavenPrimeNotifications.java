package org.bitstrings.idea.plugins.mavenprime;

import java.util.List;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;

public final class MavenPrimeNotifications
{
    public static final String GROUP_ID = "MavenPrime";

    private MavenPrimeNotifications()
    {
    }

    public static Notification error(Project project, String content, AnAction... actions)
    {
        return notify(project, content, NotificationType.ERROR, actions);
    }

    public static Notification warning(Project project, String content, AnAction... actions)
    {
        return notify(project, content, NotificationType.WARNING, actions);
    }

    public static Notification info(Project project, String content, AnAction... actions)
    {
        return notify(project, content, NotificationType.INFORMATION, actions);
    }

    private static Notification notify(
        Project project, String content, NotificationType type, AnAction... actions)
    {
        Notification notification =
            NotificationGroupManager
                .getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(MavenPrimeBundle.message("mavenprime.name"), content, type)
                .addActions(List.of(actions));

        notification.notify(project);

        return notification;
    }
}
