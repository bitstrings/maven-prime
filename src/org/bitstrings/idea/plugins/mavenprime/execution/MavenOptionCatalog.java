package org.bitstrings.idea.plugins.mavenprime.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class MavenOptionCatalog
{
    private static final Logger LOG = Logger.getInstance(MavenOptionCatalog.class);

    private static final String HELP_OPTION = "-h";

    private static final int TIMEOUT_MILLIS = 15_000;

    private static final Pattern ADVERTISED_OPTION =
        Pattern.compile("^\\s+(-{1,2}[A-Za-z][\\w.-]*)(?:,\\s*(--[\\w-]+))?(?=[\\s=]|$)");

    private final Project project;

    private final Map<String, Set<String>> cache = new ConcurrentHashMap<>();

    public MavenOptionCatalog(Project project)
    {
        this.project = project;
    }

    public static MavenOptionCatalog getInstance(Project project)
    {
        return project.getService(MavenOptionCatalog.class);
    }

    public Set<String> optionsOf(MavenInstallation installation, String jreName)
    {
        Optional<Path> launcher = installation.getLauncher();

        if (launcher.isEmpty())
        {
            return Set.of();
        }

        return cache.computeIfAbsent(
            launcher.get().toString(), path -> probe(path, jreName, installation.isDaemon()));
    }

    public void invalidate()
    {
        cache.clear();
    }

    private Set<String> probe(String launcher, String jreName, boolean daemon)
    {
        GeneralCommandLine commandLine = new GeneralCommandLine(launcher);

        commandLine.addParameter(HELP_OPTION);
        commandLine.setCharset(StandardCharsets.UTF_8);

        MavenCommandLineBuilder.applyDumbTerminal(commandLine);
        MavenCommandLineBuilder.applyJavaHome(commandLine, project, jreName, daemon);

        try
        {
            ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(TIMEOUT_MILLIS, true);

            return output.getExitCode() == 0 ? parseOptions(output.getStdout()) : Set.of();
        }
        catch (ExecutionException unavailable)
        {
            LOG.debug("Maven Prime could not read the options advertised by " + launcher, unavailable);

            return Set.of();
        }
    }

    public static Set<String> parseOptions(String helpText)
    {
        Set<String> options = new LinkedHashSet<>();

        helpText
            .lines()
            .forEach(
                line ->
                {
                    Matcher matcher = ADVERTISED_OPTION.matcher(line);

                    if (matcher.find())
                    {
                        options.add(matcher.group(1));

                        if (matcher.group(2) != null)
                        {
                            options.add(matcher.group(2));
                        }
                    }
                });

        return options;
    }
}
