package org.bitstrings.idea.plugins.mavenprime.goals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bitstrings.idea.plugins.mavenprime.config.MavenPrimeConfigService;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class GoalRegistry
    implements Disposable
{
    private final Project project;

    public GoalRegistry(Project project)
    {
        this.project = project;
    }

    public static GoalRegistry getInstance(Project project)
    {
        return project.getService(GoalRegistry.class);
    }

    @Override
    public void dispose()
    {
    }

    public void listen()
    {
        project
            .getMessageBus()
            .connect(this)
            .subscribe(
                MavenPrimeConfigService.TOPIC,
                (MavenPrimeConfigService.ConfigListener) () -> GoalActions.getInstance().sync());
    }

    public List<ScopedGoal> all()
    {
        List<ScopedGoal> goals = new ArrayList<>();

        for (GoalDefinition definition : projectScoped())
        {
            goals.add(new ScopedGoal(GoalScope.PROJECT, definition));
        }

        for (GoalDefinition definition : ApplicationGoalStore.getInstance().getGoals())
        {
            goals.add(new ScopedGoal(GoalScope.GLOBAL, definition));
        }

        return goals;
    }

    public List<GoalDefinition> global()
    {
        return ApplicationGoalStore.getInstance().getGoals();
    }

    public List<GoalDefinition> projectScoped()
    {
        return MavenPrimeConfigService.getInstance(project).getGoals();
    }

    public boolean contains(String goalId)
    {
        return MavenPrimeConfigService.getInstance(project).hasGoal(goalId)
            || ApplicationGoalStore.getInstance().hasGoal(goalId);
    }

    public ScopedGoal find(String goalId)
    {
        for (ScopedGoal goal : all())
        {
            if (Objects.equals(goal.definition().id, goalId))
            {
                return goal;
            }
        }

        return null;
    }

    public void updateGlobal(List<GoalDefinition> goals)
    {
        ApplicationGoalStore.getInstance().setGoals(goals);

        GoalActions.getInstance().sync();
    }

    public void updateProject(List<GoalDefinition> goals)
    {
        MavenPrimeConfigService.getInstance(project).setGoals(goals);

        GoalActions.getInstance().sync();
    }

    public String add(GoalScope scope, GoalDefinition definition)
    {
        List<GoalDefinition> goals = scoped(scope);

        GoalDefinition added = definition.copy();

        added.id =
            GoalIds.uniqueIn(
                added.derivedId(),
                goals.stream().map(existing -> existing.id).collect(Collectors.toSet()));

        goals.add(added);

        store(scope, goals);

        return added.id;
    }

    public void replace(GoalScope scope, GoalDefinition definition)
    {
        mutate(
            scope,
            goals ->
            {
                int index = indexOf(goals, definition.id);

                if (index < 0)
                {
                    goals.add(definition);
                }
                else
                {
                    goals.set(index, definition);
                }
            });
    }

    public void remove(GoalScope scope, GoalDefinition definition)
    {
        mutate(scope, goals -> goals.removeIf(candidate -> Objects.equals(candidate.id, definition.id)));
    }

    public boolean canMove(GoalScope scope, String goalId, int delta)
    {
        return targetOf(scoped(scope), goalId, delta) >= 0;
    }

    public boolean move(GoalScope scope, String goalId, int delta)
    {
        if (!canMove(scope, goalId, delta))
        {
            return false;
        }

        mutate(
            scope,
            goals ->
            {
                int index = indexOf(goals, goalId);

                goals.add(index + delta, goals.remove(index));
            });

        return true;
    }

    private static int targetOf(List<GoalDefinition> goals, String goalId, int delta)
    {
        int index = indexOf(goals, goalId);
        int target = index + delta;

        return ((index < 0) || (target < 0) || (target >= goals.size())) ? -1 : target;
    }

    private List<GoalDefinition> scoped(GoalScope scope)
    {
        return (scope == GoalScope.PROJECT) ? projectScoped() : global();
    }

    private void mutate(GoalScope scope, Consumer<List<GoalDefinition>> change)
    {
        List<GoalDefinition> goals = scoped(scope);

        change.accept(goals);

        store(scope, goals);
    }

    private void store(GoalScope scope, List<GoalDefinition> goals)
    {
        if (scope == GoalScope.PROJECT)
        {
            updateProject(goals);
        }
        else
        {
            updateGlobal(goals);
        }
    }

    private static int indexOf(List<GoalDefinition> goals, String id)
    {
        for (int index = 0; index < goals.size(); index++)
        {
            if (Objects.equals(goals.get(index).id, id))
            {
                return index;
            }
        }

        return -1;
    }
}
