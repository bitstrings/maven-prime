package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.List;
import java.util.function.Consumer;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;

public final class GoalChooser
{
    private GoalChooser()
    {
    }

    public static void choose(Project project, MavenProject module, Consumer<ScopedGoal> chosen)
    {
        List<ScopedGoal> goals = GoalRegistry.getInstance(project).all();

        if (goals.isEmpty())
        {
            MavenPrimeNotifications.info(project, MavenPrimeBundle.message("mavenprime.popup.noGoals"));

            return;
        }

        JBPopupFactory
            .getInstance()
            .createPopupChooserBuilder(goals)
            .setTitle(MavenPrimeBundle.message("mavenprime.popup.chooseGoal", module.getDisplayName()))
            .setRenderer(new ScopedGoalRenderer())
            .setNamerForFiltering(GoalPresentation::filterTextOf)
            .setItemChosenCallback(chosen::accept)
            .createPopup()
            .showCenteredInCurrentWindow(project);
    }
}
