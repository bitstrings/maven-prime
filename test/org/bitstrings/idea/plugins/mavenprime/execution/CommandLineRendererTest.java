package org.bitstrings.idea.plugins.mavenprime.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenVersion;
import org.junit.Test;

public class CommandLineRendererTest
{
    private static final MavenInstallation MAVEN_3 =
        MavenInstallation.maven(Paths.get("/opt/maven"), MavenVersion.parse("3.9.12"));

    private static final MavenInstallation MAVEN_4 =
        MavenInstallation.maven(Paths.get("/opt/maven4"), MavenVersion.parse("4.0.0"));

    private static final MavenInstallation MVND =
        MavenInstallation.daemon(
            Paths.get("/opt/mvnd"),
            Paths.get("/opt/mvnd/bin/mvnd"),
            MavenVersion.parse("3.9.11"),
            MavenVersion.parse("1.0.3"));

    @Test
    public void renderProperties_valuedProperty_rendersDefinitionWithValue()
    {
        Map<String, String> properties = new LinkedHashMap<>();

        properties.put("skipTests", "true");

        assertEquals(List.of("-DskipTests=true"), CommandLineRenderer.renderProperties(properties));
    }

    @Test
    public void renderProperties_valuelessProperty_rendersBareDefinition()
    {
        Map<String, String> properties = new LinkedHashMap<>();

        properties.put("quickstart", "");

        assertEquals(List.of("-Dquickstart"), CommandLineRenderer.renderProperties(properties));
    }

    @Test
    public void renderProperties_blankKey_isSkipped()
    {
        Map<String, String> properties = new LinkedHashMap<>();

        properties.put("  ", "value");

        assertTrue(CommandLineRenderer.renderProperties(properties).isEmpty());
    }

    @Test
    public void renderProfiles_enabledAndDisabledProfiles_prefixesTheDisabledOneWithBang()
    {
        Map<String, Boolean> profiles = new LinkedHashMap<>();

        profiles.put("release", Boolean.TRUE);
        profiles.put("dev", Boolean.FALSE);

        assertEquals(List.of("-P", "release,!dev"), CommandLineRenderer.renderProfiles(profiles));
    }

    @Test
    public void renderProfiles_noProfiles_rendersNothing()
    {
        assertTrue(CommandLineRenderer.renderProfiles(Map.of()).isEmpty());
    }

    @Test
    public void renderThreads_threadCount_rendersOptionAndValue()
    {
        assertEquals(List.of("-T", "1C"), CommandLineRenderer.renderThreads(" 1C "));
    }

    @Test
    public void renderProjects_severalProjects_joinsThemWithCommas()
    {
        assertEquals(List.of("-pl", "core,web"), CommandLineRenderer.renderProjects(List.of("core", "web")));
    }

    @Test
    public void renderSettingsFile_configuredFile_rendersTheOption()
    {
        assertEquals(
            List.of("-s", "/home/dev/.m2/settings.xml"),
            CommandLineRenderer.renderSettingsFile("/home/dev/.m2/settings.xml"));
    }

    @Test
    public void renderSettingsFile_blankFile_rendersNothing()
    {
        assertTrue(CommandLineRenderer.renderSettingsFile("  ").isEmpty());
    }


    @Test
    public void renderRawOptions_quotedArgument_keepsItAsOneArgument()
    {
        assertEquals(
            List.of("-Dmessage=hello world", "-X"),
            CommandLineRenderer.renderRawOptions("\"-Dmessage=hello world\" -X"));
    }

    @Test
    public void unsupportedFlags_resumeOnMaven3_dropsTheFlag()
    {
        assertTrue(CommandLineRenderer.unsupportedFlags(List.of(MavenFlag.RESUME), MAVEN_3).contains(MavenFlag.RESUME));
    }

    @Test
    public void unsupportedFlags_resumeOnMaven4_keepsTheFlag()
    {
        assertTrue(CommandLineRenderer.unsupportedFlags(List.of(MavenFlag.RESUME), MAVEN_4).isEmpty());
    }

    @Test
    public void unsupportedFlags_resumeOnDaemon_keepsTheFlag()
    {
        assertTrue(CommandLineRenderer.unsupportedFlags(List.of(MavenFlag.RESUME), MVND).isEmpty());
    }

    @Test
    public void render_requestWithFlagsAndGoals_putsOptionsBeforeGoals()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("clean", "install"));

        request.flags.add(MavenFlag.OFFLINE);

        RenderedCommandLine rendered = CommandLineRenderer.render(request, MAVEN_3);

        assertEquals(List.of("-o", "clean", "install"), rendered.getArguments());
    }

    @Test
    public void render_unsupportedFlag_dropsItAndReportsIt()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("verify"));

        request.flags.add(MavenFlag.RESUME);

        RenderedCommandLine rendered = CommandLineRenderer.render(request, MAVEN_3);

        assertEquals(List.of("verify"), rendered.getArguments());
        assertTrue(rendered.getDroppedFlags().contains(MavenFlag.RESUME));
    }

    @Test
    public void render_defaultPomFileName_addsNoFileOption()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("test"));

        assertFalse(CommandLineRenderer.render(request, MAVEN_3).getOptions().contains("-f"));
    }

    @Test
    public void render_alternatePomFileName_addsFileOption()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("test"));

        request.pomFileName = "pom-release.xml";

        assertEquals(
            List.of("-f", "pom-release.xml"), CommandLineRenderer.render(request, MAVEN_3).getOptions());
    }

    @Test
    public void render_failureBehaviorAndOutputLevel_rendersBothOptions()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("install"));

        request.failureBehavior = FailureBehavior.FAIL_AT_END;
        request.outputLevel = OutputLevel.DEBUG;

        List<String> options = CommandLineRenderer.render(request, MAVEN_3).getOptions();

        assertTrue(options.contains("-fae"));
        assertTrue(options.contains("-X"));
    }
}
