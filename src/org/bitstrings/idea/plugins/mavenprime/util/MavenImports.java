package org.bitstrings.idea.plugins.mavenprime.util;

import java.util.Collection;
import java.util.List;

import org.jetbrains.idea.maven.project.MavenImportListener;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

public final class MavenImports
{
    private MavenImports()
    {
    }

    public static void onImported(Project project, Disposable parent, Runnable onImported)
    {
        project
            .getMessageBus()
            .connect(parent)
            .subscribe(
                MavenImportListener.TOPIC,
                new MavenImportListener()
                {
                    @Override
                    public void importFinished(
                        Collection<MavenProject> importedProjects, List<Module> newModules)
                    {
                        ApplicationManager
                            .getApplication()
                            .invokeLater(onImported, project.getDisposed());
                    }
                });
    }

    public static void onModelAvailable(Project project, Disposable parent, Runnable onAvailable)
    {
        onModelAvailable(project, parent, onAvailable, onAvailable);
    }

    // Writing to the Maven model from onImported schedules another import, so the two are separable.
    public static void onModelAvailable(
        Project project, Disposable parent, Runnable onActivated, Runnable onImported)
    {
        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);

        manager.addManagerListener(
            new MavenProjectsManager.Listener()
            {
                @Override
                public void activated()
                {
                    schedule(project, onActivated);
                }

                @Override
                public void projectImportCompleted()
                {
                    schedule(project, onImported);
                }
            },
            parent);
    }

    private static void schedule(Project project, Runnable action)
    {
        ApplicationManager.getApplication().invokeLater(action, project.getDisposed());
    }
}
