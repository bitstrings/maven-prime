package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;

public record ModuleContext(MavenProject module, ModuleContext.Kind kind)
{
    public enum Kind
    {
        FOLLOWING,
        PINNED,
        PINNED_MISSING
    }

    public static ModuleContext resolve(
        String pinnedPath, List<MavenProject> modules, MavenProject followed)
    {
        if (StringUtils.isBlank(pinnedPath))
        {
            return new ModuleContext(followed, Kind.FOLLOWING);
        }

        MavenProject pinned = find(modules, pinnedPath);

        return (pinned == null)
            ? new ModuleContext(null, Kind.PINNED_MISSING)
            : new ModuleContext(pinned, Kind.PINNED);
    }

    public boolean isPinned()
    {
        return kind != Kind.FOLLOWING;
    }

    private static MavenProject find(List<MavenProject> modules, String path)
    {
        for (MavenProject candidate : modules)
        {
            if (candidate.getFile().getPath().equals(path))
            {
                return candidate;
            }
        }

        return null;
    }
}
