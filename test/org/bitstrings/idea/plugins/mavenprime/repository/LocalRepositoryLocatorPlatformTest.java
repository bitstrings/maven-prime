package org.bitstrings.idea.plugins.mavenprime.repository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperties;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperty;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class LocalRepositoryLocatorPlatformTest
    extends BasePlatformTestCase
{
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

    public void testLocate_theIdeSettingsFileDeclaresOne_followsItWithoutAReload()
        throws IOException
    {
        Path repository = repositoryDeclaredByIde("ide-repo");

        assertEquals(
            "the Repository tab must land where the IDE resolved the local repository",
            repository,
            LocalRepositoryLocator.locate(getProject()));
    }

    public void testLocate_theBuildContextSettingsFileDeclaresOne_usesItOverTheIdeResolution()
        throws IOException
    {
        repositoryDeclaredByIde("ide-repo");

        Path repository = workspace.toPath().resolve("context-repo");

        declareInBuildContext(settingsDeclaring("context", repository));

        assertEquals(
            "a build running with -s resolves against the repository that file declares",
            repository,
            LocalRepositoryLocator.locate(getProject()));
    }

    public void testLocate_theContextProperty_winsOverEveryOtherSource()
        throws IOException
    {
        repositoryDeclaredByIde("ide-repo");

        declareInBuildContext(settingsDeclaring("context", workspace.toPath().resolve("context-repo")));

        BuildContext
            .getInstance(getProject())
            .setProperties(
                List.of(
                    BuildContextProperty.of(
                        LocalRepositoryLocator.REPOSITORY_PROPERTY, "/from/property", true)));

        assertEquals(Path.of("/from/property"), LocalRepositoryLocator.locate(getProject()));
    }

    public void testLocate_aBuildContextSettingsFileWithoutALocalRepository_fallsBackToTheIdeResolution()
        throws IOException
    {
        Path repository = repositoryDeclaredByIde("ide-repo");

        declareInBuildContext(write("no-repo.xml", "<settings><offline>true</offline></settings>"));

        assertEquals(repository, LocalRepositoryLocator.locate(getProject()));
    }

    public void testLocate_aMalformedBuildContextSettingsFile_fallsBackInsteadOfFailing()
        throws IOException
    {
        Path repository = repositoryDeclaredByIde("ide-repo");

        declareInBuildContext(write("malformed.xml", "<settings> not xml at all"));

        assertEquals(repository, LocalRepositoryLocator.locate(getProject()));
    }

    public void testLocate_aBuildContextSettingsFileThatDoesNotExist_fallsBackToTheIdeResolution()
        throws IOException
    {
        Path repository = repositoryDeclaredByIde("ide-repo");

        declareInBuildContext(workspace.toPath().resolve("absent.xml"));

        assertEquals(repository, LocalRepositoryLocator.locate(getProject()));
    }

    public void testDeclaredIn_theSameSettingsFileReadAgain_isNotParsedASecondTime()
        throws IOException
    {
        Path settings = settingsDeclaring("cached", workspace.toPath().resolve("cached-repo"));

        Path once = LocalRepositoryLocator.declaredIn(workspace.toPath(), settings);

        assertNotNull(once);
        assertSame(
            "Search Everywhere resolves the repository per keystroke, so an unchanged file is read once",
            once,
            LocalRepositoryLocator.declaredIn(workspace.toPath(), settings));
    }

    public void testDeclaredIn_aSettingsFileEditedSinceItWasRead_reportsWhatItNowDeclares()
        throws IOException
    {
        Path settings = settingsDeclaring("edited", workspace.toPath().resolve("first-repo"));

        LocalRepositoryLocator.declaredIn(workspace.toPath(), settings);

        Path edited = workspace.toPath().resolve("second-repository");

        settingsDeclaring("edited", edited);

        assertEquals(
            "an edit outside the IDE has to reach the next lookup",
            edited,
            LocalRepositoryLocator.declaredIn(workspace.toPath(), settings));
    }

    private Path repositoryDeclaredByIde(String name)
        throws IOException
    {
        Path repository = workspace.toPath().resolve(name);

        ideSettings().setUserSettingsFile(settingsDeclaring("ide", repository).toString());

        return repository;
    }

    private void declareInBuildContext(Path settingsFile)
    {
        BuildContextEnvironment environment = new BuildContextEnvironment();

        environment.settingsFile = settingsFile.toString();

        BuildContext.getInstance(getProject()).setEnvironment(environment);
    }

    private Path settingsDeclaring(String name, Path repository)
        throws IOException
    {
        return write(
            "settings-" + name + ".xml",
            "<settings><localRepository>" + repository + "</localRepository></settings>");
    }

    private Path write(String name, String content)
        throws IOException
    {
        Path file = workspace.toPath().resolve(name);

        Files.writeString(file, content, StandardCharsets.UTF_8);

        return file;
    }

    private MavenGeneralSettings ideSettings()
    {
        return MavenProjectsManager.getInstance(getProject()).getGeneralSettings();
    }
}
