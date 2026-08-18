package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@Service(Service.Level.APP)
@State(name = "MavenPrimeDaemonInstallations", storages = @Storage("mavenPrimeDaemons.xml"))
public final class DaemonInstallations
    implements PersistentStateComponent<DaemonInstallations>
{
    public volatile List<String> homes = new ArrayList<>();

    public static DaemonInstallations getInstance()
    {
        return ApplicationManager.getApplication().getService(DaemonInstallations.class);
    }

    public List<String> getHomes()
    {
        return homes;
    }

    public void setHomes(List<String> values)
    {
        homes = new ArrayList<>(values);
    }

    public void adopt(List<String> migrated)
    {
        if (homes.isEmpty() && !migrated.isEmpty())
        {
            setHomes(migrated);
        }
    }

    @Override
    public DaemonInstallations getState()
    {
        return this;
    }

    @Override
    public void loadState(DaemonInstallations state)
    {
        XmlSerializerUtil.copyBean(state, this);
    }
}
