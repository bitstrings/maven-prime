package org.bitstrings.idea.plugins.mavenprime.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.build.SpyHandshake;
import org.bitstrings.idea.plugins.mavenprime.build.SpyInjection;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;
import org.jetbrains.idea.maven.execution.MavenExecutionOptions.ChecksumPolicy;
import org.jetbrains.idea.maven.execution.MavenExecutionOptions.FailureMode;
import org.jetbrains.idea.maven.execution.MavenExecutionOptions.LoggingLevel;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenInSpecificPath;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenServerManager;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.project.Project;
import com.intellij.util.execution.ParametersListUtil;

public final class IdeaMavenEngine
    implements ExecutionEngine
{
    private final Project project;

    private final SpyHandshake handshake;

    public IdeaMavenEngine(Project project, SpyHandshake handshake)
    {
        this.project = project;
        this.handshake = handshake;
    }

    @Override
    public RunnerAndConfigurationSettings createSettings(MavenPrimeRequest request, MavenInstallation installation)
    {
        MavenRunnerParameters parameters =
            new MavenRunnerParameters(
                request.workingDirectory, request.getPomFileName(), true, request.goals, request.profiles);

        parameters.setProjectsCmdOptionValues(new ArrayList<>(request.projects));
        parameters.setCmdOptions(ParametersListUtil.join(residualOptions(request, installation)));

        return MavenRunConfigurationType.createRunnerAndConfigurationSettings(
            generalSettings(request, installation),
            runnerSettings(request),
            parameters,
            project,
            StringUtils.defaultIfBlank(
                request.name, MavenRunConfigurationType.generateName(project, parameters)),
            true);
    }

    private MavenGeneralSettings generalSettings(MavenPrimeRequest request, MavenInstallation installation)
    {
        MavenGeneralSettings settings = MavenProjectsManager.getInstance(project).getGeneralSettings().clone();

        enableIfRequested(request, MavenFlag.OFFLINE, settings::setWorkOffline);
        enableIfRequested(request, MavenFlag.UPDATE_SNAPSHOTS, settings::setAlwaysUpdateSnapshots);
        enableIfRequested(request, MavenFlag.NON_RECURSIVE, settings::setNonRecursive);
        enableIfRequested(request, MavenFlag.SHOW_ERRORS, settings::setPrintErrorStackTraces);

        ChecksumPolicy checksumPolicy = checksumPolicy(request);

        if (checksumPolicy != ChecksumPolicy.NOT_SET)
        {
            settings.setChecksumPolicy(checksumPolicy);
        }

        if (request.failureBehavior != FailureBehavior.DEFAULT)
        {
            settings.setFailureBehavior(failureMode(request.failureBehavior));
        }

        LoggingLevel outputLevel = loggingLevel(request.outputLevel);

        if (outputLevel != null)
        {
            settings.setOutputLevel(outputLevel);
        }

        if (StringUtils.isNotBlank(request.threads))
        {
            settings.setThreads(request.threads.trim());
        }

        if (StringUtils.isNotBlank(request.settingsFile))
        {
            settings.setUserSettingsFile(request.settingsFile.trim());
        }

        if (installation.getHome().isPresent())
        {
            settings.setMavenHomeType(new MavenInSpecificPath(installation.getHome().get().toString()));
        }

        return settings;
    }

    private MavenRunnerSettings runnerSettings(MavenPrimeRequest request)
    {
        MavenRunnerSettings settings = MavenRunner.getInstance(project).getSettings().clone();

        settings.setSkipTests(request.flags.contains(MavenFlag.SKIP_TESTS));

        settings.setMavenProperties(mavenProperties(request));

        if (StringUtils.isNotBlank(request.vmOptions))
        {
            settings.setVmOptions(request.vmOptions.trim());
        }

        if (StringUtils.isNotBlank(request.jreName))
        {
            settings.setJreName(request.jreName.trim());
        }

        return settings;
    }

    private Map<String, String> mavenProperties(MavenPrimeRequest request)
    {
        Map<String, String> properties = new LinkedHashMap<>(request.properties);

        properties.putAll(SpyInjection.propertiesFor(handshake, extClassPath(properties)));

        return properties;
    }

    private static String extClassPath(Map<String, String> properties)
    {
        String declared = properties.get(SpyProtocol.EXT_CLASS_PATH_PROPERTY);

        File listener = MavenServerManager.Companion.getInstance().getMavenEventListener();

        if (listener == null)
        {
            return declared;
        }

        return StringUtils.isBlank(declared)
            ? listener.getPath()
            : (listener.getPath() + File.pathSeparatorChar + declared.trim());
    }

    private static void enableIfRequested(MavenPrimeRequest request, MavenFlag flag, Consumer<Boolean> setting)
    {
        if (request.flags.contains(flag))
        {
            setting.accept(Boolean.TRUE);
        }
    }

    private static ChecksumPolicy checksumPolicy(MavenPrimeRequest request)
    {
        if (request.flags.contains(MavenFlag.STRICT_CHECKSUMS))
        {
            return ChecksumPolicy.FAIL;
        }

        return request.flags.contains(MavenFlag.LAX_CHECKSUMS) ? ChecksumPolicy.WARN : ChecksumPolicy.NOT_SET;
    }

    private static FailureMode failureMode(FailureBehavior failureBehavior)
    {
        return switch (failureBehavior)
        {
            case FAIL_FAST -> FailureMode.FAST;
            case FAIL_AT_END -> FailureMode.AT_END;
            case FAIL_NEVER -> FailureMode.NEVER;
            case DEFAULT -> FailureMode.NOT_SET;
        };
    }

    private static LoggingLevel loggingLevel(OutputLevel outputLevel)
    {
        return switch (outputLevel)
        {
            case DEBUG -> LoggingLevel.DEBUG;
            case QUIET, DEFAULT -> null;
        };
    }

    private List<String> residualOptions(MavenPrimeRequest request, MavenInstallation installation)
    {
        List<MavenFlag> residual = new ArrayList<>();

        for (MavenFlag flag : request.flags)
        {
            if (!isNativelyMapped(flag) && flag.isSupportedBy(installation.getMavenVersion(), false))
            {
                residual.add(flag);
            }
        }

        List<String> options = new ArrayList<>(CommandLineRenderer.renderFlags(residual));

        if (request.outputLevel == OutputLevel.QUIET)
        {
            options.addAll(CommandLineRenderer.renderOutputLevel(request.outputLevel));
        }

        options.addAll(CommandLineRenderer.renderRawOptions(request.rawOptions));
        options.addAll(
            MavenColor.optionsFor(installation, MavenPrimeSettings.getInstance(project).colorConsole));

        return options;
    }

    private static boolean isNativelyMapped(MavenFlag flag)
    {
        return switch (flag)
        {
            case OFFLINE, UPDATE_SNAPSHOTS, NON_RECURSIVE, SHOW_ERRORS, SKIP_TESTS, STRICT_CHECKSUMS, LAX_CHECKSUMS ->
                true;
            case NO_SNAPSHOT_UPDATES, ALSO_MAKE, ALSO_MAKE_DEPENDENTS, BATCH_MODE, NO_TRANSFER_PROGRESS,
                IGNORE_TRANSITIVE_REPOSITORIES, RESUME, NO_DAEMON ->
                false;
        };
    }
}
