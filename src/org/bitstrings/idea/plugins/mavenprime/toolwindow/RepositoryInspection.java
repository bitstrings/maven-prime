package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.Topic;

public final class RepositoryInspection
{
    public static final Topic<InspectionListener> TOPIC =
        Topic.create("Maven Prime repository inspection", InspectionListener.class);

    public static final String TOOL_WINDOW_ID = "Maven Prime";

    private RepositoryInspection()
    {
    }

    public static void inspect(Project project, ArtifactCoordinates coordinates)
    {
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);

        if (window == null)
        {
            publish(project, coordinates);

            return;
        }

        window.activate(() -> publish(project, coordinates));
    }

    private static void publish(Project project, ArtifactCoordinates coordinates)
    {
        UiTopics.publish(project, TOPIC, listener -> listener.inspectRequested(coordinates));
    }

    public interface InspectionListener
    {
        void inspectRequested(ArtifactCoordinates coordinates);
    }
}
