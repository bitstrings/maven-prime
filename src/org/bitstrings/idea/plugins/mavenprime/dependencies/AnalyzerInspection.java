package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;

public final class AnalyzerInspection
{
    public static final Topic<AnalysisListener> TOPIC =
        Topic.create("Maven Prime dependency analysis", AnalysisListener.class);

    private AnalyzerInspection()
    {
    }

    public static boolean isAvailable(MavenProject module)
    {
        return (module != null) && (module.getFile() != null);
    }

    public static void analyze(Project project, MavenProject module, ArtifactCoordinates coordinates)
    {
        if (!isAvailable(module))
        {
            return;
        }

        VirtualFile pom = module.getFile();

        FileEditorManager manager = FileEditorManager.getInstance(project);

        manager.openFile(pom, true);
        manager.setSelectedEditor(pom, DependencyAnalyzerEditorProvider.EDITOR_TYPE_ID);

        UiTopics.publish(project, TOPIC, listener -> listener.analysisRequested(pom, coordinates));
    }

    public interface AnalysisListener
    {
        void analysisRequested(VirtualFile pom, ArtifactCoordinates coordinates);
    }
}
