package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MvndLayoutDetectionTest
{
    @Rule
    public final TemporaryFolder distributions = new TemporaryFolder();

    @Test
    public void isDaemonHome_mvndWrapperDistribution_isRecognized()
        throws IOException
    {
        Path home = mvndDistribution();

        assertTrue(MvndLayout.isDaemonHome(home));
    }

    @Test
    public void isDaemonDistribution_mvndWrapperDistribution_isRecognized()
        throws IOException
    {
        Path home = mvndDistribution();

        assertTrue(MvndLayout.isDaemonDistribution(home));
    }

    @Test
    public void isDaemonHome_plainMavenDistribution_isNotRecognized()
        throws IOException
    {
        Path home = plainMavenDistribution();

        assertFalse(MvndLayout.isDaemonHome(home));
    }

    @Test
    public void isDaemonDistribution_launcherWithoutEmbeddedMaven_isNotACompleteDistribution()
        throws IOException
    {
        Path home = distributions.newFolder("launcher-only").toPath();

        createExecutable(home);

        assertTrue(MvndLayout.isDaemonHome(home));
        assertFalse(MvndLayout.isDaemonDistribution(home));
    }

    @Test
    public void embeddedMavenHome_mvndDistribution_pointsAtTheEmbeddedMaven()
        throws IOException
    {
        Path home = mvndDistribution();

        assertTrue(MvndLayout.embeddedMavenHome(home).endsWith(MvndLayout.EMBEDDED_MAVEN_DIRECTORY));
    }

    private Path mvndDistribution()
        throws IOException
    {
        Path home = distributions.newFolder("maven-mvnd-1.0.3-linux-amd64").toPath();

        createExecutable(home);

        assertTrue(home.resolve("mvn").resolve("lib").toFile().mkdirs());

        return home;
    }

    private Path plainMavenDistribution()
        throws IOException
    {
        Path home = distributions.newFolder("apache-maven-3.9.12").toPath();

        assertTrue(home.resolve("lib").toFile().mkdirs());
        assertTrue(home.resolve("bin").toFile().mkdirs());
        assertTrue(home.resolve("bin").resolve("mvn").toFile().createNewFile());

        return home;
    }

    private static void createExecutable(Path home)
        throws IOException
    {
        File binDirectory = home.resolve("bin").toFile();

        assertTrue(binDirectory.mkdirs());

        for (String name : MvndLayout.executableNames())
        {
            assertTrue(new File(binDirectory, name).createNewFile());
        }
    }
}
