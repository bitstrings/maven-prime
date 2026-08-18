package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalRepositoryScanTest
{
    @Rule
    public final TemporaryFolder repository = new TemporaryFolder();

    @Rule
    public final TemporaryFolder linkFolder = new TemporaryFolder();

    @Test
    public void scan_versionDirectoryHoldingItsJar_reportsTheArtifact()
        throws IOException
    {
        write("org/example/widget/1.2.3/widget-1.2.3.jar");

        List<ArtifactCoordinates> found = LocalRepositoryScan.scan(root());

        assertEquals(1, found.size());
        assertEquals("org.example:widget:1.2.3", found.get(0).shortForm());
    }

    @Test
    public void scan_nestedGroupDirectories_joinsThemWithDots()
        throws IOException
    {
        write("org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar");

        assertEquals("org.apache.commons", LocalRepositoryScan.scan(root()).get(0).groupId());
    }

    @Test
    public void scan_onlyANegativeCacheMarkerPresent_reportsNothingBecauseTheDownloadFailed()
        throws IOException
    {
        write("org/example/widget/9.9.9/widget-9.9.9.jar.lastUpdated");

        assertTrue(LocalRepositoryScan.scan(root()).isEmpty());
    }

    @Test
    public void scan_partialDownloadPresent_reportsNothingBecauseTheArtifactIsIncomplete()
        throws IOException
    {
        write("org/example/widget/9.9.9/widget-9.9.9.jar.part");

        assertTrue(LocalRepositoryScan.scan(root()).isEmpty());
    }

    @Test
    public void scan_bothAJarAndAPomPresent_prefersTheJarExtension()
        throws IOException
    {
        write("org/example/widget/1.2.3/widget-1.2.3.pom");
        write("org/example/widget/1.2.3/widget-1.2.3.jar");

        assertEquals("jar", LocalRepositoryScan.scan(root()).get(0).extension());
    }

    @Test
    public void scan_onlyAPomPresent_reportsThePomExtension()
        throws IOException
    {
        write("org/example/parent/1.0.0/parent-1.0.0.pom");

        assertEquals("pom", LocalRepositoryScan.scan(root()).get(0).extension());
    }

    @Test
    public void scan_directoryHoldingOnlyUnrelatedFiles_reportsNothing()
        throws IOException
    {
        write("org/example/widget/1.2.3/_remote.repositories");

        assertTrue(LocalRepositoryScan.scan(root()).isEmpty());
    }

    @Test
    public void scan_missingRepositoryDirectory_reportsNothing()
    {
        assertTrue(LocalRepositoryScan.scan(root().resolve("absent")).isEmpty());
    }

    @Test
    public void scan_severalVersionsOfOneArtifact_ordersTheNewestFirst()
        throws IOException
    {
        write("org/example/widget/1.9.0/widget-1.9.0.jar");
        write("org/example/widget/1.10.0/widget-1.10.0.jar");

        List<ArtifactCoordinates> found = LocalRepositoryScan.scan(root());

        assertEquals("1.10.0", found.get(0).version());
        assertEquals("1.9.0", found.get(1).version());
    }

    @Test
    public void scan_remotelyDownloadedTimestampedSnapshot_reportsTheSnapshotCoordinates()
        throws IOException
    {
        write("org/example/widget/1.0-SNAPSHOT/widget-1.0-20240101.101010-3.jar");

        List<ArtifactCoordinates> found = LocalRepositoryScan.scan(root());

        assertEquals(1, found.size());
        assertEquals("org.example:widget:1.0-SNAPSHOT", found.get(0).shortForm());
    }

    @Test
    public void scan_timestampedSnapshotJarAndPom_prefersTheJarExtension()
        throws IOException
    {
        write("org/example/widget/1.0-SNAPSHOT/widget-1.0-20240101.101010-3.pom");
        write("org/example/widget/1.0-SNAPSHOT/widget-1.0-20240101.101010-3.jar");

        assertEquals("jar", LocalRepositoryScan.scan(root()).get(0).extension());
    }

    @Test
    public void scan_snapshotDirectoryHoldingItsMetadata_ignoresTheMetadataFile()
        throws IOException
    {
        write("org/example/widget/1.0-SNAPSHOT/maven-metadata-central.xml");

        assertTrue(LocalRepositoryScan.scan(root()).isEmpty());
    }

    @Test
    public void scan_aRepositoryReachedThroughASymlink_stillReportsItsArtifacts()
        throws IOException
    {
        write("org/example/widget/1.2.3/widget-1.2.3.jar");

        Path link = Files.createSymbolicLink(linkFolder.getRoot().toPath().resolve("repository"), root());

        assertEquals(1, LocalRepositoryScan.scan(link).size());
    }

    private Path root()
    {
        return repository.getRoot().toPath();
    }

    private void write(String relativePath)
        throws IOException
    {
        Path file = root().resolve(relativePath);

        Files.createDirectories(file.getParent());
        Files.writeString(file, "content");
    }
}
