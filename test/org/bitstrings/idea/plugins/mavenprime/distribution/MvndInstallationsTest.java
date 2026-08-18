package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertEquals;
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

    private Path install(String version)
        throws IOException
    {
        Path home = MvndInstallations.rootIn(userHome).resolve(version);

        Files.createDirectories(home.resolve("bin"));
        Files.createFile(home.resolve("bin").resolve(MvndLayout.executableNames().get(0)));

        return home;
    }
}
