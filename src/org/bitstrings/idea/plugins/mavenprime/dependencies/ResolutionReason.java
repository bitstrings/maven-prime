package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.jetbrains.idea.maven.model.MavenArtifactNode;

public enum ResolutionReason
{
    INCLUDED("included"),
    MANAGED("managed"),
    CONFLICT_LOSER("conflict"),
    DUPLICATE("duplicate"),
    EXCLUDED("excluded"),
    CYCLE("cycle");

    private final String bundleKey;

    ResolutionReason(String bundleKey)
    {
        this.bundleKey = bundleKey;
    }

    public static ResolutionReason of(MavenArtifactNode node)
    {
        switch (node.getState())
        {
            case CONFLICT:
                return CONFLICT_LOSER;

            case DUPLICATE:
                return DUPLICATE;

            case EXCLUDED:
                return EXCLUDED;

            case CYCLE:
                return CYCLE;

            default:
                return StringUtils.isNotBlank(node.getPremanagedVersion()) ? MANAGED : INCLUDED;
        }
    }

    public boolean isOmitted()
    {
        return (this == CONFLICT_LOSER) || (this == DUPLICATE) || (this == EXCLUDED) || (this == CYCLE);
    }

    public VersionTransition transition(MavenArtifactNode node)
    {
        switch (this)
        {
            case MANAGED:
                return distinct(node.getPremanagedVersion(), node.getArtifact().getVersion());

            case CONFLICT_LOSER:
                return (node.getRelatedArtifact() == null)
                    ? null
                    : distinct(node.getArtifact().getVersion(), node.getRelatedArtifact().getVersion());

            default:
                return null;
        }
    }

    private static VersionTransition distinct(String from, String to)
    {
        return Objects.equals(from, to) ? null : new VersionTransition(from, to);
    }

    public String marker()
    {
        switch (this)
        {
            case DUPLICATE:
            case EXCLUDED:
            case CYCLE:
                return MavenPrimeBundle.message("mavenprime.dependencies.marker." + bundleKey);

            default:
                return null;
        }
    }

    public String describe(MavenArtifactNode node)
    {
        switch (this)
        {
            case MANAGED:
                return MavenPrimeBundle.message(
                    "mavenprime.dependencies.reason.managed",
                    node.getPremanagedVersion(),
                    node.getArtifact().getVersion());

            case CONFLICT_LOSER:
                return MavenPrimeBundle.message(
                    "mavenprime.dependencies.reason.conflict",
                    (node.getRelatedArtifact() == null)
                        ? StringUtils.EMPTY
                        : node.getRelatedArtifact().getVersion());

            default:
                return MavenPrimeBundle.message("mavenprime.dependencies.reason." + bundleKey);
        }
    }
}
