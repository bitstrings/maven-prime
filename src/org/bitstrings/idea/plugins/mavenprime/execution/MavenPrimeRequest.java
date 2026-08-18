package org.bitstrings.idea.plugins.mavenprime.execution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.jetbrains.idea.maven.model.MavenConstants;

import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.xmlb.annotations.Transient;

public final class MavenPrimeRequest
{
    public static final String DEFAULT_POM_FILE_NAME = MavenConstants.POM_XML;

    public static final String TEST_PROPERTY = "test";

    public static final String INTEGRATION_TEST_PROPERTY = "it.test";

    public String name;

    public String workingDirectory;

    public String pomFileName = DEFAULT_POM_FILE_NAME;

    public String settingsFile;

    public final List<String> goals = new ArrayList<>();

    public final Map<String, Boolean> profiles = new LinkedHashMap<>();

    public final Map<String, String> properties = new LinkedHashMap<>();

    public final Set<MavenFlag> flags = new LinkedHashSet<>();

    public FailureBehavior failureBehavior = FailureBehavior.DEFAULT;

    public OutputLevel outputLevel = OutputLevel.DEFAULT;

    public String threads;

    public final List<String> projects = new ArrayList<>();

    public String rawOptions;

    public DistributionSpec distribution = new DistributionSpec();

    public String jreName;

    public String vmOptions;

    public MavenPrimeRequest()
    {
    }

    public static MavenPrimeRequest in(String workingDirectory, List<String> goals)
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.workingDirectory = workingDirectory;
        request.goals.addAll(goals);

        return request;
    }

    public static List<String> parseGoals(String goalLine)
    {
        return StringUtils.isBlank(goalLine) ? List.of() : ParametersListUtil.parse(goalLine.trim());
    }

    @Transient
    public String getGoalLine()
    {
        return ParametersListUtil.join(goals);
    }

    @Transient
    public void setGoalLine(String goalLine)
    {
        goals.clear();
        goals.addAll(parseGoals(goalLine));
    }

    public boolean hasGoals()
    {
        return !goals.isEmpty();
    }

    @Transient
    public boolean selectsTests()
    {
        return properties.containsKey(TEST_PROPERTY) || properties.containsKey(INTEGRATION_TEST_PROPERTY);
    }

    public RequestProblem validate()
    {
        if (!hasGoals())
        {
            return RequestProblem.NO_GOALS;
        }

        return StringUtils.isBlank(workingDirectory) ? RequestProblem.NO_WORKING_DIRECTORY : null;
    }

    public String getPomFileName()
    {
        return StringUtils.defaultIfBlank(pomFileName, DEFAULT_POM_FILE_NAME);
    }

    public MavenPrimeRequest copy()
    {
        MavenPrimeRequest copy = new MavenPrimeRequest();

        copy.name = name;
        copy.workingDirectory = workingDirectory;
        copy.pomFileName = pomFileName;
        copy.settingsFile = settingsFile;
        copy.goals.addAll(goals);
        copy.profiles.putAll(profiles);
        copy.properties.putAll(properties);
        copy.flags.addAll(flags);
        copy.failureBehavior = failureBehavior;
        copy.outputLevel = outputLevel;
        copy.threads = threads;
        copy.projects.addAll(projects);
        copy.rawOptions = rawOptions;
        copy.distribution = distribution.copy();
        copy.jreName = jreName;
        copy.vmOptions = vmOptions;

        return copy;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof MavenPrimeRequest))
        {
            return false;
        }

        MavenPrimeRequest that = (MavenPrimeRequest) other;

        return Objects.equals(workingDirectory, that.workingDirectory)
            && Objects.equals(pomFileName, that.pomFileName)
            && Objects.equals(settingsFile, that.settingsFile)
            && goals.equals(that.goals)
            && profiles.equals(that.profiles)
            && properties.equals(that.properties)
            && flags.equals(that.flags)
            && (failureBehavior == that.failureBehavior)
            && (outputLevel == that.outputLevel)
            && Objects.equals(threads, that.threads)
            && projects.equals(that.projects)
            && Objects.equals(rawOptions, that.rawOptions)
            && Objects.equals(distribution, that.distribution)
            && Objects.equals(jreName, that.jreName)
            && Objects.equals(vmOptions, that.vmOptions);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
            workingDirectory,
            pomFileName,
            settingsFile,
            goals,
            profiles,
            properties,
            flags,
            failureBehavior,
            outputLevel,
            threads,
            projects,
            rawOptions,
            distribution,
            jreName,
            vmOptions);
    }

    @Override
    public String toString()
    {
        return getGoalLine();
    }
}
