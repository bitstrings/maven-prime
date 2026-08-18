package org.bitstrings.idea.plugins.mavenprime.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProvenanceRecordedSessionTest
{
    private static final String REACTOR_POM =
        "/scratchpad/reactor/pom.xml";

    private static final String MANAGED_DEPENDENCY =
        "MODEL_DEPENDENCY\torg.example.spycheck:core\torg.apache.commons:commons-lang3:jar\t3.14.0\t"
            + "org.example.spycheck:reactor:1.0.0\t" + REACTOR_POM + "\t22";

    private static final String INHERITED_PROPERTY =
        "MODEL_PROPERTY\torg.example.spycheck:core\tlang3.version\t3.14.0\t"
            + "org.example.spycheck:reactor:1.0.0\t" + REACTOR_POM + "\t14";

    private static final String OWN_DEPENDENCY =
        "MODEL_DEPENDENCY\torg.example.spycheck:app\torg.example.spycheck:core:jar\t1.0.0\t"
            + "org.example.spycheck:app:1.0.0\t/scratchpad/reactor/app/pom.xml\t20";

    @Test
    public void parse_recordedManagedDependency_resolvesTheVersionTheChildNeverDeclared()
    {
        assertEquals("3.14.0", ProvenanceEvents.parse(MANAGED_DEPENDENCY).orElseThrow().value());
    }

    @Test
    public void parse_recordedManagedDependency_attributesTheVersionToTheParentPom()
    {
        ModelOrigin origin = ProvenanceEvents.parse(MANAGED_DEPENDENCY).orElseThrow().origin();

        assertEquals(
            new ModelOrigin("org.example.spycheck:reactor:1.0.0", REACTOR_POM, 22), origin);
    }

    @Test
    public void parse_recordedManagedDependency_isNotAttributedToTheModuleItself()
    {
        assertFalse(
            ProvenanceEvents
                .parse(MANAGED_DEPENDENCY)
                .orElseThrow()
                .origin()
                .declaredBy("org.example.spycheck:core"));
    }

    @Test
    public void parse_recordedOwnDependency_isAttributedToTheModuleItself()
    {
        assertTrue(
            ProvenanceEvents
                .parse(OWN_DEPENDENCY)
                .orElseThrow()
                .origin()
                .declaredBy("org.example.spycheck:app"));
    }

    @Test
    public void parse_recordedInheritedProperty_pointsAtTheParentLineThatDeclaredIt()
    {
        assertEquals(14, ProvenanceEvents.parse(INHERITED_PROPERTY).orElseThrow().origin().line());
    }

    @Test
    public void parse_recordedInheritedProperty_isNavigable()
    {
        assertTrue(ProvenanceEvents.parse(INHERITED_PROPERTY).orElseThrow().origin().isNavigable());
    }
}
