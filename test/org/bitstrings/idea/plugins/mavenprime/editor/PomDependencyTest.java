package org.bitstrings.idea.plugins.mavenprime.editor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PomDependencyTest
{
    private static final PomDependency GUAVA = new PomDependency(null, "com.google.guava", "guava");

    @Test
    public void matches_theManagementKeyOfTheSameArtifact_isTrue()
    {
        assertTrue(GUAVA.matches("com.google.guava:guava:jar"));
    }

    @Test
    public void matches_theSameArtifactWithoutAType_isTrue()
    {
        assertTrue(GUAVA.matches("com.google.guava:guava"));
    }

    @Test
    public void matches_anArtifactIdThatOnlySharesThePrefix_isFalse()
    {
        assertFalse(GUAVA.matches("com.google.guava:guava-testlib:jar"));
    }

    @Test
    public void matches_null_isFalse()
    {
        assertFalse(GUAVA.matches(null));
    }
}
