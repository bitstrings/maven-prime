package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.Arrays;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceLog;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.FileContentUtil;

@Service(Service.Level.PROJECT)
public final class EffectiveModelHintRefresher
    implements ProvenanceLog.ProvenanceReadListener
{
    private final Project project;

    public EffectiveModelHintRefresher(Project project)
    {
        this.project = project;
    }

    public static EffectiveModelHintRefresher getInstance(Project project)
    {
        return project.getService(EffectiveModelHintRefresher.class);
    }

    public void listen()
    {
        project.getMessageBus().connect(project).subscribe(ProvenanceLog.READ_TOPIC, this);
    }

    @Override
    public void provenanceRead()
    {
        List<VirtualFile> poms = openPoms();

        if (!poms.isEmpty())
        {
            FileContentUtil.reparseFiles(project, poms, true);
        }
    }

    List<VirtualFile> openPoms()
    {
        if (project.isDisposed())
        {
            return List.of();
        }

        return Arrays
            .stream(FileEditorManager.getInstance(project).getOpenFiles())
            .filter(this::isPom)
            .toList();
    }

    private boolean isPom(VirtualFile file)
    {
        MavenProject module = MavenProjects.byPomFile(project, file);

        return (module != null) && file.equals(module.getFile());
    }
}
