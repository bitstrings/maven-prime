package org.bitstrings.idea.plugins.mavenprime.run;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenPrimeTestDebugRunnerPlatformTest
    extends BasePlatformTestCase
{
    private static final MavenPrimeTestDebugRunner RUNNER = new MavenPrimeTestDebugRunner();

    public void testGetRunner_aMavenPrimeRunSelectingTests_resolvesToTheMavenPrimeDebugRunner()
    {
        ProgramRunner<?> runner =
            ProgramRunner.getRunner(
                DefaultDebugExecutor.EXECUTOR_ID, configurationFor(MavenPrimeRequest.TEST_PROPERTY));

        assertNotNull("without a registered runner the Debug action has nothing to launch", runner);
        assertEquals(MavenPrimeTestDebugRunner.RUNNER_ID, runner.getRunnerId());
    }

    public void testGetRunner_aMavenPrimeRunSelectingNoTests_findsNoDebugRunner()
    {
        assertNull(
            "a goal run debugs the Maven JVM, which stops in nothing the user wrote, so offering Debug "
                + "there promises what it cannot do",
            ProgramRunner.getRunner(DefaultDebugExecutor.EXECUTOR_ID, goalConfiguration()));
    }

    public void testCanRun_theRunExecutor_leavesItToThePlainRunPath()
    {
        assertFalse(
            "claiming the Run executor would open a debugger for a plain run",
            RUNNER.canRun(
                DefaultRunExecutor.EXECUTOR_ID, configurationFor(MavenPrimeRequest.TEST_PROPERTY)));
    }

    public void testCanRun_anIntegrationTestSelection_claimsTheDebugExecutor()
    {
        assertTrue(
            "failsafe forks a JVM the same way surefire does, so it debugs the same way",
            RUNNER.canRun(
                DefaultDebugExecutor.EXECUTOR_ID,
                configurationFor(MavenPrimeRequest.INTEGRATION_TEST_PROPERTY)));
    }

    private MavenPrimeRunConfiguration configurationFor(String selector)
    {
        MavenPrimeRunConfiguration configuration = goalConfiguration();

        MavenPrimeRequest request = MavenPrimeRequest.in("/repo", List.of("test"));

        request.properties.put(selector, "CartTest");

        configuration.setRequest(request);

        return configuration;
    }

    private MavenPrimeRunConfiguration goalConfiguration()
    {
        RunnerAndConfigurationSettings settings =
            RunManager
                .getInstance(getProject())
                .createConfiguration(
                    "debug runner", MavenPrimeRunConfigurationType.getInstance().getFactory());

        MavenPrimeRunConfiguration configuration = (MavenPrimeRunConfiguration) settings.getConfiguration();

        configuration.setRequest(MavenPrimeRequest.in("/repo", List.of("package")));

        return configuration;
    }
}
