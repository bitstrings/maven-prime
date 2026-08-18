package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.goals.GoalActions;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public final class MavenPrimeToolWindowFactory
    implements ToolWindowFactory, DumbAware
{

    @Override
    public void init(ToolWindow toolWindow)
    {
        Project project = toolWindow.getProject();

        MavenProjectsManager
            .getInstance(project)
            .addManagerListener(new AvailabilityListener(toolWindow), project);

        GoalActions.getInstance().sync();

        updateAvailability(toolWindow);
    }

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow)
    {
        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(project);

        Content content = ContentFactory.getInstance().createContent(panel, null, false);

        content.setDisposer(panel);

        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(Project project)
    {
        return MavenProjects.isMavenized(project);
    }

    private static void updateAvailability(ToolWindow toolWindow)
    {
        Project project = toolWindow.getProject();

        ApplicationManager
            .getApplication()
            .invokeLater(
                () -> toolWindow.setAvailable(MavenProjects.isMavenized(project)), project.getDisposed());
    }

    private static final class AvailabilityListener
        implements MavenProjectsManager.Listener
    {
        private final ToolWindow toolWindow;

        AvailabilityListener(ToolWindow toolWindow)
        {
            this.toolWindow = toolWindow;
        }

        @Override
        public void activated()
        {
            updateAvailability(toolWindow);
        }

        @Override
        public void projectImportCompleted()
        {
            updateAvailability(toolWindow);
        }
    }
}
