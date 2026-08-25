package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MvndInstallationsTest
{
    private Path userHome;

    @Before
    public void createUserHome()
        throws IOException
    {
        userHome = Files.createTempDirectory("mavenprime-home");
    }

    @After
    public void deleteUserHome()
        throws IOException
    {
        try (var paths = Files.walk(userHome))
        {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
            {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    public void managedIn_aHomeWithoutTheManagedDirectory_findsNothing()
    {
        assertEquals(List.of(), MvndInstallations.managedIn(userHome));
    }

    @Test
    public void managedIn_aDirectoryWithoutTheMvndExecutable_isNotOffered()
        throws IOException
    {
        Files.createDirectories(MvndInstallations.rootIn(userHome).resolve("1.0.3"));

        assertEquals(List.of(), MvndInstallations.managedIn(userHome));
    }

    @Test
    public void managedIn_twoInstalledVersions_putsTheNewestFirst()
        throws IOException
    {
        Path older = install("1.0.2");
        Path newer = install("1.0.3");

        assertEquals(List.of(newer, older), MvndInstallations.managedIn(userHome));
    }

    @Test
    public void managedIn_versionsThatSortWrongLexicographically_stillOrdersByVersion()
        throws IOException
    {
        Path older = install("0.9.4");
        Path newer = install("1.0.3");

        assertEquals(List.of(newer, older), MvndInstallations.managedIn(userHome));
    }

    @Test
    public void rootIn_anyUserHome_staysUnderTheMavenUserDirectory()
    {
        assertTrue(
            MvndInstallations.rootIn(userHome).startsWith(userHome.resolve(".m2")));
    }

    @Test
    public void isManaged_aDaemonMavenPrimeDownloaded_isOurs()
        throws IOException
    {
        assertTrue(MvndInstallations.isManaged(install("1.0.3"), userHome));
    }

    @Test
    public void isManaged_aDaemonFoundElsewhereOnTheMachine_isNotOurs()
        throws IOException
    {
        assertFalse(MvndInstallations.isManaged(elsewhere(), userHome));
    }

    @Test
    public void remove_aDaemonMavenPrimeDownloaded_takesItsFilesOffDisk()
        throws IOException
    {
        Path home = install("1.0.3");

        MvndInstallations.remove(home, userHome);

        assertFalse(
            "a daemon left on disk keeps being detected, so removing it appears to do nothing",
            Files.exists(home));
        assertEquals(List.of(), MvndInstallations.managedIn(userHome));
    }

    @Test
    public void remove_aDaemonMavenPrimeDidNotDownload_leavesItAlone()
        throws IOException
    {
        Path home = elsewhere();

        assertThrows(IllegalArgumentException.class, () -> MvndInstallations.remove(home, userHome));
        assertTrue("deleting a directory Maven Prime did not create is never ours to do", Files.exists(home));
    }

    private Path elsewhere()
        throws IOException
    {
        return Files.createDirectories(userHome.resolve("opt").resolve("mvnd"));
    }

    private Path install(String version)
        throws IOException
    {
        Path home = MvndInstallations.rootIn(userHome).resolve(version);

        Files.createDirectories(home.resolve("bin"));
        Files.createFile(home.resolve("bin").resolve(MvndLayout.executableNames().get(0)));

        return home;
    }
}
