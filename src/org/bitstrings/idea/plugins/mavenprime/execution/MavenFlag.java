package org.bitstrings.idea.plugins.mavenprime.execution;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.distribution.MavenVersion;

public enum MavenFlag
{
    OFFLINE("offline", "-o", Support.ALWAYS),
    UPDATE_SNAPSHOTS("updateSnapshots", "-U", Support.ALWAYS),
    NO_SNAPSHOT_UPDATES("noSnapshotUpdates", "-nsu", Support.ALWAYS),
    NON_RECURSIVE("nonRecursive", "-N", Support.ALWAYS),
    ALSO_MAKE("alsoMake", "-am", Support.ALWAYS),
    ALSO_MAKE_DEPENDENTS("alsoMakeDependents", "-amd", Support.ALWAYS),
    SKIP_TESTS("skipTests", "-DskipTests", Support.ALWAYS),
    SHOW_ERRORS("showErrors", "-e", Support.ALWAYS),
    BATCH_MODE("batchMode", "-B", Support.ALWAYS),
    NO_TRANSFER_PROGRESS("noTransferProgress", "-ntp", Support.ALWAYS),
    STRICT_CHECKSUMS("strictChecksums", "-C", Support.ALWAYS),
    LAX_CHECKSUMS("laxChecksums", "-c", Support.ALWAYS),
    IGNORE_TRANSITIVE_REPOSITORIES("ignoreTransitiveRepositories", "-itr", Support.SINCE_MAVEN_3_9),
    RESUME("resume", "-r", Support.MAVEN_4_OR_DAEMON),
    NO_DAEMON("noDaemon", "-Dmvnd.noDaemon=true", Support.DAEMON_ONLY);

    public enum Support
    {
        ALWAYS,
        SINCE_MAVEN_3_9,
        MAVEN_4_OR_DAEMON,
        DAEMON_ONLY;

        private static final int MAVEN_3_9_MAJOR = 3;

        private static final int MAVEN_3_9_MINOR = 9;

        private static final int MAVEN_4_MAJOR = 4;

        public boolean isSupported(MavenVersion mavenVersion, boolean daemon)
        {
            return switch (this)
            {
                case ALWAYS -> true;
                case SINCE_MAVEN_3_9 -> mavenVersion.isAtLeast(MAVEN_3_9_MAJOR, MAVEN_3_9_MINOR);
                case MAVEN_4_OR_DAEMON -> daemon || mavenVersion.isAtLeast(MAVEN_4_MAJOR);
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

    public boolean isSupportedBy(MavenVersion mavenVersion, boolean daemon)
    {
        return support.isSupported(mavenVersion, daemon);
    }
}
