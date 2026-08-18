package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PluginOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PropertyOrigin;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;

public final class ProvenanceEvents
{
    private static final int TYPE = 0;

    private static final int MODULE = 1;

    private static final int NAME = 2;

    private static final int VALUE = 3;

    private static final int MODEL_ID = 4;

    private static final int FILE = 5;

    private static final int LINE = 6;

    private ProvenanceEvents()
    {
    }

    public static Optional<ProvenanceEvent> parse(String line)
    {
        String[] fields = SpyProtocol.decode(line);

        if (fields.length == 0)
        {
            return Optional.empty();
        }

        String module = field(fields, MODULE);
        String name = field(fields, NAME);

        if (StringUtils.isBlank(module) || StringUtils.isBlank(name))
        {
            return Optional.empty();
        }

        return switch (fields[TYPE])
        {
            case SpyProtocol.MODEL_DEPENDENCY ->
                Optional.of(new DependencyOrigin(module, name, field(fields, VALUE), origin(fields)));
            case SpyProtocol.MODEL_PLUGIN ->
                Optional.of(new PluginOrigin(module, name, field(fields, VALUE), origin(fields)));
            case SpyProtocol.MODEL_PROPERTY ->
                Optional.of(new PropertyOrigin(module, name, field(fields, VALUE), origin(fields)));
            default -> Optional.empty();
        };
    }

    private static ModelOrigin origin(String[] fields)
    {
        String file = field(fields, FILE);

        return StringUtils.isBlank(file)
            ? ModelOrigin.UNKNOWN
            : new ModelOrigin(
                field(fields, MODEL_ID), file, NumberUtils.toInt(field(fields, LINE), 0));
    }

    private static String field(String[] fields, int index)
    {
        return (index < fields.length) ? fields[index] : StringUtils.EMPTY;
    }
}
