package org.bitstrings.idea.plugins.mavenprime.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.intellij.util.execution.ParametersListUtil;

public final class RenderedCommandLine
{
    private final List<String> options;

    private final List<String> goals;

    private final Set<MavenFlag> droppedFlags;

    RenderedCommandLine(List<String> options, List<String> goals, Set<MavenFlag> droppedFlags)
    {
        this.options = List.copyOf(options);
        this.goals = List.copyOf(goals);
        this.droppedFlags = Set.copyOf(droppedFlags);
    }

    public List<String> getOptions()
    {
        return options;
    }

    public List<String> getGoals()
    {
        return goals;
    }

    public List<String> getArguments()
    {
        List<String> arguments = new ArrayList<>(options.size() + goals.size());

        arguments.addAll(options);
        arguments.addAll(goals);

        return arguments;
    }

    public Set<MavenFlag> getDroppedFlags()
    {
        return droppedFlags;
    }

    @Override
    public String toString()
    {
        return ParametersListUtil.join(getArguments());
    }
}
