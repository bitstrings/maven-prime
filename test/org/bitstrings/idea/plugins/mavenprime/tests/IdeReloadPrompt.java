package org.bitstrings.idea.plugins.mavenprime.tests;

import java.util.Set;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.project.Project;

public final class IdeReloadPrompt
{
    private static final String PATH = "/tmp/mavenprime-pending-reload";

    private IdeReloadPrompt()
    {
    }

    public static void raise(Project project)
    {
        ExternalSystemProjectNotificationAware.getInstance(project).notificationNotify(new Pending());
    }

    public static void clear(Project project)
    {
        ExternalSystemProjectNotificationAware.getInstance(project).notificationExpire();
    }

    private static final class Pending
        implements ExternalSystemProjectAware
    {
        @Override
        public ExternalSystemProjectId getProjectId()
        {
            return new ExternalSystemProjectId(new ProjectSystemId("MAVEN"), PATH);
        }

        @Override
        public Set<String> getSettingsFiles()
        {
            return Set.of();
        }

        @Override
        public void subscribe(ExternalSystemProjectListener listener, Disposable parent)
        {
        }

        @Override
        public void reloadProject(ExternalSystemProjectReloadContext context)
        {
        }
    }
}
