package org.bitstrings.idea.plugins.mavenprime.run;

import java.util.List;
import java.util.Objects;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public final class MavenPrimeTestConfigurationProducer
    extends LazyRunConfigurationProducer<MavenPrimeRunConfiguration>
{
    private static final String TEST_GOAL = "test";

    @Override
    public ConfigurationFactory getConfigurationFactory()
    {
        return MavenPrimeRunConfigurationType.getInstance().getFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(
        MavenPrimeRunConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement)
    {
        TestSelection selection = TestSelections.of(context.getPsiLocation());

        MavenProject module = moduleOf(context.getProject(), selection);

        if (module == null)
        {
            return false;
        }

        configuration.setRequest(requestFor(module.getDirectory(), selection));
        configuration.setName(nameFor(selection));

        sourceElement.set(selection.element());

        return true;
    }

    @Override
    public boolean isConfigurationFromContext(
        MavenPrimeRunConfiguration configuration, ConfigurationContext context)
    {
        TestSelection selection = TestSelections.of(context.getPsiLocation());

        MavenProject module = moduleOf(context.getProject(), selection);

        if (module == null)
        {
            return false;
        }

        MavenPrimeRequest request = configuration.getRequest();

        return Objects.equals(request.workingDirectory, module.getDirectory())
            && selection.filter().equals(request.properties.get(MavenPrimeRequest.TEST_PROPERTY));
    }

    static MavenPrimeRequest requestFor(String moduleDirectory, TestSelection selection)
    {
        MavenPrimeRequest request = MavenPrimeRequest.in(moduleDirectory, List.of(TEST_GOAL));

        request.properties.put(MavenPrimeRequest.TEST_PROPERTY, selection.filter());
        request.name = nameFor(selection);

        return request;
    }

    static String nameFor(TestSelection selection)
    {
        return MavenPrimeBundle.message("mavenprime.test.run.name", selection.label());
    }

    private static MavenProject moduleOf(Project project, TestSelection selection)
    {
        if (selection == null)
        {
            return null;
        }

        PsiFile file = selection.element().getContainingFile();

        return (file == null) ? null : MavenProjects.forFile(project, file.getVirtualFile());
    }
}
