package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.Objects;

import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public final class ArtifactSearchContributorFactory
    implements SearchEverywhereContributorFactory<ArtifactCoordinates>
{
    @Override
    public boolean isAvailable(Project project)
    {
        return MavenProjects.isMavenized(project) && MavenPrimeSettings.getInstance(project).searchEverywhere;
    }

    @Override
    public SearchEverywhereContributor<ArtifactCoordinates> createContributor(AnActionEvent event)
    {
        return new ArtifactSearchContributor(
            Objects.requireNonNull(event.getProject(), "Search Everywhere was opened without a project"));
    }
}
