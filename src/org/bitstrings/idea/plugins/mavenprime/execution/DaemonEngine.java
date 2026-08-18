package org.bitstrings.idea.plugins.mavenprime.execution;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.run.MavenPrimeRunConfiguration;
import org.bitstrings.idea.plugins.mavenprime.run.MavenPrimeRunConfigurationType;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.project.Project;

public final class DaemonEngine
    implements ExecutionEngine
{
    private final Project project;

    public DaemonEngine(Project project)
    {
        this.project = project;
    }

    @Override
    public RunnerAndConfigurationSettings createSettings(MavenPrimeRequest request, MavenInstallation installation)
    {
        MavenPrimeRunConfigurationType type = MavenPrimeRunConfigurationType.getInstance();

        RunnerAndConfigurationSettings settings =
            RunManager
                .getInstance(project)
                .createConfiguration(
                    StringUtils.defaultIfBlank(request.name, request.getGoalLine()), type.getFactory());

        ((MavenPrimeRunConfiguration) settings.getConfiguration()).setRequest(request);

        return settings;
    }
}
