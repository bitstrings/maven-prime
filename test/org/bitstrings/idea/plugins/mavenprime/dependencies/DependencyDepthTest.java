package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DependencyDepthTest
{
    @Test
    public void allows_directDepthAtTheFirstTransitiveLevel_isFalse()
    {
        assertFalse(DependencyDepth.DIRECT.allows(1));
    }

    @Test
    public void allows_directDepthAtTheDeclaredLevel_isTrue()
    {
        assertTrue(DependencyDepth.DIRECT.allows(0));
    }

    @Test
    public void allows_threeLevels_stopsAfterTheThirdLevel()
    {
        assertTrue(DependencyDepth.THREE.allows(2));
        assertFalse(DependencyDepth.THREE.allows(3));
    }

    @Test
    public void allows_anyDepth_neverStops()
    {
        assertTrue(DependencyDepth.ALL.allows(Integer.MAX_VALUE - 1));
    }
}
