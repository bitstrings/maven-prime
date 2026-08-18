package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.SystemProperties;

public final class MvndDownloadTask
{
    private static final Logger LOG = Logger.getInstance(MvndDownloadTask.class);

    private MvndDownloadTask()
    {
    }

    public static void start(Project project, MvndDownload download, Consumer<Path> onInstalled)
    {
        ProgressManager
            .getInstance()
            .run(
                new Task.Backgroundable(
                    project,
                    MavenPrimeBundle.message("mavenprime.daemon.download.title", download.version()),
                    true)
                {
                    @Override
                    public void run(ProgressIndicator indicator)
                    {
                        install(project, download, indicator, onInstalled);
                    }
                });
    }

    private static void install(
        Project project, MvndDownload download, ProgressIndicator indicator, Consumer<Path> onInstalled)
    {
        try
        {
            Path home =
                MvndInstaller.install(
                    download, Paths.get(SystemProperties.getUserHome()), indicator);

            MavenPrimeNotifications.info(
                project,
                MavenPrimeBundle.message("mavenprime.daemon.download.done", download.version(), home));

            ApplicationManager
                .getApplication()
                .invokeLater(() -> onInstalled.accept(home), project.getDisposed());
        }
        catch (IOException failure)
        {
            LOG.warn("Maven Prime could not install " + download.archiveName(), failure);

            MavenPrimeNotifications.error(
                project,
                MavenPrimeBundle.message(
                    "mavenprime.daemon.download.failed",
                    download.archiveName(),
                    String.valueOf(failure.getMessage())));
        }
    }
}
