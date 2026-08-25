package org.bitstrings.idea.plugins.mavenprime.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
@State(name = "MavenPrimeProjectConfig", storages = @Storage(ProjectConfigStore.FILE_NAME))
public final class ProjectConfigStore
    implements PersistentStateComponent<MavenPrimeConfig>
{
    public static final String FILE_NAME = "mavenPrime.xml";

    private volatile MavenPrimeConfig config = new MavenPrimeConfig();

    public static ProjectConfigStore getInstance(Project project)
    {
        return project.getService(ProjectConfigStore.class);
    }

    @Override
    public MavenPrimeConfig getState()
    {
        return config;
    }

    @Override
    public void loadState(MavenPrimeConfig loaded)
    {
        config = loaded;
    }

    public void set(MavenPrimeConfig updated)
    {
        config = updated;
    }
}
