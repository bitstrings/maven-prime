package org.bitstrings.idea.plugins.mavenprime.editor;

import org.bitstrings.idea.plugins.mavenprime.model.ModelOrigin;

import com.intellij.codeInsight.hints.declarative.InlayActionHandler;
import com.intellij.codeInsight.hints.declarative.InlayActionPayload;
import com.intellij.codeInsight.hints.declarative.StringInlayActionPayload;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

public final class ModelOriginNavigator
    implements InlayActionHandler
{
    @Override
    public void handleClick(EditorMouseEvent event, InlayActionPayload payload)
    {
        Project project = event.getEditor().getProject();

        if ((project == null) || !(payload instanceof StringInlayActionPayload))
        {
            return;
        }

        ModelOrigin origin = ModelOriginPayloads.decode(((StringInlayActionPayload) payload).getText());

        if (!origin.isNavigable())
        {
            return;
        }

        VirtualFile declaringFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(origin.file());

        if (declaringFile != null)
        {
            new OpenFileDescriptor(project, declaringFile, Math.max(0, origin.line() - 1), 0).navigate(true);
        }
    }
}
