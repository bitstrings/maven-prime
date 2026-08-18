package org.bitstrings.idea.plugins.mavenprime.build;

public sealed interface MavenLogEvent
{
    enum Severity
    {
        ERROR,
        WARNING
    }

    int NO_POSITION = -1;

    record ModuleStarted(String groupId, String artifactId)
        implements MavenLogEvent
    {
        public String coordinates()
        {
            return groupId + ':' + artifactId;
        }
    }

    record MojoStarted(String coordinates, String executionId, String module)
        implements MavenLogEvent
    {
    }

    record ModuleResult(String module, boolean success, boolean skipped, String cause)
        implements MavenLogEvent
    {
    }

    record Problem(Severity severity, String message, String path, int line, int column)
        implements MavenLogEvent
    {
        public static Problem withoutPosition(Severity severity, String message)
        {
            return new Problem(severity, message, null, NO_POSITION, NO_POSITION);
        }

        public boolean hasPosition()
        {
            return path != null;
        }
    }

    record BuildFinished(boolean success)
        implements MavenLogEvent
    {
    }
}
