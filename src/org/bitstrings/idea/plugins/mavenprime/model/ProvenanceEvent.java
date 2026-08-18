package org.bitstrings.idea.plugins.mavenprime.model;

public sealed interface ProvenanceEvent
{
    String module();

    String name();

    String value();

    ModelOrigin origin();

    record DependencyOrigin(String module, String name, String value, ModelOrigin origin)
        implements ProvenanceEvent
    {
    }

    record PluginOrigin(String module, String name, String value, ModelOrigin origin)
        implements ProvenanceEvent
    {
    }

    record PropertyOrigin(String module, String name, String value, ModelOrigin origin)
        implements ProvenanceEvent
    {
    }
}
