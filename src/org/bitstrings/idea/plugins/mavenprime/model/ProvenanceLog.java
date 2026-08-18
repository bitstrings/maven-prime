package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PluginOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PropertyOrigin;
import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

@Service(Service.Level.PROJECT)
public final class ProvenanceLog
{
    public static final Topic<ProvenanceListener> TOPIC =
        Topic.create("Maven Prime model provenance", ProvenanceListener.class);

    private final Project project;

    private final BuildHistory<ProvenanceData> history = new BuildHistory<>(ProvenanceData::new);

    public ProvenanceLog(Project project)
    {
        this.project = project;
    }

    public static ProvenanceLog getInstance(Project project)
    {
        return project.getService(ProvenanceLog.class);
    }

    public ProvenanceData startBuild(String name)
    {
        ProvenanceData data = history.start(name, new ProvenanceData());

        publish();

        return data;
    }

    public List<BuildEntry<ProvenanceData>> getBuilds()
    {
        return history.getEntries();
    }

    public BuildEntry<ProvenanceData> getSelectedBuild()
    {
        return history.getSelected();
    }

    public void select(BuildEntry<ProvenanceData> entry)
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

    public BuildCompleteness getCompleteness()
    {
        return history.getSelectedData().getCompleteness();
    }

    public List<DependencyOrigin> getDependencies(String module)
    {
        return history.getSelectedData().getDependencies(module);
    }

    public List<PluginOrigin> getPlugins(String module)
    {
        return history.getSelectedData().getPlugins(module);
    }

    public List<PropertyOrigin> getProperties(String module)
    {
        return history.getSelectedData().getProperties(module);
    }

    public List<String> getModules()
    {
        return history.getSelectedData().getModules();
    }

    private void publish()
    {
        UiTopics.publish(project, TOPIC, ProvenanceListener::provenanceUpdated);
    }

    public interface ProvenanceListener
    {
        void provenanceUpdated();
    }
}
