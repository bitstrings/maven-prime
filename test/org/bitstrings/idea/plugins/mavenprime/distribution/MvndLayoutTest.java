package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MvndLayoutTest
{
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void isDaemonHome_homeWithExecutable_isTrue()
        throws IOException
    {
        Path home = daemonHome();

        assertTrue(MvndLayout.isDaemonHome(home));
    }

    @Test
    public void isDaemonHome_homeWithoutExecutable_isFalse()
        throws IOException
    {
        assertFalse(MvndLayout.isDaemonHome(temporaryFolder.newFolder("empty").toPath()));
    }

    @Test
    public void isDaemonHome_missingDirectory_isFalse()
    {
        assertFalse(MvndLayout.isDaemonHome(temporaryFolder.getRoot().toPath().resolve("absent")));
    }

    @Test
    public void findExecutable_homeWithExecutable_returnsItsPath()
        throws IOException
    {
        Path home = daemonHome();

        assertEquals(
            home.resolve("bin").resolve(MvndLayout.executableNames().get(0)), MvndLayout.findExecutable(home));
    }

    @Test
    public void homeOfExecutable_executableInBin_returnsTheHome()
        throws IOException
    {
        Path home = daemonHome();

        assertEquals(home, MvndLayout.homeOfExecutable(MvndLayout.findExecutable(home)));
    }

    @Test
    public void embeddedMavenHome_homeWithMvnDirectory_returnsThatDirectory()
        throws IOException
    {
        Path home = daemonHome();

        Files.createDirectories(home.resolve(MvndLayout.EMBEDDED_MAVEN_DIRECTORY));

        assertEquals(home.resolve(MvndLayout.EMBEDDED_MAVEN_DIRECTORY), MvndLayout.embeddedMavenHome(home));
    }

    @Test
    public void embeddedMavenHome_homeWithoutMvnDirectory_returnsTheHomeItself()
        throws IOException
    {
        Path home = daemonHome();

        assertEquals(home, MvndLayout.embeddedMavenHome(home));
    }

    @Test
    public void daemonVersion_homeWithClientJar_readsTheVersion()
        throws IOException
    {
        Path home = daemonHome();

        Path daemonLibrary = home.resolve(MvndLayout.EMBEDDED_MAVEN_DIRECTORY).resolve("lib").resolve("mvnd");

        Files.createDirectories(daemonLibrary);
        Files.createFile(daemonLibrary.resolve("mvnd-client-1.0.3.jar"));

        assertEquals(MavenVersion.parse("1.0.3"), MvndLayout.daemonVersion(home));
    }

    @Test
    public void daemonVersion_nativeDistributionWithoutClientJar_readsTheVersion()
        throws IOException
    {
        Path home = daemonHome();

        Path daemonLibrary = home.resolve(MvndLayout.EMBEDDED_MAVEN_DIRECTORY).resolve("lib").resolve("mvnd");

        Files.createDirectories(daemonLibrary);
        Files.createFile(daemonLibrary.resolve("mvnd-agent-1.0.2.jar"));
        Files.createFile(daemonLibrary.resolve("mvnd-common-1.0.2.jar"));

        assertEquals(MavenVersion.parse("1.0.2"), MvndLayout.daemonVersion(home));
    }

    @Test
    public void daemonVersion_homeWithoutDaemonJars_isUnknown()
        throws IOException
    {
        assertFalse(MvndLayout.daemonVersion(daemonHome()).isKnown());
    }

    @Test
    public void isDaemonDistribution_homeWithoutEmbeddedMaven_isFalse()
        throws IOException
    {
        assertFalse(MvndLayout.isDaemonDistribution(daemonHome()));
    }

    @Test
    public void isDaemonDistribution_homeWithEmbeddedMaven_isTrue()
        throws IOException
    {
        Path home = daemonHome();

        Files.createDirectories(home.resolve(MvndLayout.EMBEDDED_MAVEN_DIRECTORY));

        assertTrue(MvndLayout.isDaemonDistribution(home));
    }

    private Path daemonHome()
        throws IOException
    {
        Path home = temporaryFolder.newFolder("mvnd").toPath();

        Path binDirectory = home.resolve("bin");

        Files.createDirectories(binDirectory);
        Files.createFile(binDirectory.resolve(MvndLayout.executableNames().get(0)));

        return home;
    }
}
