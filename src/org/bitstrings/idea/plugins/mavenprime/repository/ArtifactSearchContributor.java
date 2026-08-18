package org.bitstrings.idea.plugins.mavenprime.repository;

import java.nio.file.Path;

import javax.swing.ListCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.toolwindow.RepositoryInspection;
import org.bitstrings.idea.plugins.mavenprime.ui.ArtifactCoordinatesRenderer;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.util.Processor;

public final class ArtifactSearchContributor
    implements SearchEverywhereContributor<ArtifactCoordinates>
{
    private static final int SORT_WEIGHT = 700;

    private final Project project;

    public ArtifactSearchContributor(Project project)
    {
        this.project = project;
    }

    @Override
    public String getSearchProviderId()
    {
        return ArtifactSearchContributor.class.getName();
    }

    @Override
    public String getGroupName()
    {
        return MavenPrimeBundle.message("mavenprime.repository.searchEverywhere.group");
    }

    @Override
    public int getSortWeight()
    {
        return SORT_WEIGHT;
    }

    @Override
    public boolean showInFindResults()
    {
        return false;
    }

    @Override
    public boolean isDumbAware()
    {
        return true;
    }

    @Override
    public void fetchElements(
        String pattern, ProgressIndicator indicator, Processor<? super ArtifactCoordinates> consumer)
    {
        if (StringUtils.isBlank(pattern))
        {
            return;
        }

        LocalRepositoryIndex index = LocalRepositoryIndex.getInstance(project);

        index.ensureIndexed(
            ApplicationManager
                .getApplication()
                .runReadAction((Computable<Path>) () -> LocalRepositoryLocator.locate(project)));

        for (ArtifactCoordinates coordinates : index.search(pattern, LocalRepositoryIndex.DEFAULT_LIMIT))
        {
            indicator.checkCanceled();

            if (!consumer.process(coordinates))
            {
                return;
            }
        }
    }

    @Override
    public boolean processSelectedItem(ArtifactCoordinates selected, int modifiers, String text)
    {
        RepositoryInspection.inspect(project, selected);

        return true;
    }

    @Override
    public ListCellRenderer<? super ArtifactCoordinates> getElementsRenderer()
    {
        return new ArtifactCoordinatesRenderer();
    }

    @Override
    public String getItemDescription(ArtifactCoordinates element)
    {
        return element.toString();
    }
}
