package org.bitstrings.idea.plugins.mavenprime.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;

import com.intellij.util.execution.ParametersListUtil;

public final class CommandLineRenderer
{
    private static final String PROFILES_OPTION = "-P";

    private static final String THREADS_OPTION = "-T";

    private static final String PROJECTS_OPTION = "-pl";

    private static final String FILE_OPTION = "-f";

    private static final String SETTINGS_OPTION = "-s";

    private static final String PROPERTY_OPTION_PREFIX = "-D";

    private static final String DISABLED_PROFILE_PREFIX = "!";

    private static final String PROFILE_SEPARATOR = ",";

    private CommandLineRenderer()
    {
    }

    public static RenderedCommandLine render(MavenPrimeRequest request, MavenInstallation installation)
    {
        Set<MavenFlag> dropped = unsupportedFlags(request.flags, installation);

        List<MavenFlag> supported = new ArrayList<>(request.flags);

        supported.removeAll(dropped);

        return new RenderedCommandLine(renderOptions(request, supported), request.goals, dropped);
    }

    public static List<String> renderOptions(MavenPrimeRequest request, Collection<MavenFlag> flags)
    {
        List<String> options = new ArrayList<>();

        options.addAll(renderPomFile(request));
        options.addAll(renderSettingsFile(request.settingsFile));
        options.addAll(renderProfiles(request.profiles));
        options.addAll(renderProperties(request.properties));
        options.addAll(renderFlags(flags));
        options.addAll(renderFailureBehavior(request.failureBehavior));
        options.addAll(renderOutputLevel(request.outputLevel));
        options.addAll(renderThreads(request.threads));
        options.addAll(renderProjects(request.projects));
        options.addAll(renderRawOptions(request.rawOptions));

        return options;
    }

    public static Set<MavenFlag> unsupportedFlags(Collection<MavenFlag> flags, MavenInstallation installation)
    {
        Set<MavenFlag> unsupported = new LinkedHashSet<>();

        for (MavenFlag flag : flags)
        {
            if (!flag.isSupportedBy(installation.getMavenVersion(), installation.isDaemon()))
            {
                unsupported.add(flag);
            }
        }

        return unsupported;
    }

    public static List<String> renderFlags(Collection<MavenFlag> flags)
    {
        List<String> options = new ArrayList<>(flags.size());

        for (MavenFlag flag : flags)
        {
            options.add(flag.getOption());
        }

        return options;
    }

    public static List<String> renderProfiles(Map<String, Boolean> profiles)
    {
        String encoded = encodeProfiles(profiles);

        return StringUtils.isBlank(encoded) ? List.of() : List.of(PROFILES_OPTION, encoded);
    }

    public static String encodeProfiles(Map<String, Boolean> profiles)
    {
        List<String> encoded = new ArrayList<>(profiles.size());

        for (Map.Entry<String, Boolean> profile : profiles.entrySet())
        {
            if (StringUtils.isNotBlank(profile.getKey()) && (profile.getValue() != null))
            {
                encoded.add(
                    Boolean.FALSE.equals(profile.getValue())
                        ? (DISABLED_PROFILE_PREFIX + profile.getKey().trim())
                        : profile.getKey().trim());
            }
        }

        return String.join(PROFILE_SEPARATOR, encoded);
    }

    public static List<String> renderProperties(Map<String, String> properties)
    {
        List<String> options = new ArrayList<>(properties.size());

        for (Map.Entry<String, String> property : properties.entrySet())
        {
            if (StringUtils.isNotBlank(property.getKey()))
            {
                options.add(renderProperty(property.getKey(), property.getValue()));
            }
        }

        return options;
    }

    public static String renderProperty(String key, String value)
    {
        return StringUtils.isEmpty(value)
            ? (PROPERTY_OPTION_PREFIX + key)
            : (PROPERTY_OPTION_PREFIX + key + '=' + value);
    }

    public static List<String> renderFailureBehavior(FailureBehavior failureBehavior)
    {
        return failureBehavior.hasOption() ? List.of(failureBehavior.getOption()) : List.of();
    }

    public static List<String> renderOutputLevel(OutputLevel outputLevel)
    {
        return outputLevel.hasOption() ? List.of(outputLevel.getOption()) : List.of();
    }

    public static List<String> renderThreads(String threads)
    {
        return StringUtils.isBlank(threads) ? List.of() : List.of(THREADS_OPTION, threads.trim());
    }

    public static List<String> renderProjects(List<String> projects)
    {
        return projects.isEmpty()
            ? List.of()
            : List.of(PROJECTS_OPTION, String.join(PROFILE_SEPARATOR, projects));
    }

    public static List<String> renderRawOptions(String rawOptions)
    {
        return StringUtils.isBlank(rawOptions) ? List.of() : ParametersListUtil.parse(rawOptions.trim());
    }

    public static List<String> renderSettingsFile(String settingsFile)
    {
        return StringUtils.isBlank(settingsFile) ? List.of() : List.of(SETTINGS_OPTION, settingsFile.trim());
    }

    public static List<String> renderPomFile(MavenPrimeRequest request)
    {
        return MavenPrimeRequest.DEFAULT_POM_FILE_NAME.equals(request.getPomFileName())
            ? List.of()
            : List.of(FILE_OPTION, request.getPomFileName());
    }
}
