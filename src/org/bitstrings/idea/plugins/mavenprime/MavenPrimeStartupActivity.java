package org.bitstrings.idea.plugins.mavenprime;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalActions;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.model.ModelAutoRefresher;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

public final class MavenPrimeStartupActivity
    implements ProjectActivity
{
    @Override
    public Object execute(Project project, Continuation<? super Unit> continuation)
    {
        GoalRegistry.getInstance(project).listen();

        GoalActions.getInstance().sync();

        BuildContext.getInstance(project).listen();

        ModelAutoRefresher.getInstance(project).listen();

        return Unit.INSTANCE;
    }
}
