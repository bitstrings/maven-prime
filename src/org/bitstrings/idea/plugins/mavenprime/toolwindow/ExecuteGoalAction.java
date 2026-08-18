package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

final class ExecuteGoalAction
    extends DumbAwareAction
{
    private final ExecutionMode mode;

    private final BooleanSupplier enabled;

    private final Consumer<ExecutionMode> execute;

    ExecuteGoalAction(ExecutionMode mode, BooleanSupplier enabled, Consumer<ExecutionMode> execute)
    {
        super(
            MavenPrimeBundle.message(
                (mode == ExecutionMode.DEBUG) ? "mavenprime.action.debugGoal" : "mavenprime.action.runGoal"),
            null,
            (mode == ExecutionMode.DEBUG) ? AllIcons.Actions.StartDebugger : AllIcons.Actions.Execute);

        this.mode = mode;
        this.enabled = enabled;
        this.execute = execute;
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        execute.accept(mode);
    }

    @Override
    public void update(AnActionEvent event)
    {
        event.getPresentation().setEnabled(enabled.getAsBoolean());
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.EDT;
    }
}
