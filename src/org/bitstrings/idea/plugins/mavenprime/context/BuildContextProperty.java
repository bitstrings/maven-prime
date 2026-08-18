package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public final class BuildContextProperty
{
    public String key = StringUtils.EMPTY;

    public String value = StringUtils.EMPTY;

    public boolean appliesToImport;

    public BuildContextProperty()
    {
    }

    public static BuildContextProperty of(String key, String value, boolean appliesToImport)
    {
        BuildContextProperty property = new BuildContextProperty();

        property.key = StringUtils.defaultString(key);
        property.value = StringUtils.defaultString(value);
        property.appliesToImport = appliesToImport;

        return property;
    }

    public boolean isDefined()
    {
        return StringUtils.isNotBlank(key);
    }

    public BuildContextProperty copy()
    {
        return of(key, value, appliesToImport);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof BuildContextProperty))
        {
            return false;
        }

        BuildContextProperty that = (BuildContextProperty) other;

        return (appliesToImport == that.appliesToImport)
            && Objects.equals(key, that.key)
            && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(key, value, Boolean.valueOf(appliesToImport));
    }

    @Override
    public String toString()
    {
        return key;
    }
}
