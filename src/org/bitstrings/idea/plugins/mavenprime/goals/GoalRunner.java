package org.bitstrings.idea.plugins.mavenprime.goals;

import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeExecutor;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.project.Project;

public final class GoalRunner
{
    private GoalRunner()
    {
    }

    public static boolean run(Project project, ScopedGoal goal, MavenProject module, ExecutionMode mode)
    {
        if ((goal == null) || (module == null))
        {
            return false;
        }

        MavenPrimeRequest request = goal.definition().template.copy();

        request.workingDirectory = module.getDirectory();
        request.name = goal.getDisplayName();

        MavenPrimeExecutor.getInstance(project).execute(request, mode);

        return true;
    }
}
