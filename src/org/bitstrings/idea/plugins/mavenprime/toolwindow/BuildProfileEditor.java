package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import javax.swing.JComponent;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.ui.PanelEditorFile;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;

@Service(Service.Level.PROJECT)
public final class BuildProfileEditor
{
    private final Project project;

    private final PanelEditorFile file;

    public BuildProfileEditor(Project project)
    {
        this.project = project;
        this.file =
            new PanelEditorFile(
                MavenPrimeBundle.message("mavenprime.profile.editorTab"),
                AllIcons.Actions.ProfileCPU,
                this::createPanel);
    }

    public static BuildProfileEditor getInstance(Project project)
    {
        return project.getService(BuildProfileEditor.class);
    }

    public void open()
    {
        FileEditorManager.getInstance(project).openFile(file, true, true);
    }

    PanelEditorFile getFile()
    {
        return file;
    }

    private JComponent createPanel(Disposable editor)
    {
        BuildProfilePanel panel = new BuildProfilePanel(project);

        Disposer.register(editor, panel);

        return panel;
    }
}
