package org.bitstrings.idea.plugins.mavenprime.editor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bitstrings.idea.plugins.mavenprime.model.ModelOrigin;

public final class ModelOriginPayloads
{
    private static final char SEPARATOR = '|';

    private ModelOriginPayloads()
    {
    }

    public static String encode(ModelOrigin origin)
    {
        return origin.file() + SEPARATOR + origin.line();
    }

    public static ModelOrigin decode(String payload)
    {
        int separator = StringUtils.lastIndexOf(payload, SEPARATOR);

        if (separator < 0)
        {
            return ModelOrigin.UNKNOWN;
        }

        return new ModelOrigin(
            StringUtils.EMPTY,
            payload.substring(0, separator),
            NumberUtils.toInt(payload.substring(separator + 1), 0));
    }
}
