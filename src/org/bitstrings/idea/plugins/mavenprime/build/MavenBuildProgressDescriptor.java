package org.bitstrings.idea.plugins.mavenprime.build;

import com.intellij.build.BuildDescriptor;
import com.intellij.build.DefaultBuildDescriptor;
import com.intellij.build.progress.BuildProgressDescriptor;

public final class MavenBuildProgressDescriptor
    implements BuildProgressDescriptor
{
    private final String title;

    private final BuildDescriptor buildDescriptor;

    public MavenBuildProgressDescriptor(Object buildId, String title, String workingDirectory)
    {
        this.title = title;
        this.buildDescriptor =
            new DefaultBuildDescriptor(buildId, title, workingDirectory, System.currentTimeMillis());
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public BuildDescriptor getBuildDescriptor()
    {
        return buildDescriptor;
    }
}
