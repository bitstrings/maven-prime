package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCacheStatus.Verdict;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalRepositoryDoctorTest
{
    private static final String COORDINATES = "org.example:widget:jar:1.2.3";

    private static final String FILE_NAME = "widget-1.2.3.jar";

    private static final String CENTRAL = "https://repo.maven.apache.org/maven2/";

    @Rule
    public final TemporaryFolder repository = new TemporaryFolder();

    @Test
    public void inspect_artifactPresentOnDisk_reportsItResolved()
        throws IOException
    {
        Path directory = versionDirectory();

        Files.writeString(directory.resolve(FILE_NAME), "jar");

        assertEquals(Verdict.RESOLVED, inspect().verdict());
    }

    @Test
    public void inspect_artifactNeverFetched_reportsItNeverAttempted()
        throws IOException
    {
        versionDirectory();

        assertEquals(Verdict.NEVER_ATTEMPTED, inspect().verdict());
    }

    @Test
    public void inspect_emptyErrorEntry_reportsTheArtifactAsNotFound()
        throws IOException
    {
        writeMarker(notFoundAt(CENTRAL, 1775625453596L));

        assertEquals(Verdict.NOT_FOUND, inspect().verdict());
    }

    @Test
    public void inspect_populatedErrorEntry_reportsATransferFailure()
        throws IOException
    {
        Map<String, String> marker = notFoundAt(CENTRAL, 1775625453596L);

        marker.put(CENTRAL + ".error", "Connection refused");

        writeMarker(marker);

        assertEquals(Verdict.TRANSFER_FAILED, inspect().verdict());
    }

    @Test
    public void inspect_recordedFailure_reportsWhenMavenLastTried()
        throws IOException
    {
        writeMarker(notFoundAt(CENTRAL, 1775625453596L));

        assertEquals(1775625453596L, inspect().lastAttemptMillis().orElseThrow());
    }

    @Test
    public void inspect_recordedFailure_isReportedAsNegativelyCached()
        throws IOException
    {
        writeMarker(notFoundAt(CENTRAL, 1775625453596L));

        assertTrue(inspect().isNegativelyCached());
    }

    @Test
    public void inspect_recordedFailure_namesTheRepositoryUrlThatWasTried()
        throws IOException
    {
        writeMarker(notFoundAt(CENTRAL, 1775625453596L));

        assertEquals(CENTRAL, inspect().attempts().get(0).repository());
    }

    @Test
    public void inspect_artifactPresent_isNotReportedAsNegativelyCached()
        throws IOException
    {
        Path directory = versionDirectory();

        Files.writeString(directory.resolve(FILE_NAME), "jar");

        assertFalse(inspect().isNegativelyCached());
    }

    @Test
    public void inspect_fileRecordedAgainstSeveralRepositories_listsThemInAStableOrder()
        throws IOException
    {
        Path directory = versionDirectory();

        Files.writeString(directory.resolve(FILE_NAME), "jar");
        Files.writeString(
            directory.resolve(LocalRepositoryDoctor.REMOTE_REPOSITORIES),
            FILE_NAME + ">central=\n" + FILE_NAME + ">main=\n",
            StandardCharsets.UTF_8);

        assertEquals("central, main", inspect().originRepository());
    }

    @Test
    public void inspect_locallyInstalledArtifact_recordsNoOriginRepository()
        throws IOException
    {
        Path directory = versionDirectory();

        Files.writeString(directory.resolve(FILE_NAME), "jar");
        Files.writeString(
            directory.resolve(LocalRepositoryDoctor.REMOTE_REPOSITORIES),
            FILE_NAME + ">=\n",
            StandardCharsets.UTF_8);

        assertEquals(null, inspect().originRepository());
    }

    @Test
    public void inspect_versionDirectoryAbsent_reportsItNeverAttempted()
    {
        assertEquals(Verdict.NEVER_ATTEMPTED, inspect().verdict());
    }

    @Test
    public void inspect_twoRepositoriesRecorded_returnsAnAttemptPerRepository()
        throws IOException
    {
        Map<String, String> marker = notFoundAt(CENTRAL, 100L);

        marker.putAll(notFoundAt("https://mirror.example.com/", 200L));

        writeMarker(marker);

        assertEquals(2, inspect().attempts().size());
    }

    private ArtifactCacheStatus inspect()
    {
        return LocalRepositoryDoctor.inspect(
            repository.getRoot().toPath(), ArtifactCoordinates.parse(COORDINATES).orElseThrow());
    }

    private Path versionDirectory()
        throws IOException
    {
        return Files.createDirectories(
            repository.getRoot().toPath().resolve("org/example/widget/1.2.3"));
    }

    private static Map<String, String> notFoundAt(String repositoryUrl, long millis)
    {
        Map<String, String> marker = new LinkedHashMap<>();

        marker.put(repositoryUrl + ".error", StringUtils.EMPTY);
        marker.put(repositoryUrl + ".lastUpdated", Long.toString(millis));

        return marker;
    }

    private void writeMarker(Map<String, String> entries)
        throws IOException
    {
        Properties properties = new Properties();

        entries.forEach(properties::setProperty);

        try (OutputStream out =
            Files.newOutputStream(
                versionDirectory().resolve(FILE_NAME + LocalRepositoryDoctor.LAST_UPDATED_SUFFIX)))
        {
            properties.store(out, "Maven Resolver internal implementation file");
        }
    }
}
