package org.bitstrings.idea.plugins.mavenprime.model;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.util.MavenImports;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class ModelAutoRefresher
    implements BuildContext.ModelListener
{
    private final Project project;

    public ModelAutoRefresher(Project project)
    {
        this.project = project;
    }

    public static ModelAutoRefresher getInstance(Project project)
    {
        return project.getService(ModelAutoRefresher.class);
    }

    public void listen()
    {
        project.getMessageBus().connect(project).subscribe(BuildContext.MODEL_TOPIC, this);

        MavenImports.onImported(project, project, this::pomSourcesChanged);
    }

    @Override
    public void buildModelChanged()
    {
        reread();
    }

    void pomSourcesChanged()
    {
        reread();
    }

    private void reread()
    {
        if (MavenPrimeSettings.getInstance(project).autoRefresh && MavenProjects.isMavenized(project))
        {
            ModelRefresher.getInstance(project).refresh();
        }
    }
}
