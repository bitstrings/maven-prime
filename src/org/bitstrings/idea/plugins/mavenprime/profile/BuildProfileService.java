package org.bitstrings.idea.plugins.mavenprime.profile;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

@Service(Service.Level.PROJECT)
public final class BuildProfileService
{
    public static final Topic<BuildProfileListener> TOPIC =
        Topic.create("Maven Prime build profile", BuildProfileListener.class);

    private final Project project;

    private final BuildHistory<BuildProfile> history = new BuildHistory<>(BuildProfile::new);

    public BuildProfileService(Project project)
    {
        this.project = project;
    }

    public static BuildProfileService getInstance(Project project)
    {
        return project.getService(BuildProfileService.class);
    }

    public BuildProfile startBuild(String name)
    {
        BuildProfile profile = history.start(name, new BuildProfile());

        publish();

        return profile;
    }

    public List<BuildEntry<BuildProfile>> getBuilds()
    {
        return history.getEntries();
    }

    public BuildEntry<BuildProfile> getSelectedBuild()
    {
        return history.getSelected();
    }

    public void select(BuildEntry<BuildProfile> entry)
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

    public BuildProfile getCurrent()
    {
        return history.getSelectedData();
    }

    private void publish()
    {
        UiTopics.publish(project, TOPIC, BuildProfileListener::profileUpdated);
    }

    public interface BuildProfileListener
    {
        void profileUpdated();
    }
}
