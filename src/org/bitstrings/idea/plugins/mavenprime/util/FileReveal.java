package org.bitstrings.idea.plugins.mavenprime.util;

import java.nio.file.Files;
import java.nio.file.Path;

import com.intellij.ide.actions.RevealFileAction;

public final class FileReveal
{
    private FileReveal()
    {
    }

    public static void reveal(Path location)
    {
        if (location == null)
        {
            return;
        }

        if (Files.isDirectory(location))
        {
            RevealFileAction.openDirectory(location);
        }
        else
        {
            RevealFileAction.openFile(location);
        }
    }
}
