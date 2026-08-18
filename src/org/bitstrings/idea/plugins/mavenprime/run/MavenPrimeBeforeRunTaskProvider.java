package org.bitstrings.idea.plugins.mavenprime.run;

import javax.swing.Icon;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeIcons;
import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeExecutor;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;
import org.bitstrings.idea.plugins.mavenprime.ui.GoalPickerDialog;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

public final class MavenPrimeBeforeRunTaskProvider
    extends BeforeRunTaskProvider<MavenPrimeBeforeRunTask>
{
    static final Key<MavenPrimeBeforeRunTask> ID = Key.create("MavenPrime.BeforeRunTask");

    private final Project project;

    public MavenPrimeBeforeRunTaskProvider(Project project)
    {
        this.project = project;
    }

    @Override
    public Key<MavenPrimeBeforeRunTask> getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return MavenPrimeBundle.message("mavenprime.beforeRun.name");
    }

    @Override
    public Icon getIcon()
    {
        return MavenPrimeIcons.ACTION;
    }

    @Override
    public Icon getTaskIcon(MavenPrimeBeforeRunTask task)
    {
        return MavenPrimeIcons.ACTION;
    }

    @Override
    public String getDescription(MavenPrimeBeforeRunTask task)
    {
        ScopedGoal goal = goalOf(task);

        return (goal == null)
            ? MavenPrimeBundle.message("mavenprime.beforeRun.description.unknown")
            : MavenPrimeBundle.message("mavenprime.beforeRun.description", goal.getDisplayName());
    }

    @Override
    public boolean isConfigurable()
    {
        return true;
    }

    @Override
    public MavenPrimeBeforeRunTask createTask(RunConfiguration configuration)
    {
        return MavenProjects.isPossiblyMavenized(project) ? new MavenPrimeBeforeRunTask() : null;
    }

    @Override
    public Promise<Boolean> configureTask(
        DataContext context, RunConfiguration configuration, MavenPrimeBeforeRunTask task)
    {
        ScopedGoal chosen = GoalPickerDialog.pick(project, goalOf(task));

        if (chosen != null)
        {
            task.setGoalId(chosen.definition().id);
        }

        return Promises.resolvedPromise(Boolean.valueOf(chosen != null));
    }

    @Override
    public boolean canExecuteTask(RunConfiguration configuration, MavenPrimeBeforeRunTask task)
    {
        return goalOf(task) != null;
    }

    @Override
    public boolean executeTask(
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        MavenPrimeBeforeRunTask task)
    {
        ScopedGoal goal = goalOf(task);

        if (goal == null)
        {
            return false;
        }

        return MavenPrimeExecutor.getInstance(project).executeAndWait(requestFor(goal), ExecutionMode.RUN);
    }

    static MavenPrimeRequest requestFor(ScopedGoal goal)
    {
        MavenPrimeRequest request = goal.definition().template.copy();

        request.name = goal.getDisplayName();

        return request;
    }

    private ScopedGoal goalOf(MavenPrimeBeforeRunTask task)
    {
        return StringUtils.isBlank(task.getGoalId())
            ? null
            : GoalRegistry.getInstance(project).find(task.getGoalId());
    }
}
