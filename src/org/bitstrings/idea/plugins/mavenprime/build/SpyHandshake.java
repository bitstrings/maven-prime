package org.bitstrings.idea.plugins.mavenprime.build;

import org.apache.commons.lang3.StringUtils;

public record SpyHandshake(int port, String token, String buildName)
{
    public static final SpyHandshake NONE = new SpyHandshake(0, StringUtils.EMPTY, StringUtils.EMPTY);

    public boolean isUsable()
    {
        return port > 0;
    }
}
