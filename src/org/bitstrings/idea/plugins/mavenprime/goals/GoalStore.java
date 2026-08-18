package org.bitstrings.idea.plugins.mavenprime.goals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.intellij.openapi.components.PersistentStateComponent;

public abstract class GoalStore
    implements PersistentStateComponent<GoalList>
{
    private final Object stateLock = new Object();

    private GoalList state = new GoalList();

    private Set<String> goalIds;

    @Override
    public GoalList getState()
    {
        GoalList snapshot = new GoalList();

        synchronized (stateLock)
        {
            snapshot.appliedDefaults =
                (state.appliedDefaults == null) ? null : new ArrayList<>(state.appliedDefaults);
            snapshot.goals.addAll(getGoals());
        }

        return snapshot;
    }

    @Override
    public void loadState(GoalList loaded)
    {
        synchronized (stateLock)
        {
            state = loaded;
            goalIds = null;

            assignIds();
        }
    }

    public List<GoalDefinition> getGoals()
    {
        synchronized (stateLock)
        {
            List<GoalDefinition> goals = new ArrayList<>(state.goals.size());

            for (GoalDefinition definition : state.goals)
            {
                if (definition.isComplete())
                {
                    goals.add(definition.copy());
                }
            }

            return goals;
        }
    }

    public boolean hasGoal(String goalId)
    {
        if (goalId == null)
        {
            return false;
        }

        synchronized (stateLock)
        {
            if (goalIds == null)
            {
                goalIds = currentGoalIds();
            }

            return goalIds.contains(goalId);
        }
    }

    private Set<String> currentGoalIds()
    {
        Set<String> ids = new HashSet<>();

        for (GoalDefinition definition : state.goals)
        {
            if (definition.isComplete())
            {
                ids.add(definition.id);
            }
        }

        return Set.copyOf(ids);
    }

    public void setGoals(List<GoalDefinition> goals)
    {
        synchronized (stateLock)
        {
            state.goals.clear();

            for (GoalDefinition definition : goals)
            {
                state.goals.add(definition.copy());
            }

            assignIds();

            goalIds = null;
        }
    }

    public void add(GoalDefinition definition)
    {
        synchronized (stateLock)
        {
            state.goals.add(definition.copy());

            assignIds();

            goalIds = null;
        }
    }

    protected void seedDefaults(List<GoalDefinition> defaults)
    {
        synchronized (stateLock)
        {
            state.goals.clear();
            state.appliedDefaults = new ArrayList<>();
            goalIds = null;
        }

        mergeDefaults(defaults);
    }

    protected void mergeDefaults(List<GoalDefinition> defaults)
    {
        synchronized (stateLock)
        {
            if (state.appliedDefaults == null)
            {
                rederiveIds();

                state.appliedDefaults = idsInOrder();
            }

            for (GoalDefinition definition : defaults)
            {
                String id = definition.id;

                if (!state.appliedDefaults.contains(id))
                {
                    GoalDefinition added = definition.copy();

                    added.id = id;

                    state.goals.add(added);
                    state.appliedDefaults.add(id);
                }
            }

            assignIds();

            goalIds = null;
        }
    }

    private void rederiveIds()
    {
        for (GoalDefinition definition : state.goals)
        {
            definition.id = null;
        }

        assignIds();
    }

    private List<String> idsInOrder()
    {
        List<String> ids = new ArrayList<>(state.goals.size());

        for (GoalDefinition definition : state.goals)
        {
            ids.add(definition.id);
        }

        return ids;
    }

    private void assignIds()
    {
        Set<String> taken = new LinkedHashSet<>();

        for (GoalDefinition definition : state.goals)
        {
            if (StringUtils.isNotBlank(definition.id) && taken.add(definition.id))
            {
                continue;
            }

            definition.id = GoalIds.uniqueIn(definition.derivedId(), taken);

            taken.add(definition.id);
        }
    }
}
