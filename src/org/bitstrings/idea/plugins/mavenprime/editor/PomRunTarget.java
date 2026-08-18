package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.List;

import org.jetbrains.idea.maven.project.MavenProject;

public record PomRunTarget(MavenProject module, List<String> goals, String label)
{
    public PomRunTarget
    {
        goals = List.copyOf(goals);
    }

    public boolean needsGoalChoice()
    {
        return goals.isEmpty();
    }
}
