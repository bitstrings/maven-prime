package org.bitstrings.idea.plugins.mavenprime.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

public final class TestConfigFile
{
    private TestConfigFile()
    {
    }

    public static void write(Project project, String json)
        throws IOException
    {
        MavenPrimeConfigService service = MavenPrimeConfigService.getInstance(project);

        loadRootIntoVfs(project);

        service.setGoals(List.of(GoalDefinition.of("placeholder", "validate")));

        VirtualFile file = service.findConfigFile();

        if (file == null)
        {
            throw new IllegalStateException(
                "the fixture project has no reactor root, so " + MavenPrimeConfigService.FILE_NAME
                    + " could not be created");
        }

        WriteAction.run(() -> VfsUtil.saveText(file, json));

        service.invalidate();
    }

    public static void delete(Project project, Object requestor)
        throws IOException
    {
        MavenPrimeConfigService service = MavenPrimeConfigService.getInstance(project);

        loadRootIntoVfs(project);

        VirtualFile file = service.findConfigFile();

        if (file != null)
        {
            WriteAction.run(() -> file.delete(requestor));
        }

        service.invalidate();
    }

    private static void loadRootIntoVfs(Project project)
        throws IOException
    {
        String basePath = project.getBasePath();

        if (basePath == null)
        {
            return;
        }

        Files.createDirectories(Path.of(basePath));

        LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath);
    }
}
