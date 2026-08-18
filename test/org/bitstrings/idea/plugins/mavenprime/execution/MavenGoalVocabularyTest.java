package org.bitstrings.idea.plugins.mavenprime.execution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class MavenGoalVocabularyTest
{
    @Test
    public void hasPhase_aLifecyclePhase_isTrue()
    {
        assertTrue(MavenGoalVocabulary.hasPhase(List.of("install")));
    }

    @Test
    public void hasPhase_aPhaseBeyondTheEightCommonOnes_isStillRecognized()
    {
        assertTrue(MavenGoalVocabulary.hasPhase(List.of("pre-integration-test")));
    }

    @Test
    public void hasPhase_onlyPluginGoals_isFalse()
    {
        assertFalse(MavenGoalVocabulary.hasPhase(List.of("dependency:tree", "help:effective-pom")));
    }

    @Test
    public void hasPhase_aPluginGoalFollowedByAPhase_isTrue()
    {
        assertTrue(MavenGoalVocabulary.hasPhase(List.of("dependency:purge-local-repository", "install")));
    }

    @Test
    public void hasPhase_onlyCommandLineOptions_isFalse()
    {
        assertFalse(MavenGoalVocabulary.hasPhase(List.of("-X", "-DskipTests")));
    }

    @Test
    public void hasPhase_noGoals_isFalse()
    {
        assertFalse(MavenGoalVocabulary.hasPhase(List.of()));
    }
}
