package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public final class PropertyOverride
{
    public String key = StringUtils.EMPTY;

    public String value;

    public Boolean appliesToImport;

    public PropertyOverride()
    {
    }

    public static PropertyOverride of(String key, String value, Boolean appliesToImport)
    {
        PropertyOverride override = new PropertyOverride();

        override.key = StringUtils.defaultString(key);
        override.value = value;
        override.appliesToImport = appliesToImport;

        return override;
    }

    public boolean isDefined()
    {
        return StringUtils.isNotBlank(key) && ((value != null) || (appliesToImport != null));
    }

    public PropertyOverride copy()
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

        if (!(other instanceof PropertyOverride))
        {
            return false;
        }

        PropertyOverride that = (PropertyOverride) other;

        return Objects.equals(key, that.key)
            && Objects.equals(value, that.value)
            && Objects.equals(appliesToImport, that.appliesToImport);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(key, value, appliesToImport);
    }

    @Override
    public String toString()
    {
        return key;
    }
}
