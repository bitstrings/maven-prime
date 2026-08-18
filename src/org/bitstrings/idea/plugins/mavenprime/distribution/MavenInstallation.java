package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.nio.file.Path;
import java.util.Optional;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.openapi.util.SystemInfo;

public final class MavenInstallation
{
    private final DistributionKind kind;

    private final Path home;

    private final Path executable;

    private final MavenVersion mavenVersion;

    private final MavenVersion daemonVersion;

    private final String error;

    private MavenInstallation(
        DistributionKind kind,
        Path home,
        Path executable,
        MavenVersion mavenVersion,
        MavenVersion daemonVersion,
        String error)
    {
        this.kind = kind;
        this.home = home;
        this.executable = executable;
        this.mavenVersion = mavenVersion;
        this.daemonVersion = daemonVersion;
        this.error = error;
    }

    public static MavenInstallation ide(Path home, MavenVersion mavenVersion)
    {
        return new MavenInstallation(DistributionKind.IDE, home, null, mavenVersion, MavenVersion.UNKNOWN, null);
    }

    public static MavenInstallation maven(Path home, MavenVersion mavenVersion)
    {
        return new MavenInstallation(
            DistributionKind.MAVEN_HOME, home, null, mavenVersion, MavenVersion.UNKNOWN, null);
    }

    public static MavenInstallation daemon(
        Path home, Path executable, MavenVersion mavenVersion, MavenVersion daemonVersion)
    {
        return new MavenInstallation(
            DistributionKind.MVND_HOME, home, executable, mavenVersion, daemonVersion, null);
    }

    public static MavenInstallation invalid(DistributionKind kind, String error)
    {
        return new MavenInstallation(kind, null, null, MavenVersion.UNKNOWN, MavenVersion.UNKNOWN, error);
    }

    public DistributionKind getKind()
    {
        return kind;
    }

    public Optional<Path> getHome()
    {
        return Optional.ofNullable(home);
    }

    public Optional<Path> getExecutable()
    {
        return Optional.ofNullable(executable);
    }

    public Optional<Path> getMavenHome()
    {
        return getHome().map(path -> isDaemon() ? MvndLayout.embeddedMavenHome(path) : path);
    }

    public Optional<Path> getLauncher()
    {
        return isDaemon() ? getExecutable() : getMavenHome().map(MavenInstallation::mavenLauncher);
    }

    private static Path mavenLauncher(Path mavenHome)
    {
        return mavenHome.resolve("bin").resolve(SystemInfo.isWindows ? "mvn.cmd" : "mvn");
    }

    public MavenVersion getMavenVersion()
    {
        return mavenVersion;
    }

    public boolean isDaemon()
    {
        return kind.isDaemon();
    }

    public boolean isValid()
    {
        return error == null;
    }

    public String getError()
    {
        return error;
    }

    public String getDisplayName()
    {
        if (!isValid())
        {
            return MavenPrimeBundle.message("mavenprime.distribution.display.invalid", kind.getTitle());
        }

        if (isDaemon())
        {
            return MavenPrimeBundle.message(
                "mavenprime.distribution.display.daemon", display(daemonVersion), display(mavenVersion));
        }

        return MavenPrimeBundle.message(
            (kind == DistributionKind.IDE)
                ? "mavenprime.distribution.display.ide"
                : "mavenprime.distribution.display.maven",
            display(mavenVersion));
    }

    private static String display(MavenVersion version)
    {
        return version.isKnown() ? version.toString() : MavenPrimeBundle.message("mavenprime.version.unknown");
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
