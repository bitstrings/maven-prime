package org.bitstrings.idea.plugins.mavenprime.goals;

import javax.swing.Icon;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.actions.MavenPrimeActionContext;
import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

public final class GoalAction
    extends DumbAwareAction
{
    private final String goalId;

    private final ExecutionMode mode;

    GoalAction(String goalId, GoalLabel label, ExecutionMode mode)
    {
        super(label.getText(), descriptionOf(label, mode), iconOf(mode));

        this.goalId = goalId;
        this.mode = mode;
    }

    void setLabel(GoalLabel label)
    {
        getTemplatePresentation().setText(label.getText());
        getTemplatePresentation().setDescription(descriptionOf(label, mode));
    }

    private static String descriptionOf(GoalLabel label, ExecutionMode mode)
    {
        return MavenPrimeBundle.message(
            mode.isDebug() ? "mavenprime.goals.action.debug.description" : "mavenprime.goals.action.description",
            label.displayName());
    }

    private static Icon iconOf(ExecutionMode mode)
    {
        return mode.isDebug() ? AllIcons.Actions.StartDebugger : AllIcons.Actions.Execute;
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        Project project = event.getProject();

        if (project == null)
        {
            return;
        }

        ScopedGoal goal = GoalRegistry.getInstance(project).find(goalId);

        if (goal == null)
        {
            return;
        }

        GoalRunner.run(project, goal, MavenPrimeActionContext.resolveModule(event), mode);
    }

    @Override
    public void update(AnActionEvent event)
    {
        Project project = event.getProject();

        event
            .getPresentation()
            .setEnabledAndVisible(
                (project != null) && GoalRegistry.getInstance(project).contains(goalId));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }
}
