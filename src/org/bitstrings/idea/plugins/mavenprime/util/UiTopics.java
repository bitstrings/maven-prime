package org.bitstrings.idea.plugins.mavenprime.util;

import java.util.function.Consumer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

public final class UiTopics
{
    private UiTopics()
    {
    }

    public static <L> void publish(Project project, Topic<L> topic, Consumer<L> notification)
    {
        if (project.isDisposed())
        {
            return;
        }

        ApplicationManager
            .getApplication()
            .invokeLater(
                () -> notification.accept(project.getMessageBus().syncPublisher(topic)),
                project.getDisposed());
    }
}
