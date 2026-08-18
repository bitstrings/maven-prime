package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RepositoryFilesTest
{
    private static final ArtifactCoordinates WIDGET =
        new ArtifactCoordinates("org.example", "widget", "jar", StringUtils.EMPTY, "1.2.3");

    private static final String DIRECTORY = "org/example/widget/1.2.3/";

    @Rule
    public final TemporaryFolder repository = new TemporaryFolder();

    @Test
    public void of_versionDirectoryHoldingTheJar_findsTheArtifactFile()
        throws IOException
    {
        write(DIRECTORY + "widget-1.2.3.jar", "jar");

        assertNotNull(RepositoryFiles.of(root(), WIDGET).artifact());
    }

    @Test
    public void of_versionDirectoryWithoutASourcesJar_reportsSourcesAsMissing()
        throws IOException
    {
        write(DIRECTORY + "widget-1.2.3.jar", "jar");

        assertNull(RepositoryFiles.of(root(), WIDGET).sources());
    }

    @Test
    public void of_sourcesJarBesideTheArtifact_findsIt()
        throws IOException
    {
        write(DIRECTORY + "widget-1.2.3-sources.jar", "sources");

        assertNotNull(RepositoryFiles.of(root(), WIDGET).sources());
    }

    @Test
    public void of_javadocJarBesideTheArtifact_findsIt()
        throws IOException
    {
        write(DIRECTORY + "widget-1.2.3-javadoc.jar", "javadoc");

        assertNotNull(RepositoryFiles.of(root(), WIDGET).javadoc());
    }

    @Test
    public void of_pomBesideTheArtifact_findsIt()
        throws IOException
    {
        write(DIRECTORY + "widget-1.2.3.pom", "pom");

        assertNotNull(RepositoryFiles.of(root(), WIDGET).pom());
    }

    @Test
    public void of_severalFilesInTheVersionDirectory_sumsTheirSizes()
        throws IOException
    {
        write(DIRECTORY + "widget-1.2.3.jar", "1234");
        write(DIRECTORY + "widget-1.2.3.pom", "567");

        assertEquals(7L, RepositoryFiles.of(root(), WIDGET).sizeBytes());
    }

    @Test
    public void of_versionDirectoryThatWasNeverCreated_reportsNoSize()
    {
        assertEquals(0L, RepositoryFiles.of(root(), WIDGET).sizeBytes());
    }

    @Test
    public void presentableFile_jarOnDisk_prefersTheJar()
        throws IOException
    {
        write(DIRECTORY + "widget-1.2.3.jar", "jar");
        write(DIRECTORY + "widget-1.2.3.pom", "pom");

        RepositoryFiles files = RepositoryFiles.of(root(), WIDGET);

        assertEquals(files.artifact(), files.presentableFile());
    }

    @Test
    public void presentableFile_onlyThePomOnDisk_fallsBackToThePom()
        throws IOException
    {
        write(DIRECTORY + "widget-1.2.3.pom", "pom");

        RepositoryFiles files = RepositoryFiles.of(root(), WIDGET);

        assertEquals(files.pom(), files.presentableFile());
    }

    @Test
    public void presentableFile_nothingOnDisk_fallsBackToTheDirectory()
    {
        RepositoryFiles files = RepositoryFiles.of(root(), WIDGET);

        assertEquals(files.directory(), files.presentableFile());
    }

    private Path root()
    {
        return repository.getRoot().toPath();
    }

    private void write(String relativePath, String content)
        throws IOException
    {
        Path file = root().resolve(relativePath);

        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
