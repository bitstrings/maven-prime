package org.bitstrings.idea.plugins.mavenprime.repository;

import org.apache.commons.lang3.StringUtils;

public record RepositoryCredentials(String username, String password, Source source)
{
    public enum Source
    {
        SETTINGS_XML,
        PASSWORD_SAFE,
        ENCRYPTED,
        NONE
    }

    public static RepositoryCredentials none()
    {
        return new RepositoryCredentials(null, null, Source.NONE);
    }

    public static RepositoryCredentials encrypted()
    {
        return new RepositoryCredentials(null, null, Source.ENCRYPTED);
    }

    public boolean isUsable()
    {
        return StringUtils.isNotBlank(username) && (password != null);
    }

    @Override
    public String toString()
    {
        return "RepositoryCredentials[source=" + source + ", username=" + StringUtils.defaultString(username) + ']';
    }
}
