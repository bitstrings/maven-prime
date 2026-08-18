package org.bitstrings.idea.plugins.mavenprime.ui;

import org.apache.commons.lang3.StringUtils;

public final class PropertyEntry
{
    static final String VALUE_SEPARATOR = " = ";

    private String key;

    private String value;

    public PropertyEntry(String key, String value)
    {
        this.key = StringUtils.defaultString(key);
        this.value = StringUtils.defaultString(value);
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = StringUtils.defaultString(key);
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = StringUtils.defaultString(value);
    }

    @Override
    public String toString()
    {
        return StringUtils.isBlank(value) ? key : (key + VALUE_SEPARATOR + value);
    }
}
