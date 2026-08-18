package org.bitstrings.idea.plugins.mavenprime.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.execution.FailureBehavior;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.execution.OutputLevel;

public final class RequestConfig
{
    public List<String> goals;

    public String settingsFile;

    public String pomFileName;

    public DistributionConfig distribution;

    public Map<String, Boolean> profiles;

    public Map<String, String> properties;

    public List<String> flags;

    public String failureBehavior;

    public String outputLevel;

    public String threads;

    public List<String> projects;

    public String options;

    public String jre;

    public String vmOptions;

    public RequestConfig()
    {
    }

    public static RequestConfig of(MavenPrimeRequest request)
    {
        RequestConfig config = new RequestConfig();

        config.goals = List.copyOf(request.goals);
        config.settingsFile = StringUtils.trimToNull(request.settingsFile);
        config.pomFileName = request.getPomFileName().equals(MavenPrimeRequest.DEFAULT_POM_FILE_NAME)
            ? null
            : request.pomFileName;
        config.distribution =
            request.distribution.isContext() ? null : DistributionConfig.of(request.distribution);
        config.profiles = request.profiles.isEmpty() ? null : new LinkedHashMap<>(request.profiles);
        config.properties = request.properties.isEmpty() ? null : new LinkedHashMap<>(request.properties);
        config.flags = request.flags.isEmpty() ? null : names(request.flags);
        config.failureBehavior =
            (request.failureBehavior == FailureBehavior.DEFAULT) ? null : request.failureBehavior.name();
        config.outputLevel = (request.outputLevel == OutputLevel.DEFAULT) ? null : request.outputLevel.name();
        config.threads = StringUtils.trimToNull(request.threads);
        config.projects = request.projects.isEmpty() ? null : List.copyOf(request.projects);
        config.options = StringUtils.trimToNull(request.rawOptions);
        config.jre = StringUtils.trimToNull(request.jreName);
        config.vmOptions = StringUtils.trimToNull(request.vmOptions);

        return config;
    }

    private static List<String> names(Iterable<MavenFlag> flags)
    {
        List<String> encoded = new ArrayList<>();

        for (MavenFlag flag : flags)
        {
            encoded.add(flag.name());
        }

        return encoded;
    }

    public MavenPrimeRequest toRequest()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        applyTo(request);

        return request;
    }

    public void applyTo(MavenPrimeRequest request)
    {
        if (goals != null)
        {
            request.goals.clear();
            request.goals.addAll(declared(goals));
        }

        if (StringUtils.isNotBlank(settingsFile))
        {
            request.settingsFile = settingsFile.trim();
        }

        if (StringUtils.isNotBlank(pomFileName))
        {
            request.pomFileName = pomFileName.trim();
        }

        if (distribution != null)
        {
            request.distribution = distribution.toSpec();
        }

        if (profiles != null)
        {
            profiles.forEach(
                (name, enabled) ->
                {
                    if (enabled == null)
                    {
                        request.profiles.remove(name);
                    }
                    else
                    {
                        request.profiles.put(name, enabled);
                    }
                });
        }

        if (properties != null)
        {
            properties.forEach(
                (name, value) ->
                {
                    if (value == null)
                    {
                        request.properties.remove(name);
                    }
                    else
                    {
                        request.properties.put(name, value);
                    }
                });
        }

        applyFlags(request);

        if (failureBehavior != null)
        {
            request.failureBehavior =
                EnumUtils.getEnum(FailureBehavior.class, failureBehavior, request.failureBehavior);
        }

        if (outputLevel != null)
        {
            request.outputLevel = EnumUtils.getEnum(OutputLevel.class, outputLevel, request.outputLevel);
        }

        if (StringUtils.isNotBlank(threads))
        {
            request.threads = threads.trim();
        }

        if (projects != null)
        {
            request.projects.clear();
            request.projects.addAll(declared(projects));
        }

        if (StringUtils.isNotBlank(options))
        {
            request.rawOptions = options.trim();
        }

        if (StringUtils.isNotBlank(jre))
        {
            request.jreName = jre.trim();
        }

        if (StringUtils.isNotBlank(vmOptions))
        {
            request.vmOptions = vmOptions.trim();
        }
    }

    private static List<String> declared(List<String> values)
    {
        List<String> present = new ArrayList<>(values.size());

        for (String value : values)
        {
            if (StringUtils.isNotBlank(value))
            {
                present.add(value);
            }
        }

        return present;
    }

    private void applyFlags(MavenPrimeRequest request)
    {
        if (flags == null)
        {
            return;
        }

        for (String flag : flags)
        {
            MavenFlag parsed = EnumUtils.getEnum(MavenFlag.class, flag);

            if (parsed != null)
            {
                request.flags.add(parsed);
            }
        }
    }
}
