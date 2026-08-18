package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.dependencies.ParentPomSearch.Found;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.junit.Test;

import com.intellij.util.Processor;

public class ParentPomSearchTest
{
    @Test
    public void stoppingAt_theAncestorThatDeclaresIt_stopsTheWalkThere()
    {
        Found<String> found = new Found<>();

        Processor<MavenDomProjectModel> walk = ParentPomSearch.stoppingAt(found, parent -> "declared");

        assertTrue(
            "processParentProjects reads true as stop, so a hit must return true",
            walk.process(null));
    }

    @Test
    public void stoppingAt_anAncestorThatDeclaresNothing_keepsWalkingUpwards()
    {
        Found<String> found = new Found<>();

        Processor<MavenDomProjectModel> walk = ParentPomSearch.stoppingAt(found, parent -> null);

        assertFalse(
            "a grandparent declaration is only reachable if a miss keeps the walk going",
            walk.process(null));
    }

    @Test
    public void stoppingAt_theAncestorThatDeclaresIt_keepsWhatItFound()
    {
        Found<String> found = new Found<>();

        ParentPomSearch.stoppingAt(found, parent -> "declared").process(null);

        assertEquals("declared", found.value);
    }

    @Test
    public void stoppingAt_aHitFollowedByNoFurtherCall_neverOverwritesTheMatch()
    {
        Found<String> found = new Found<>();

        List<String> ancestry = new ArrayList<>(List.of("parent", "grandparent"));

        Processor<MavenDomProjectModel> walk =
            ParentPomSearch.stoppingAt(found, parent -> "parent".equals(ancestry.remove(0)) ? "hit" : null);

        walk.process(null);

        assertEquals(
            "the earlier walk continued past a hit and nulled it from the grandparent",
            "hit",
            found.value);
    }
}
