package org.bitstrings.idea.plugins.mavenprime.repository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperties;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenConfigurationPlatformTest
    extends BasePlatformTestCase
{
    private static final String IDE_SETTINGS = "/ide/settings.xml";

    private static final String MAVEN_PRIME_SETTINGS = "/mavenprime/settings.xml";

    private File workspace;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        MavenProjectsManager.getInstance(getProject()).initForTests();

        workspace = FileUtil.createTempDirectory("mavenprime", getName());
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            RunAll.runAll(
                () -> BuildContext.getInstance(getProject()).loadState(new BuildContextProperties()),
                () -> ideSettings().setUserSettingsFile(StringUtils.EMPTY),
                () -> FileUtil.delete(workspace));
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testSettingsFileOf_theIdeConfiguredOneAndMavenPrimeDidNot_usesTheIdeOne()
    {
        ideSettings().setUserSettingsFile(IDE_SETTINGS);

        assertEquals(
            "a settings file configured in the IDE has to reach the repository tools too",
            IDE_SETTINGS,
            MavenConfiguration.settingsFileOf(getProject()));
    }

    public void testSettingsFileOf_mavenPrimeDeclaredOne_winsOverTheIdeSetting()
    {
        ideSettings().setUserSettingsFile(IDE_SETTINGS);

        BuildContextEnvironment environment = new BuildContextEnvironment();

        environment.settingsFile = MAVEN_PRIME_SETTINGS;

        BuildContext.getInstance(getProject()).setEnvironment(environment);

        assertEquals(MAVEN_PRIME_SETTINGS, MavenConfiguration.settingsFileOf(getProject()));
    }

    public void testSettingsFileOf_neitherConfiguredOne_reportsNothing()
    {
        assertNull(MavenConfiguration.settingsFileOf(getProject()));
    }

    public void testLocate_theIdeSettingsFileDeclaringALocalRepository_pointsTheRepositoryToolsAtIt()
        throws IOException
    {
        Path repository = workspace.toPath().resolve("team-repo");

        ideSettings().setUserSettingsFile(settingsDeclaring(repository).toString());

        assertEquals(
            "the Repository tab and the build it launches must agree on one repository",
            repository,
            LocalRepositoryLocator.locate(getProject()));
    }

    public void testSettingsFiles_theIdeConfiguredAUserSettingsFile_readsCredentialsFromIt()
        throws IOException
    {
        Path settings = settingsDeclaring(workspace.toPath().resolve("team-repo"));

        ideSettings().setUserSettingsFile(settings.toString());

        assertTrue(
            "a server block in the configured settings file has to be reachable",
            MavenSettingsFiles.of(getProject()).contains(settings));
    }

    private Path settingsDeclaring(Path repository)
        throws IOException
    {
        Path settings = workspace.toPath().resolve("settings.xml");

        Files.writeString(
            settings,
            "<settings><localRepository>" + repository + "</localRepository></settings>",
            StandardCharsets.UTF_8);

        return settings;
    }

    private MavenGeneralSettings ideSettings()
    {
        return MavenProjectsManager.getInstance(getProject()).getGeneralSettings();
    }
}
