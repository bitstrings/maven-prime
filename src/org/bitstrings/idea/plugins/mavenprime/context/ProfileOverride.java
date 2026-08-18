package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public final class ProfileOverride
{
    public String name = StringUtils.EMPTY;

    public Boolean enabled;

    public ProfileOverride()
    {
    }

    public static ProfileOverride of(String name, Boolean enabled)
    {
        ProfileOverride override = new ProfileOverride();

        override.name = StringUtils.defaultString(name);
        override.enabled = enabled;

        return override;
    }

    public boolean isDefined()
    {
        return StringUtils.isNotBlank(name);
    }

    public ProfileOverride copy()
    {
        return of(name, enabled);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ProfileOverride))
        {
            return false;
        }

        ProfileOverride that = (ProfileOverride) other;

        return Objects.equals(name, that.name) && Objects.equals(enabled, that.enabled);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, enabled);
    }

    @Override
    public String toString()
    {
        return name + '=' + enabled;
    }
}
