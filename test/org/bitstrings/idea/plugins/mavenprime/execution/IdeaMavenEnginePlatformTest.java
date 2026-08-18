package org.bitstrings.idea.plugins.mavenprime.execution;

import java.io.IOException;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.build.SpyHandshake;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenVersion;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

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

        assertTrue(cmdOptions(), cmdOptions().contains("-Dstyle.color=always"));
    }

    public void testCreateSettings_colorConsoleOff_asksMavenForNoColor()
        throws IOException
    {
        MavenPrimeSettings.getInstance(getProject()).colorConsole = false;

        assertTrue(cmdOptions(), cmdOptions().contains("-Dstyle.color=never"));
    }

    private String cmdOptions()
        throws IOException
    {
        RunnerAndConfigurationSettings settings =
            new IdeaMavenEngine(getProject(), SpyHandshake.NONE).createSettings(request(), installation());

        return ((MavenRunConfiguration) settings.getConfiguration()).getRunnerParameters().getCmdOptions();
    }

    private static MavenPrimeRequest request()
        throws IOException
    {
        MavenPrimeRequest request =
            MavenPrimeRequest.in(FileUtil.createTempDirectory("mavenprime", "work").getPath(), List.of("verify"));

        request.name = "color";

        return request;
    }

    private static MavenInstallation installation()
        throws IOException
    {
        return MavenInstallation.maven(
            FileUtil.createTempDirectory("mavenprime", "maven").toPath(), MavenVersion.parse("3.9.11"));
    }
}
