package org.bitstrings.idea.plugins.mavenprime.daemon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallationService;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenCommandLineBuilder;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

public final class DaemonCommandRunner
{
    private static final int TIMEOUT_MILLIS = 60_000;

    private DaemonCommandRunner()
    {
    }

    public static void runInBackground(
        Project project, DistributionSpec spec, DaemonCommand command, Consumer<String> onFinished)
    {
        ProgressManager
            .getInstance()
            .run(
                new Task.Backgroundable(project, MavenPrimeBundle.message("mavenprime.daemon.task", command.getTitle()))
                {
                    @Override
                    public void run(ProgressIndicator indicator)
                    {
                        String output =
                            execute(
                                project,
                                MavenInstallationService.getInstance(project).resolve(spec, null),
                                command);

                        ApplicationManager
                            .getApplication()
                            .invokeLater(() -> onFinished.accept(output), project.getDisposed());
                    }
                });
    }

    private static String execute(Project project, MavenInstallation installation, DaemonCommand command)
    {
        if (!installation.isValid())
        {
            return installation.getError();
        }

        Optional<Path> executable = installation.getExecutable();

        if (executable.isEmpty())
        {
            return MavenPrimeBundle.message("mavenprime.execution.error.noExecutable");
        }

        GeneralCommandLine commandLine = new GeneralCommandLine(executable.get().toString());

        commandLine.addParameter(command.getOption());
        commandLine.setCharset(StandardCharsets.UTF_8);

        MavenCommandLineBuilder.applyDumbTerminal(commandLine);

        if (StringUtils.isNotBlank(project.getBasePath()))
        {
            commandLine.setWorkDirectory(project.getBasePath());
        }

        MavenCommandLineBuilder.applyJavaHome(
            commandLine, project, BuildContext.getInstance(project).getEnvironment().jreName, true);

        try
        {
            ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(TIMEOUT_MILLIS, true);

            return StringUtils.defaultIfBlank(
                reportOf(output), MavenPrimeBundle.message("mavenprime.daemon.noOutput"));
        }
        catch (ExecutionException ex)
        {
            return MavenPrimeBundle.message("mavenprime.daemon.failed", ex.getMessage());
        }
    }

    static String reportOf(ProcessOutput output)
    {
        String reported = StringUtils.trimToEmpty(output.getStdout());

        if (output.getExitCode() == 0)
        {
            return reported;
        }

        return StringUtils.trimToEmpty(reported + System.lineSeparator() + output.getStderr());
    }
}
