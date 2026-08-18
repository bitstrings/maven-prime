package org.bitstrings.idea.plugins.mavenprime.run;

import java.util.Objects;

import org.bitstrings.idea.plugins.mavenprime.editor.PomRunTarget;
import org.bitstrings.idea.plugins.mavenprime.editor.PomRunTargets;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;
import org.bitstrings.idea.plugins.mavenprime.ui.GoalChooser;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;

public final class MavenPrimeConfigurationProducer
    extends LazyRunConfigurationProducer<MavenPrimeRunConfiguration>
{
    @Override
    public ConfigurationFactory getConfigurationFactory()
    {
        return MavenPrimeRunConfigurationType.getInstance().getFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(
        MavenPrimeRunConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement)
    {
        PsiElement location = context.getPsiLocation();

        PomRunTarget target = PomRunTargets.of(location);

        if (target == null)
        {
            return false;
        }

        configuration.setRequest(MavenPrimeRequest.in(target.module().getDirectory(), target.goals()));
        configuration.setName(target.label());

        sourceElement.set(location);

        return true;
    }

    @Override
    public boolean isConfigurationFromContext(
        MavenPrimeRunConfiguration configuration, ConfigurationContext context)
    {
        PomRunTarget target = PomRunTargets.of(context.getPsiLocation());

        if (target == null)
        {
            return false;
        }

        MavenPrimeRequest request = configuration.getRequest();

        return Objects.equals(request.workingDirectory, target.module().getDirectory())
            && (target.needsGoalChoice() || request.goals.equals(target.goals()));
    }

    @Override
    public void onFirstRun(
        ConfigurationFromContext fromContext, ConfigurationContext context, Runnable startRunnable)
    {
        PomRunTarget target = PomRunTargets.of(context.getPsiLocation());

        if ((target == null) || !target.needsGoalChoice())
        {
            super.onFirstRun(fromContext, context, startRunnable);

            return;
        }

        GoalChooser.choose(
            context.getProject(),
            target.module(),
            goal -> startWith(fromContext, target, goal, startRunnable));
    }

    private static void startWith(
        ConfigurationFromContext fromContext, PomRunTarget target, ScopedGoal goal, Runnable startRunnable)
    {
        MavenPrimeRequest request = goal.definition().template.copy();

        request.workingDirectory = target.module().getDirectory();
        request.name = goal.getDisplayName();

        MavenPrimeRunConfiguration configuration = (MavenPrimeRunConfiguration) fromContext.getConfiguration();

        configuration.setRequest(request);
        configuration.setName(goal.getDisplayName());

        startRunnable.run();
    }
}
