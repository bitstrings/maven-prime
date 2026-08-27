package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactDownloading;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

@Service(Service.Level.PROJECT)
public final class ResolutionLog
{
    public static final Topic<ResolutionListener> TOPIC =
        Topic.create("Maven Prime resolution log", ResolutionListener.class);

    private final Project project;

    private final BuildHistory<ResolutionData> history =
        new BuildHistory<>(ResolutionData::new, this::retainedBuilds);

    public ResolutionLog(Project project)
    {
        this.project = project;
    }

    private int retainedBuilds()
    {
        return MavenPrimeSettings.getInstance(project).retainedBuilds;
    }

    public static ResolutionLog getInstance(Project project)
    {
        return project.getService(ResolutionLog.class);
    }

    public ResolutionData startBuild(String name, String goals)
    {
        ResolutionData data = history.start(name, goals, new ResolutionData());

        publish();

        return data;
    }

    public List<BuildEntry<ResolutionData>> getBuilds()
    {
        return history.getEntries();
    }

    public BuildEntry<ResolutionData> getSelectedBuild()
    {
        return history.getSelected();
    }

    public void select(BuildEntry<ResolutionData> entry)
    {
        if (history.select(entry))
        {
            publish();
        }
    }

    public void buildFinished()
    {
        publish();
    }

    public List<ResolutionEvent> getFailures()
    {
        return history.getSelectedData().getFailures();
    }

    public List<ArtifactDownloading> getDownloads()
    {
        return history.getSelectedData().getDownloads();
    }

    public BuildCompleteness getCompleteness()
    {
        return history.getSelectedData().getCompleteness();
    }

    private void publish()
    {
        UiTopics.publish(project, TOPIC, ResolutionListener::resolutionUpdated);
    }

    public interface ResolutionListener
    {
        void resolutionUpdated();
    }
}
