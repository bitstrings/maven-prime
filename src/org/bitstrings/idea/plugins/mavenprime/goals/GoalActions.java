package org.bitstrings.idea.plugins.mavenprime.goals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.util.concurrency.AppExecutorUtil;

@Service
public final class GoalActions
    implements Disposable
{
    static final String PLUGIN_ID = "org.bitstrings.idea.plugins.MavenPrime";

    static final String ID_PREFIX = "MavenPrime.Goal.";

    static final String DEBUG_ID_PREFIX = "MavenPrime.DebugGoal.";

    static final String GOALS_GROUP_ID = "MavenPrime.Goals";

    static final String DEBUG_GOALS_GROUP_ID = "MavenPrime.DebugGoals";

    private final Object lock = new Object();

    private final Map<ExecutionMode, Set<String>> registered = new EnumMap<>(ExecutionMode.class);

    public GoalActions()
    {
        ApplicationManager
            .getApplication()
            .getMessageBus()
            .connect(this)
            .subscribe(
                ProjectManager.TOPIC,
                new ProjectManagerListener()
                {
                    @Override
                    public void projectClosed(Project closed)
                    {
                        sync();
                    }
                });
    }

    public static GoalActions getInstance()
    {
        return ApplicationManager.getApplication().getService(GoalActions.class);
    }

    @Override
    public void dispose()
    {
    }

    public static String actionIdOf(String goalId, ExecutionMode mode)
    {
        return idPrefixOf(mode) + goalId;
    }

    public void sync()
    {
        ReadAction
            .nonBlocking(GoalActions::collect)
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.any(), this::apply)
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    private static Map<String, GoalLabel> collect()
    {
        Map<String, GoalLabel> wanted = new LinkedHashMap<>();

        for (Project project : ProjectManager.getInstance().getOpenProjects())
        {
            if (project.isDisposed())
            {
                continue;
            }

            for (ScopedGoal goal : GoalRegistry.getInstance(project).all())
            {
                wanted.putIfAbsent(goal.definition().id, GoalLabel.of(goal));
            }
        }

        return wanted;
    }

    void apply(Map<String, GoalLabel> wanted)
    {
        ActionManager actions = ActionManager.getInstance();

        synchronized (lock)
        {
            for (ExecutionMode mode : ExecutionMode.values())
            {
                applyFor(actions, mode, wanted);
            }
        }
    }

    private void applyFor(ActionManager actions, ExecutionMode mode, Map<String, GoalLabel> wanted)
    {
        Set<String> registeredGoalIds = goalsRegisteredFor(mode);

        for (String staleGoalId : new ArrayList<>(registeredGoalIds))
        {
            if (!wanted.containsKey(staleGoalId))
            {
                actions.unregisterAction(idPrefixOf(mode) + staleGoalId);

                registeredGoalIds.remove(staleGoalId);
            }
        }

        wanted.forEach((goalId, label) -> register(actions, mode, goalId, label));

        regroup(actions, mode, wanted.keySet());
    }

    private void register(ActionManager actions, ExecutionMode mode, String goalId, GoalLabel label)
    {
        String actionId = idPrefixOf(mode) + goalId;

        AnAction existing = actions.getAction(actionId);

        if (existing instanceof GoalAction)
        {
            ((GoalAction) existing).setLabel(label);
        }
        else
        {
            actions.registerAction(actionId, new GoalAction(goalId, label, mode), PluginId.getId(PLUGIN_ID));
        }

        goalsRegisteredFor(mode).add(goalId);
    }

    private static void regroup(ActionManager actions, ExecutionMode mode, Collection<String> goalIds)
    {
        AnAction group = actions.getAction(groupIdOf(mode));

        if (!(group instanceof DefaultActionGroup))
        {
            return;
        }

        DefaultActionGroup goals = (DefaultActionGroup) group;

        goals.removeAll();

        for (String goalId : goalIds)
        {
            AnAction goal = actions.getAction(idPrefixOf(mode) + goalId);

            if (goal != null)
            {
                goals.add(goal);
            }
        }
    }

    private Set<String> goalsRegisteredFor(ExecutionMode mode)
    {
        return registered.computeIfAbsent(mode, absent -> new HashSet<>());
    }

    private static String idPrefixOf(ExecutionMode mode)
    {
        return mode.isDebug() ? DEBUG_ID_PREFIX : ID_PREFIX;
    }

    private static String groupIdOf(ExecutionMode mode)
    {
        return mode.isDebug() ? DEBUG_GOALS_GROUP_ID : GOALS_GROUP_ID;
    }
}
