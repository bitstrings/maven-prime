package org.bitstrings.idea.plugins.mavenprime.actions;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeExecutor;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.utils.MavenDataKeys;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

public class RunSelectedGoalsAction
    extends DumbAwareAction
{
    private final ExecutionMode mode;

    protected RunSelectedGoalsAction(ExecutionMode mode)
    {
        this.mode = mode;
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        Project project = event.getProject();

        MavenProject module = MavenPrimeActionContext.resolveModule(event);

        List<String> goals = event.getData(MavenDataKeys.MAVEN_GOALS);

        if ((project == null) || (module == null) || (goals == null) || goals.isEmpty())
        {
            return;
        }

        MavenPrimeRequest request = MavenPrimeRequest.in(module.getDirectory(), goals);

        request.name = request.getGoalLine();

        MavenPrimeExecutor.getInstance(project).execute(request, mode);
    }

    @Override
    public void update(AnActionEvent event)
    {
        List<String> goals = event.getData(MavenDataKeys.MAVEN_GOALS);

        event
            .getPresentation()
            .setEnabledAndVisible(
                (event.getProject() != null)
                    && (goals != null)
                    && !goals.isEmpty()
                    && (MavenPrimeActionContext.resolveModule(event) != null));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }

    public static final class Run
        extends RunSelectedGoalsAction
    {
        public Run()
        {
            super(ExecutionMode.RUN);
        }
    }

    public static final class Debug
        extends RunSelectedGoalsAction
    {
        public Debug()
        {
            super(ExecutionMode.DEBUG);
        }
    }
}
