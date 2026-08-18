package org.bitstrings.idea.plugins.mavenprime.actions;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeExecutor;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;
import org.bitstrings.idea.plugins.mavenprime.ui.GoalChooser;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;

public class RunGoalPopupAction
    extends DumbAwareAction
{
    private final ExecutionMode mode;

    public RunGoalPopupAction()
    {
        this(ExecutionMode.RUN);
    }

    protected RunGoalPopupAction(ExecutionMode mode)
    {
        this.mode = mode;
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        Project project = event.getProject();

        if (project == null)
        {
            return;
        }

        MavenProject module = MavenPrimeActionContext.resolveModule(event);

        if (module == null)
        {
            chooseModule(project);

            return;
        }

        chooseGoal(project, module);
    }

    @Override
    public void update(AnActionEvent event)
    {
        Project project = event.getProject();

        event.getPresentation().setEnabledAndVisible((project != null) && MavenProjects.isMavenized(project));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }

    private void chooseModule(Project project)
    {
        List<MavenProject> modules = MavenProjects.all(project);

        if (modules.isEmpty())
        {
            return;
        }

        JBPopupFactory
            .getInstance()
            .createPopupChooserBuilder(modules)
            .setTitle(MavenPrimeBundle.message("mavenprime.popup.chooseModule"))
            .setNamerForFiltering(MavenProject::getDisplayName)
            .setItemChosenCallback(module -> chooseGoal(project, module))
            .createPopup()
            .showCenteredInCurrentWindow(project);
    }

    private void chooseGoal(Project project, MavenProject module)
    {
        GoalChooser.choose(project, module, goal -> run(project, module, goal));
    }

    private void run(Project project, MavenProject module, ScopedGoal goal)
    {
        MavenPrimeRequest request = goal.definition().template.copy();

        request.workingDirectory = module.getDirectory();
        request.name = goal.getDisplayName();

        MavenPrimeExecutor.getInstance(project).execute(request, mode);
    }
}
