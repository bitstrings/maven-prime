package org.bitstrings.idea.plugins.mavenprime.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.build.SpyHandshake;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenVersion;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenCommandLineBuilderTest
    extends BasePlatformTestCase
{
    private static final int SPY_PORT = 54321;

    private static final String EXT_CLASS_PATH_OPTION = "-D" + SpyProtocol.EXT_CLASS_PATH_PROPERTY + '=';

    private static final String USER_EXTENSION = "/user/extension.jar";

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

    public void testApplyJavaHome_noJdkConfiguredAnywhere_stillPointsAtAnExistingJdk()
    {
        GeneralCommandLine commandLine = new GeneralCommandLine("mvnd");

        MavenCommandLineBuilder.applyJavaHome(commandLine, getProject(), null, true);

        String javaHome = commandLine.getEnvironment().get("JAVA_HOME");

        assertNotNull("mvnd cannot start a daemon without a JAVA_HOME", javaHome);
        assertTrue(
            "JAVA_HOME must exist, or mvnd dies before starting a daemon",
            Files.isDirectory(Paths.get(javaHome)));
    }

    public void testApplyJavaHome_aJavaHomeThatDoesNotExist_isNotHandedToTheBuild()
    {
        GeneralCommandLine commandLine = new GeneralCommandLine("mvnd");

        commandLine.withEnvironment("JAVA_HOME", "/does/not/exist");

        MavenCommandLineBuilder.applyJavaHome(commandLine, getProject(), null, true);

        assertFalse(
            "an unusable JAVA_HOME must be replaced, not passed through",
            "/does/not/exist".equals(commandLine.getEnvironment().get("JAVA_HOME")));
    }

    public void testBuild_colorConsoleOnWithPlainMaven_asksMavenForColor()
        throws Exception
    {
        colorConsole(true);

        assertTrue(parametersOf(mavenInstallation()).contains("-Dstyle.color=always"));
    }

    public void testBuild_colorConsoleOffWithPlainMaven_asksMavenForNoColor()
        throws Exception
    {
        colorConsole(false);

        assertTrue(parametersOf(mavenInstallation()).contains("-Dstyle.color=never"));
    }

    public void testBuild_colorConsoleOnWithTheDaemon_asksTheDaemonForColor()
        throws Exception
    {
        colorConsole(true);

        assertEquals("always", valueAfter(parametersOf(daemonInstallation()), "--color"));
    }

    public void testBuild_colorConsoleOffWithTheDaemon_asksTheDaemonForNoColor()
        throws Exception
    {
        colorConsole(false);

        assertEquals("never", valueAfter(parametersOf(daemonInstallation()), "--color"));
    }

    public void testBuild_colorConsoleOnWithTheDaemon_keepsTheRawStreamHardening()
        throws Exception
    {
        colorConsole(true);

        List<String> parameters = parametersOf(daemonInstallation());

        assertTrue(parameters.contains("--raw-streams"));
        assertTrue(parameters.contains("-Dorg.jline.terminal.dumb=true"));
    }

    private void colorConsole(boolean enabled)
    {
        MavenPrimeSettings.getInstance(getProject()).colorConsole = enabled;
    }

    private List<String> parametersOf(MavenInstallation installation)
        throws Exception
    {
        return commandLineFor(installation, SpyHandshake.NONE).getParametersList().getList();
    }

    private static String valueAfter(List<String> parameters, String option)
    {
        int index = parameters.indexOf(option);

        assertTrue("expected " + option + " in " + parameters, index >= 0);

        return parameters.get(index + 1);
    }

    public void testBuild_spyPortGiven_injectsTheExtensionClassPathAndThePort()
        throws Exception
    {
        List<String> parameters = parametersFor(SPY_PORT);

        assertTrue(
            "expected an -Dmaven.ext.class.path parameter in " + parameters,
            parameters.stream().anyMatch(parameter -> parameter.startsWith("-Dmaven.ext.class.path=")));
        assertTrue(
            "expected the spy port in " + parameters,
            parameters.contains("-D" + SpyProtocol.PORT_PROPERTY + '=' + SPY_PORT));
    }

    public void testBuild_spyPortGiven_pointsTheExtensionClassPathAtTheSpyJarOnly()
        throws Exception
    {
        String extension =
            parametersFor(SPY_PORT)
                .stream()
                .filter(parameter -> parameter.startsWith("-Dmaven.ext.class.path="))
                .findFirst()
                .orElseThrow();

        assertFalse("the plugin jar must never reach Maven's extension class path", extension.contains(","));
    }

    public void testBuild_userDeclaredExtensionClassPath_keepsItAlongsideTheSpyJar()
        throws Exception
    {
        List<String> parameters = parametersDeclaringAnExtension();

        String merged =
            parameters.get(lastExtensionClassPathIndex(parameters)).substring(EXT_CLASS_PATH_OPTION.length());

        assertTrue(
            "expected the user extension to survive, got " + merged, merged.contains(USER_EXTENSION));
        assertTrue("expected the spy jar alongside it, got " + merged, merged.contains(File.pathSeparator));
    }

    public void testBuild_userDeclaredExtensionClassPath_emitsTheMergedValueAfterTheUserOption()
        throws Exception
    {
        List<String> parameters = parametersDeclaringAnExtension();

        assertTrue(
            "the merged extension class path must win Maven's last-one-wins resolution",
            lastExtensionClassPathIndex(parameters)
                > parameters.indexOf(EXT_CLASS_PATH_OPTION + USER_EXTENSION));
    }

    public void testBuild_noSpyPort_injectsNothing()
        throws Exception
    {
        List<String> parameters = parametersFor(0);

        assertFalse(
            parameters.stream().anyMatch(parameter -> parameter.startsWith("-Dmaven.ext.class.path=")));
        assertFalse(
            parameters.stream().anyMatch(parameter -> parameter.contains(SpyProtocol.PORT_PROPERTY)));
    }

    public void testBuild_daemonInstallation_silencesTheJlineDumbTerminalWarning()
        throws Exception
    {
        assertTrue(parametersFor(0).contains("-Dorg.jline.terminal.dumb=true"));
    }

    public void testBuild_plainMavenInstallation_launchesTheMavenScriptRatherThanMvnd()
        throws Exception
    {
        GeneralCommandLine commandLine = commandLineFor(mavenInstallation(), SpyHandshake.NONE);

        assertTrue(
            "expected a Maven launcher, got " + commandLine.getExePath(),
            commandLine.getExePath().contains("bin"));
    }

    public void testBuild_plainMavenInstallation_doesNotPassTheDaemonOnlyOptions()
        throws Exception
    {
        List<String> parameters = commandLineFor(mavenInstallation(), SpyHandshake.NONE).getParametersList().getList();

        assertFalse(parameters.contains("--raw-streams"));
        assertFalse(parameters.contains("-Dorg.jline.terminal.dumb=true"));
    }

    public void testBuild_plainMavenInstallationWithVmOptions_passesThemThroughMavenOpts()
        throws Exception
    {
        MavenPrimeRequest request = requestIn(List.of("verify"));

        request.vmOptions = "-Xmx2g";

        GeneralCommandLine commandLine =
            MavenCommandLineBuilder.build(getProject(), request, mavenInstallation(), SpyHandshake.NONE);

        assertTrue(commandLine.getEnvironment().get("MAVEN_OPTS").endsWith("-Xmx2g"));
        assertFalse(commandLine.getParametersList().getList().contains("-Dmvnd.jvmArgs=-Xmx2g"));
    }

    public void testBuild_daemonInstallationWithVmOptions_passesThemAsDaemonJvmArgs()
        throws Exception
    {
        MavenPrimeRequest request = requestIn(List.of("verify"));

        request.vmOptions = "-Xmx2g";

        GeneralCommandLine commandLine =
            MavenCommandLineBuilder.build(getProject(), request, daemonInstallation(), SpyHandshake.NONE);

        assertTrue(commandLine.getParametersList().getList().contains("-Dmvnd.jvmArgs=-Xmx2g"));
    }

    public void testBuild_spyPortGivenToPlainMaven_stillInjectsTheExtensionClassPath()
        throws Exception
    {
        List<String> parameters =
            commandLineFor(mavenInstallation(), handshake(SPY_PORT)).getParametersList().getList();

        assertTrue(
            parameters.stream().anyMatch(parameter -> parameter.startsWith("-Dmaven.ext.class.path=")));
    }

    private List<String> parametersFor(int spyPort)
        throws Exception
    {
        return commandLineFor(daemonInstallation(), handshake(spyPort)).getParametersList().getList();
    }

    private List<String> parametersDeclaringAnExtension()
        throws Exception
    {
        MavenPrimeRequest request = requestIn(List.of("verify"));

        request.rawOptions = EXT_CLASS_PATH_OPTION + USER_EXTENSION;

        return MavenCommandLineBuilder
            .build(getProject(), request, daemonInstallation(), handshake(SPY_PORT))
            .getParametersList()
            .getList();
    }

    private static int lastExtensionClassPathIndex(List<String> parameters)
    {
        int last = -1;

        for (int index = 0; index < parameters.size(); index++)
        {
            if (parameters.get(index).startsWith(EXT_CLASS_PATH_OPTION))
            {
                last = index;
            }
        }

        return last;
    }

    private GeneralCommandLine commandLineFor(MavenInstallation installation, SpyHandshake handshake)
        throws Exception
    {
        return MavenCommandLineBuilder.build(
            getProject(), requestIn(List.of("verify")), installation, handshake);
    }

    private static MavenPrimeRequest requestIn(List<String> goals)
        throws IOException
    {
        return MavenPrimeRequest.in(
            FileUtil.createTempDirectory("mavenprime", "work").getPath(), goals);
    }

    private MavenInstallation mavenInstallation()
        throws IOException
    {
        return MavenInstallation.maven(
            FileUtil.createTempDirectory("mavenprime", "maven").toPath(), MavenVersion.parse("3.9.11"));
    }

    private MavenInstallation daemonInstallation()
        throws IOException
    {
        Path home = FileUtil.createTempDirectory("mavenprime", "mvnd").toPath();

        File binDirectory = home.resolve("bin").toFile();

        assertTrue(binDirectory.mkdirs());

        File executable = new File(binDirectory, "mvnd");

        assertTrue(executable.createNewFile());

        return MavenInstallation.daemon(
            home, executable.toPath(), MavenVersion.parse("3.9.11"), MavenVersion.parse("1.0.3"));
    }

    private static SpyHandshake handshake(int port)
    {
        return new SpyHandshake(port, "token", "build");
    }

    public void testMergedMavenOpts_nothingInheritedFromTheShell_isJustWhatTheUserAskedFor()
    {
        assertEquals("-Xmx2g", MavenCommandLineBuilder.mergedMavenOpts(null, "-Xmx2g"));
    }

    public void testMergedMavenOpts_aShellValueAlreadySet_keepsItAndAppendsTheRequestedOptions()
    {
        assertEquals(
            "a truststore or proxy set in MAVEN_OPTS must survive editing the VM options field",
            "-Djavax.net.ssl.trustStore=/corp/ts.jks -Xmx2g",
            MavenCommandLineBuilder.mergedMavenOpts("-Djavax.net.ssl.trustStore=/corp/ts.jks", "-Xmx2g"));
    }

    public void testMergedMavenOpts_conflictingHeapSettings_putsTheRequestedOneLastSoItWins()
    {
        assertEquals("-Xmx4G -Xmx2g", MavenCommandLineBuilder.mergedMavenOpts("-Xmx4G", "-Xmx2g"));
    }

    public void testMergedMavenOpts_aBlankShellValue_doesNotLeaveLeadingSpace()
    {
        assertEquals("-Xmx2g", MavenCommandLineBuilder.mergedMavenOpts("   ", "-Xmx2g"));
    }
}
