package org.bitstrings.idea.plugins.mavenprime.util;

import org.apache.commons.lang3.EnumUtils;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;

public final class UiState
{
    private final PropertiesComponent properties;

    private final String prefix;

    public UiState(Project project, String prefix)
    {
        this.properties = PropertiesComponent.getInstance(project);
        this.prefix = prefix;
    }

    public <E extends Enum<E>> E getMode(String key, E fallback)
    {
        return EnumUtils.getEnum(fallback.getDeclaringClass(), properties.getValue(prefix + key), fallback);
    }

    public void setMode(String key, Enum<?> value)
    {
        properties.setValue(prefix + key, value.name());
    }

    public String getText(String key, String fallback)
    {
        return properties.getValue(prefix + key, fallback);
    }

    public void setText(String key, String value, String fallback)
    {
        properties.setValue(prefix + key, value, fallback);
    }

    public int getInt(String key, int fallback)
    {
        return properties.getInt(prefix + key, fallback);
    }

    public void setInt(String key, int value, int fallback)
    {
        properties.setValue(prefix + key, value, fallback);
    }

    public boolean isEnabled(String key, boolean fallback)
    {
        return properties.getBoolean(prefix + key, fallback);
    }

    public void setEnabled(String key, boolean value, boolean fallback)
    {
        properties.setValue(prefix + key, value, fallback);
    }
}
