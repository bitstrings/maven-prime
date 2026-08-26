package org.bitstrings.idea.plugins.mavenprime.model;

import org.bitstrings.idea.plugins.mavenprime.util.MavenImports;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;

@Service(Service.Level.PROJECT)
public final class ModelStaleness
    implements ProvenanceLog.ProvenanceReadListener
{
    public static final Topic<StalenessListener> TOPIC =
        Topic.create("Maven Prime model staleness", StalenessListener.class);

    private final Project project;

    private volatile boolean readOwed;

    private volatile boolean published;

    public ModelStaleness(Project project)
    {
        this.project = project;
    }

    public static ModelStaleness getInstance(Project project)
    {
        return project.getService(ModelStaleness.class);
    }

    public void listen()
    {
        MessageBusConnection connection = project.getMessageBus().connect(project);

        connection.subscribe(ProvenanceLog.READ_TOPIC, this);
        connection.subscribe(
            ExternalSystemProjectNotificationAware.TOPIC,
            new ExternalSystemProjectNotificationAware.Listener()
            {
                @Override
                public void onNotificationChanged(Project changed)
                {
                    republishOnChange();
                }
            });

        MavenImports.onImported(project, project, this::pomSourcesChanged);
    }

    // Loading the IDE's pending changes clears its prompt without re-reading the effective model, so
    // dropping either half leaves a window where the views claim a model that no longer matches.
    public boolean isStale()
    {
        return readOwed
            || ExternalSystemProjectNotificationAware.getInstance(project).isNotificationVisible();
    }

    public void pomSourcesChanged()
    {
        readOwed = true;

        republishOnChange();
    }

    @Override
    public void provenanceRead()
    {
        readOwed = false;

        republishOnChange();
    }

    private void republishOnChange()
    {
        boolean stale = isStale();

        if (stale == published)
        {
            return;
        }

        published = stale;

        UiTopics.publish(project, TOPIC, StalenessListener::modelStalenessChanged);
    }

    public interface StalenessListener
    {
        void modelStalenessChanged();
    }
}
