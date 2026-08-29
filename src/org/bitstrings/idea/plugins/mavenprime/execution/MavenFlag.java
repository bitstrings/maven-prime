package org.bitstrings.idea.plugins.mavenprime.execution;

import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;

public enum MavenFlag
{
    OFFLINE("offline", "-o", Support.ADVERTISED),
    UPDATE_SNAPSHOTS("updateSnapshots", "-U", Support.ADVERTISED),
    NO_SNAPSHOT_UPDATES("noSnapshotUpdates", "-nsu", Support.ADVERTISED),
    UPDATE_ARTIFACTS("updateArtifacts", "-UA", Support.ADVERTISED),
    UPDATE_METADATA("updateMetadata", "-UM", Support.ADVERTISED),
    NON_RECURSIVE("nonRecursive", "-N", Support.ADVERTISED),
    ALSO_MAKE("alsoMake", "-am", Support.ADVERTISED),
    ALSO_MAKE_DEPENDENTS("alsoMakeDependents", "-amd", Support.ADVERTISED),
    SKIP_TESTS("skipTests", "-DskipTests", Support.PROPERTY),
    SHOW_ERRORS("showErrors", "-e", Support.ADVERTISED),
    BATCH_MODE("batchMode", "-B", Support.ADVERTISED),
    NO_TRANSFER_PROGRESS("noTransferProgress", "-ntp", Support.ADVERTISED),
    STRICT_CHECKSUMS("strictChecksums", "-C", Support.ADVERTISED),
    LAX_CHECKSUMS("laxChecksums", "-c", Support.ADVERTISED),
    IGNORE_TRANSITIVE_REPOSITORIES("ignoreTransitiveRepositories", "-itr", Support.ADVERTISED),
    RESUME("resume", "-r", Support.ADVERTISED),
    NO_DAEMON("noDaemon", "-Dmvnd.noDaemon=true", Support.DAEMON_ONLY);

    public enum Support
    {
        ADVERTISED,
        PROPERTY,
        DAEMON_ONLY;

        public boolean isSupported(String option, Set<String> advertisedOptions, boolean daemon)
        {
            return switch (this)
            {
                case ADVERTISED -> advertisedOptions.isEmpty() || advertisedOptions.contains(option);
                case PROPERTY -> true;
                case DAEMON_ONLY -> daemon;
            };
        }
    }

    private final String bundleKey;

    private final String option;

    private final Support support;

    MavenFlag(String bundleKey, String option, Support support)
    {
        this.bundleKey = bundleKey;
        this.option = option;
        this.support = support;
    }

    public String getOption()
    {
        return option;
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.flag." + bundleKey);
    }

    public String getDescription()
    {
        return MavenPrimeBundle.message("mavenprime.flag." + bundleKey + ".description");
    }

    public boolean isSupportedBy(MavenInstallation installation, Set<String> advertisedOptions)
    {
        return support.isSupported(option, advertisedOptions, installation.isDaemon());
    }
}
