package org.bitstrings.idea.plugins.mavenprime.execution;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.build.SpyHandshake;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenVersion;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.execution.ParametersListUtil;

public class IdeaMavenEnginePlatformTest
    extends BasePlatformTestCase
{
    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            MavenPrimeSettings.getInstance(getProject()).colorConsole = new MavenPrimeSettings().colorConsole;
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testCreateSettings_colorConsoleOn_asksMavenForColor()
        throws IOException
    {
        MavenPrimeSettings.getInstance(getProject()).colorConsole = true;

        List<String> options = cmdOptions(request(), Set.of());

        assertTrue(options.toString(), options.contains("-Dstyle.color=always"));
    }

    public void testCreateSettings_colorConsoleOff_asksMavenForNoColor()
        throws IOException
    {
        MavenPrimeSettings.getInstance(getProject()).colorConsole = false;

        List<String> options = cmdOptions(request(), Set.of());

        assertTrue(options.toString(), options.contains("-Dstyle.color=never"));
    }

    public void testCreateSettings_aFlagTheLauncherAdvertises_rendersIt()
        throws IOException
    {
        MavenPrimeRequest request = request();

        request.flags.add(MavenFlag.RESUME);

        List<String> options = cmdOptions(request, Set.of("-r"));

        assertTrue(options.toString(), options.contains("-r"));
    }

    public void testCreateSettings_aFlagTheLauncherNeverAdvertised_dropsIt()
        throws IOException
    {
        MavenPrimeRequest request = request();

        request.flags.add(MavenFlag.RESUME);

        List<String> options = cmdOptions(request, Set.of("-o"));

        assertFalse(options.toString(), options.contains("-r"));
    }

    private List<String> cmdOptions(MavenPrimeRequest request, Set<String> advertisedOptions)
        throws IOException
    {
        RunnerAndConfigurationSettings settings =
            new IdeaMavenEngine(getProject(), SpyHandshake.NONE, advertisedOptions)
                .createSettings(request, installation());

        return ParametersListUtil.parse(
            ((MavenRunConfiguration) settings.getConfiguration()).getRunnerParameters().getCmdOptions());
    }

    private static MavenPrimeRequest request()
        throws IOException
    {
        MavenPrimeRequest request =
            MavenPrimeRequest.in(FileUtil.createTempDirectory("mavenprime", "work").getPath(), List.of("verify"));

        request.name = "engine";

        return request;
    }

    private static MavenInstallation installation()
        throws IOException
    {
        return MavenInstallation.maven(
            FileUtil.createTempDirectory("mavenprime", "maven").toPath(), MavenVersion.parse("3.9.11"));
    }
}
