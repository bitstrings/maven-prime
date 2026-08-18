package org.bitstrings.idea.plugins.mavenprime.run;

import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenPrimeRunnerPlatformTest
    extends BasePlatformTestCase
{
    public void testGetRunner_aMavenPrimeConfiguration_isClaimedByARunner()
    {
        assertNotNull(
            "no ProgramRunner accepts a Maven Prime configuration, so nothing happens when it is executed",
            ProgramRunner.getRunner(ExecutionMode.RUN.getExecutor().getId(), configuration()));
    }

    private RunConfiguration configuration()
    {
        RunnerAndConfigurationSettings settings =
            RunManager
                .getInstance(getProject())
                .createConfiguration(
                    "clean install", MavenPrimeRunConfigurationType.getInstance().getFactory());

        return settings.getConfiguration();
    }
}
