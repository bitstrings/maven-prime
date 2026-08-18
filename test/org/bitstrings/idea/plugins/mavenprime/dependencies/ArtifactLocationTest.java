package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.jetbrains.idea.maven.model.MavenArtifact;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArtifactLocationTest
{
    @Rule
    public final TemporaryFolder repository = new TemporaryFolder();

    @Test
    public void pathOf_artifactWithoutExplicitFile_derivesTheRepositoryLayoutPath()
    {
        assertEquals(
            Paths.get("com", "lib", "widget", "1.0", "widget-1.0.jar"), ArtifactLocation.pathOf(artifact(null)));
    }

    @Test
    public void fileOf_existingArtifactFile_returnsThatFile()
        throws IOException
    {
        File jar = repository.newFile("widget-1.0.jar");

        assertEquals(jar.toPath(), ArtifactLocation.fileOf(artifact(jar)));
    }

    @Test
    public void fileOf_unresolvedArtifact_returnsNull()
    {
        assertNull(ArtifactLocation.fileOf(artifact(new File(repository.getRoot(), "widget-1.0.jar"))));
    }

    @Test
    public void directoryOf_existingArtifactFile_returnsTheContainingDirectory()
        throws IOException
    {
        File jar = repository.newFile("widget-1.0.jar");

        assertEquals(repository.getRoot().toPath(), ArtifactLocation.directoryOf(artifact(jar)));
    }

    @Test
    public void directoryOf_unresolvedArtifactInAnExistingDirectory_returnsThatDirectory()
    {
        File missing = new File(repository.getRoot(), "widget-1.0.jar");

        assertEquals(repository.getRoot().toPath(), ArtifactLocation.directoryOf(artifact(missing)));
    }

    @Test
    public void directoryOf_artifactInAMissingDirectory_returnsNull()
    {
        File missing = new File(new File(repository.getRoot(), "absent"), "widget-1.0.jar");

        assertNull(ArtifactLocation.directoryOf(artifact(missing)));
    }

    private static MavenArtifact artifact(File file)
    {
        return new MavenArtifact(
            "com.lib", "widget", "1.0", "1.0", "jar", null, "compile", false, "jar", file, null, true, false);
    }
}
