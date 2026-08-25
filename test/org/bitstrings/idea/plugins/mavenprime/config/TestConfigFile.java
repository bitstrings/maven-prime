package org.bitstrings.idea.plugins.mavenprime.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

        VirtualFile directory = loadRootIntoVfs(project);

        if (directory == null)
        {
            throw new IllegalStateException(
                "the fixture project has no reactor root, so " + MavenPrimeConfigService.FILE_NAME
                    + " could not be created");
        }

        WriteAction.run(() -> saveText(directory, json));

        service.invalidate();

        if (service.findConfigFile() == null)
        {
            throw new IllegalStateException(
                MavenPrimeConfigService.FILE_NAME + " was written beside " + directory.getPath()
                    + ", which is not the root the service reads");
        }
    }

    private static void saveText(VirtualFile directory, String json)
        throws IOException
    {
        VirtualFile file = directory.findChild(MavenPrimeConfigService.FILE_NAME);

        VfsUtil.saveText(
            (file == null)
                ? directory.createChildData(TestConfigFile.class, MavenPrimeConfigService.FILE_NAME)
                : file,
            json);
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

    private static VirtualFile loadRootIntoVfs(Project project)
        throws IOException
    {
        String basePath = project.getBasePath();

        if (basePath == null)
        {
            return null;
        }

        Files.createDirectories(Path.of(basePath));

        return LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath);
    }
}
