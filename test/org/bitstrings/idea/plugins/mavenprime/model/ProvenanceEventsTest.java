package org.bitstrings.idea.plugins.mavenprime.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PluginOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PropertyOrigin;
import org.bitstrings.idea.plugins.mavenprime.spy.SpyProtocol;
import org.junit.Test;

public class ProvenanceEventsTest
{
    @Test
    public void parse_modelDependency_readsTheDeclaringModelAndLine()
    {
        ProvenanceEvent event =
            ProvenanceEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MODEL_DEPENDENCY,
                        "org.example:core",
                        "org.apache.commons:commons-lang3:jar",
                        "3.14.0",
                        "org.example:parent:1.0.0",
                        "/repo/parent/pom.xml",
                        "22"))
                .orElseThrow();

        assertEquals(
            new DependencyOrigin(
                "org.example:core",
                "org.apache.commons:commons-lang3:jar",
                "3.14.0",
                new ModelOrigin("org.example:parent:1.0.0", "/repo/parent/pom.xml", 22)),
            event);
    }

    @Test
    public void parse_modelPlugin_readsTheDeclaringModelAndLine()
    {
        ProvenanceEvent event =
            ProvenanceEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MODEL_PLUGIN,
                        "org.example:core",
                        "org.apache.maven.plugins:maven-surefire-plugin",
                        "3.2.5",
                        "org.example:parent:1.0.0",
                        "/repo/parent/pom.xml",
                        "31"))
                .orElseThrow();

        assertEquals(
            new PluginOrigin(
                "org.example:core",
                "org.apache.maven.plugins:maven-surefire-plugin",
                "3.2.5",
                new ModelOrigin("org.example:parent:1.0.0", "/repo/parent/pom.xml", 31)),
            event);
    }

    @Test
    public void parse_modelProperty_readsTheDeclaringModelAndLine()
    {
        ProvenanceEvent event =
            ProvenanceEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MODEL_PROPERTY,
                        "org.example:core",
                        "lang3.version",
                        "3.14.0",
                        "org.example:parent:1.0.0",
                        "/repo/parent/pom.xml",
                        "14"))
                .orElseThrow();

        assertEquals(
            new PropertyOrigin(
                "org.example:core",
                "lang3.version",
                "3.14.0",
                new ModelOrigin("org.example:parent:1.0.0", "/repo/parent/pom.xml", 14)),
            event);
    }

    @Test
    public void parse_entryWithoutAFile_recordsAnUnknownOrigin()
    {
        ProvenanceEvent event =
            ProvenanceEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MODEL_DEPENDENCY, "org.example:core", "g:a:jar", "1.0", "", "", "0"))
                .orElseThrow();

        assertFalse(event.origin().isKnown());
    }

    @Test
    public void parse_entryWithoutAName_isIgnored()
    {
        assertTrue(
            ProvenanceEvents
                .parse(
                    SpyProtocol.encode(
                        SpyProtocol.MODEL_PROPERTY, "org.example:core", "", "value", "m", "/pom.xml", "3"))
                .isEmpty());
    }

    @Test
    public void parse_buildTreeEvent_carriesNoProvenanceOfItsOwn()
    {
        assertTrue(
            ProvenanceEvents
                .parse(SpyProtocol.encode(SpyProtocol.PROJECT_STARTED, "org.example:core", "Core"))
                .isEmpty());
    }

    @Test
    public void parse_emptyLine_isIgnored()
    {
        assertTrue(ProvenanceEvents.parse("").isEmpty());
    }
}
