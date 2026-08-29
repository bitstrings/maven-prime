package org.bitstrings.idea.plugins.mavenprime.execution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenVersion;
import org.junit.Test;

public class MavenFlagTest
{
    private static final MavenInstallation MAVEN =
        MavenInstallation.maven(Paths.get("/opt/maven"), MavenVersion.parse("3.9.16"));

    private static final MavenInstallation MVND =
        MavenInstallation.daemon(
            Paths.get("/opt/mvnd"),
            Paths.get("/opt/mvnd/bin/mvnd"),
            MavenVersion.parse("3.9.11"),
            MavenVersion.parse("1.0.6"));

    @Test
    public void isSupportedBy_advertisedOption_isTrue()
    {
        assertTrue(MavenFlag.UPDATE_ARTIFACTS.isSupportedBy(MAVEN, Set.of("-UA", "-o")));
    }

    @Test
    public void isSupportedBy_optionMissingFromAdvertisedOptions_isFalse()
    {
        assertFalse(MavenFlag.UPDATE_ARTIFACTS.isSupportedBy(MAVEN, Set.of("-o", "-U")));
    }

    @Test
    public void isSupportedBy_advertisedOptionsUnavailable_isTrue()
    {
        assertTrue(MavenFlag.UPDATE_ARTIFACTS.isSupportedBy(MAVEN, Set.of()));
    }

    @Test
    public void isSupportedBy_optionThatOnlyDiffersBySuffix_isFalse()
    {
        assertFalse(MavenFlag.UPDATE_ARTIFACTS.isSupportedBy(MAVEN, Set.of("-U")));
    }

    @Test
    public void isSupportedBy_userPropertyFlag_isTrueWithoutBeingAdvertised()
    {
        assertTrue(MavenFlag.SKIP_TESTS.isSupportedBy(MAVEN, Set.of("-o")));
    }

    @Test
    public void isSupportedBy_daemonOnlyFlagOnPlainMaven_isFalse()
    {
        assertFalse(MavenFlag.NO_DAEMON.isSupportedBy(MAVEN, Set.of("-o")));
    }

    @Test
    public void isSupportedBy_daemonOnlyFlagOnDaemon_isTrue()
    {
        assertTrue(MavenFlag.NO_DAEMON.isSupportedBy(MVND, Set.of("-o")));
    }

    @Test
    public void isSupportedBy_resumeAdvertisedByDaemon_isTrue()
    {
        assertTrue(MavenFlag.RESUME.isSupportedBy(MVND, Set.of("-r", "-o")));
    }

    @Test
    public void isSupportedBy_resumeNotAdvertisedByMaven_isFalse()
    {
        assertFalse(MavenFlag.RESUME.isSupportedBy(MAVEN, Set.of("-rf", "-o")));
    }
}
