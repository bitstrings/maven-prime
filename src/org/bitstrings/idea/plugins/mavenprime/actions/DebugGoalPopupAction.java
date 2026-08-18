package org.bitstrings.idea.plugins.mavenprime.actions;

import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;

public final class DebugGoalPopupAction
    extends RunGoalPopupAction
{
    public DebugGoalPopupAction()
    {
        super(ExecutionMode.DEBUG);
    }
}
