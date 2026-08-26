package org.bitstrings.idea.plugins.mavenprime.settings;

import org.apache.commons.lang3.StringUtils;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;

@Service(Service.Level.PROJECT)
@State(name = "MavenPrimeSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class MavenPrimeSettings
    implements PersistentStateComponent<MavenPrimeSettings>
{
    public static final Topic<SettingsListener> TOPIC =
        Topic.create("Maven Prime settings", SettingsListener.class);

    public boolean autoRefresh = true;

    public boolean colorConsole = true;

    public boolean sharedContext = true;

    public boolean searchEverywhere;

    public String importerVmOptions = StringUtils.EMPTY;

    public interface SettingsListener
    {
        void settingsChanged();
    }

    public static MavenPrimeSettings getInstance(Project project)
    {
        return project.getService(MavenPrimeSettings.class);
    }

    @Override
    public MavenPrimeSettings getState()
    {
        return this;
    }

    @Override
    public void loadState(MavenPrimeSettings state)
    {
        XmlSerializerUtil.copyBean(state, this);
    }
}
