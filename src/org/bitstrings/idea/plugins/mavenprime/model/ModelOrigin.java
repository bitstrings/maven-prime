package org.bitstrings.idea.plugins.mavenprime.model;

import org.apache.commons.lang3.StringUtils;

public record ModelOrigin(String modelId, String file, int line)
{
    public static final ModelOrigin UNKNOWN = new ModelOrigin(StringUtils.EMPTY, StringUtils.EMPTY, 0);

    public boolean isKnown()
    {
        return StringUtils.isNotBlank(file);
    }

    public boolean isNavigable()
    {
        return isKnown() && (line > 0);
    }

    public boolean declaredBy(String module)
    {
        return (modelId != null) && modelId.startsWith(module + ':');
    }
}
