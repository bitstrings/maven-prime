package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public final class DependencyAnalyzerEditorProvider
    implements FileEditorProvider, DumbAware
{
    static final String EDITOR_TYPE_ID = "MavenPrimeDependencyAnalyzer";

    @Override
    public boolean accept(Project project, VirtualFile file)
    {
        MavenProject mavenProject = MavenProjects.byPomFile(project, file);

        return (mavenProject != null) && file.equals(mavenProject.getFile());
    }

    @Override
    public FileEditor createEditor(Project project, VirtualFile file)
    {
        return new DependencyAnalyzerEditor(project, file);
    }

    @Override
    public String getEditorTypeId()
    {
        return EDITOR_TYPE_ID;
    }

    @Override
    public FileEditorPolicy getPolicy()
    {
        return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }
}
