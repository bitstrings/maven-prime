package org.bitstrings.idea.plugins.mavenprime.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.build.SpyHandshake;
import org.bitstrings.idea.plugins.mavenprime.build.SpyInjection;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.SystemProperties;

public final class MavenCommandLineBuilder
{
    private static final String RAW_STREAMS_OPTION = "--raw-streams";

    private static final String JAVA_HOME_PROPERTY = "-Djava.home=";

    private static final String DUMB_TERMINAL_PROPERTY = "-Dorg.jline.terminal.dumb=true";

    private static final String PROPERTY_PREFIX = "-D";

    private static final String EXT_CLASS_PATH_OPTION =
        PROPERTY_PREFIX + SpyProtocol.EXT_CLASS_PATH_PROPERTY + '=';

    private static final String DAEMON_JVM_ARGS_PROPERTY = "-Dmvnd.jvmArgs=";

    private static final String JAVA_HOME_VARIABLE = "JAVA_HOME";

    private static final String MAVEN_OPTS_VARIABLE = "MAVEN_OPTS";

    private MavenCommandLineBuilder()
    {
    }

    public static GeneralCommandLine build(
        Project project, MavenPrimeRequest request, MavenInstallation installation, SpyHandshake handshake)
        throws ExecutionException
    {
        RequestProblem problem = request.validate();

        if (problem != null)
        {
            throw new ExecutionException(problem.getMessage());
        }

        Path launcher =
            installation
                .getLauncher()
                .orElseThrow(
                    () -> new ExecutionException(MavenPrimeBundle.message("mavenprime.execution.error.noExecutable")));

        GeneralCommandLine commandLine = new GeneralCommandLine(launcher.toString());

        commandLine.setWorkDirectory(request.workingDirectory);
        commandLine.setCharset(StandardCharsets.UTF_8);
        commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);

        List<String> arguments = CommandLineRenderer.render(request, installation).getArguments();

        applyTerminal(commandLine, installation, MavenPrimeSettings.getInstance(project).colorConsole);
        applyJavaHome(commandLine, project, request.jreName, installation.isDaemon());
        applyVmOptions(commandLine, request, installation);

        commandLine.addParameters(arguments);

        applyEventSpy(commandLine, handshake, declaredExtClassPath(arguments));

        return commandLine;
    }

    private static String declaredExtClassPath(List<String> arguments)
    {
        String declared = null;

        for (String argument : arguments)
        {
            if (argument.startsWith(EXT_CLASS_PATH_OPTION))
            {
                declared = argument.substring(EXT_CLASS_PATH_OPTION.length());
            }
        }

        return declared;
    }

    private static void applyTerminal(
        GeneralCommandLine commandLine, MavenInstallation installation, boolean color)
    {
        List<String> colorOptions = MavenColor.optionsFor(installation, color);

        if (installation.isDaemon())
        {
            commandLine.addParameter(RAW_STREAMS_OPTION);
            commandLine.addParameters(colorOptions);

            applyDumbTerminal(commandLine);

            return;
        }

        commandLine.addParameters(colorOptions);
    }

    static Optional<Path> usable(String path)
    {
        return Optional.ofNullable(StringUtils.trimToNull(path)).map(Paths::get).filter(
            MavenCommandLineBuilder::exists);
    }

    private static boolean exists(Path javaHome)
    {
        return Files.isDirectory(javaHome);
    }

    public static void applyDumbTerminal(GeneralCommandLine commandLine)
    {
        commandLine.addParameter(DUMB_TERMINAL_PROPERTY);
    }

    private static void applyVmOptions(
        GeneralCommandLine commandLine, MavenPrimeRequest request, MavenInstallation installation)
    {
        if (StringUtils.isBlank(request.vmOptions))
        {
            return;
        }

        if (installation.isDaemon())
        {
            commandLine.addParameter(DAEMON_JVM_ARGS_PROPERTY + request.vmOptions.trim());

            return;
        }

        commandLine.withEnvironment(
            MAVEN_OPTS_VARIABLE, mergedMavenOpts(EnvironmentUtil.getValue(MAVEN_OPTS_VARIABLE), request.vmOptions));
    }

    static String mergedMavenOpts(String inherited, String requested)
    {
        String added = StringUtils.trimToEmpty(requested);

        return StringUtils.isBlank(inherited) ? added : (inherited.trim() + ' ' + added);
    }

    private static void applyEventSpy(
        GeneralCommandLine commandLine, SpyHandshake handshake, String declared)
    {
        SpyInjection
            .propertiesFor(handshake, declared)
            .forEach((key, value) -> commandLine.addParameter(PROPERTY_PREFIX + key + '=' + value));
    }

    public static void applyJavaHome(
        GeneralCommandLine commandLine, Project project, String jreName, boolean asDaemonDiscriminator)
    {
        Optional<Path> javaHome =
            JdkResolver
                .resolveJavaHome(project, jreName)
                .filter(MavenCommandLineBuilder::exists)
                .or(() -> usable(SystemProperties.getJavaHome()));

        if (javaHome.isEmpty())
        {
            return;
        }

        if (asDaemonDiscriminator)
        {
            commandLine.addParameter(JAVA_HOME_PROPERTY + javaHome.get());
        }

        commandLine.withEnvironment(JAVA_HOME_VARIABLE, javaHome.get().toString());
    }
}
