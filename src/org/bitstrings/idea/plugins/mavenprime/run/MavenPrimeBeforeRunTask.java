package org.bitstrings.idea.plugins.mavenprime.run;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.components.PersistentStateComponent;

public final class MavenPrimeBeforeRunTask
    extends BeforeRunTask<MavenPrimeBeforeRunTask>
    implements PersistentStateComponent<MavenPrimeBeforeRunTask.State>
{
    private String goalId = StringUtils.EMPTY;

    MavenPrimeBeforeRunTask()
    {
        super(MavenPrimeBeforeRunTaskProvider.ID);
    }

    public String getGoalId()
    {
        return goalId;
    }

    public void setGoalId(String goalId)
    {
        this.goalId = StringUtils.defaultString(goalId);
    }

    @Override
    public State getState()
    {
        State state = new State();

        state.goalId = goalId;

        return state;
    }

    @Override
    public void loadState(State state)
    {
        setGoalId(state.goalId);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof MavenPrimeBeforeRunTask) || !super.equals(other))
        {
            return false;
        }

        return goalId.equals(((MavenPrimeBeforeRunTask) other).goalId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(Integer.valueOf(super.hashCode()), goalId);
    }

    public static final class State
    {
        public String goalId = StringUtils.EMPTY;
    }
}
