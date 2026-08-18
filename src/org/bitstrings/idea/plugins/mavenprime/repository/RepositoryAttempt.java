package org.bitstrings.idea.plugins.mavenprime.repository;

import org.apache.commons.lang3.StringUtils;

public record RepositoryAttempt(String repository, long lastUpdatedMillis, String error, boolean recorded)
{
    public boolean failed()
    {
        return recorded;
    }

    public boolean notFound()
    {
        return recorded && StringUtils.isBlank(error);
    }
}
