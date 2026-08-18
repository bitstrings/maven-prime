package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JComponent;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;

public final class DependencyAnalyzerEditor
    extends UserDataHolderBase
    implements FileEditor
{
    private static final long serialVersionUID = 1L;

    private final transient VirtualFile file;

    private final DependencyAnalyzerPanel panel;

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public DependencyAnalyzerEditor(Project project, VirtualFile file)
    {
        this.file = file;
        this.panel = new DependencyAnalyzerPanel(project, file);
    }

    @Override
    public JComponent getComponent()
    {
        return panel;
    }

    @Override
    public JComponent getPreferredFocusedComponent()
    {
        return panel.getPreferredFocusedComponent();
    }

    @Override
    public String getName()
    {
        return MavenPrimeBundle.message("mavenprime.dependencies.editorTab");
    }

    @Override
    public void setState(FileEditorState state)
    {
    }

    @Override
    public boolean isModified()
    {
        return false;
    }

    @Override
    public boolean isValid()
    {
        return file.isValid();
    }

    @Override
    public void selectNotify()
    {
        panel.refresh();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public VirtualFile getFile()
    {
        return file;
    }

    @Override
    public void dispose()
    {
        Disposer.dispose(panel);
    }
}
