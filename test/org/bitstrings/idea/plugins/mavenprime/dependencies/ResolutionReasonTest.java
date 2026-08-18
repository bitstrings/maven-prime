package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.artifact;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.junit.Test;

public class ResolutionReasonTest
{
    @Test
    public void of_addedWithoutManagement_isIncluded()
    {
        assertEquals(ResolutionReason.INCLUDED, ResolutionReason.of(node(MavenArtifactState.ADDED, null)));
    }

    @Test
    public void of_addedWithPremanagedVersion_isManaged()
    {
        assertEquals(ResolutionReason.MANAGED, ResolutionReason.of(node(MavenArtifactState.ADDED, "1.0")));
    }

    @Test
    public void of_conflictState_isConflictLoser()
    {
        assertEquals(ResolutionReason.CONFLICT_LOSER, ResolutionReason.of(node(MavenArtifactState.CONFLICT, null)));
    }

    @Test
    public void of_duplicateState_isDuplicate()
    {
        assertEquals(ResolutionReason.DUPLICATE, ResolutionReason.of(node(MavenArtifactState.DUPLICATE, null)));
    }

    @Test
    public void of_excludedState_isExcluded()
    {
        assertEquals(ResolutionReason.EXCLUDED, ResolutionReason.of(node(MavenArtifactState.EXCLUDED, null)));
    }

    @Test
    public void isOmitted_included_isFalse()
    {
        assertFalse(ResolutionReason.INCLUDED.isOmitted());
    }

    @Test
    public void isOmitted_managed_isFalse()
    {
        assertFalse(ResolutionReason.MANAGED.isOmitted());
    }

    @Test
    public void isOmitted_conflictLoser_isTrue()
    {
        assertTrue(ResolutionReason.CONFLICT_LOSER.isOmitted());
    }

    @Test
    public void transition_managedNode_runsFromTheDeclaredToTheEffectiveVersion()
    {
        VersionTransition transition =
            ResolutionReason.MANAGED.transition(node(MavenArtifactState.ADDED, "1.0"));

        assertEquals(new VersionTransition("1.0", "2.0"), transition);
    }

    @Test
    public void transition_conflictLoser_runsFromThisVersionToTheWinner()
    {
        MavenArtifactNode loser =
            new MavenArtifactNode(
                null,
                artifact("com.lib", "shared", "2.0"),
                MavenArtifactState.CONFLICT,
                artifact("com.lib", "shared", "3.0"),
                "compile",
                null,
                null);

        assertEquals(new VersionTransition("2.0", "3.0"), ResolutionReason.CONFLICT_LOSER.transition(loser));
    }

    @Test
    public void transition_conflictLoserWithoutAKnownWinner_isNull()
    {
        assertNull(ResolutionReason.CONFLICT_LOSER.transition(node(MavenArtifactState.CONFLICT, null)));
    }

    @Test
    public void transition_included_isNull()
    {
        assertNull(ResolutionReason.INCLUDED.transition(node(MavenArtifactState.ADDED, null)));
    }

    @Test
    public void transition_managedToTheSameVersionItAlreadyDeclared_isNullSoNoUselessArrowIsShown()
    {
        assertNull(ResolutionReason.MANAGED.transition(node(MavenArtifactState.ADDED, "2.0")));
    }

    @Test
    public void transition_conflictLoserAgainstItsOwnVersion_isNullSoNoUselessArrowIsShown()
    {
        MavenArtifactNode sameVersion =
            new MavenArtifactNode(
                null,
                artifact("com.lib", "shared", "2.0"),
                MavenArtifactState.CONFLICT,
                artifact("com.lib", "shared", "2.0"),
                "compile",
                null,
                null);

        assertNull(ResolutionReason.CONFLICT_LOSER.transition(sameVersion));
    }

    @Test
    public void marker_included_isNull()
    {
        assertNull(ResolutionReason.INCLUDED.marker());
    }

    @Test
    public void marker_conflictLoser_isNullBecauseTheArrowCarriesIt()
    {
        assertNull(ResolutionReason.CONFLICT_LOSER.marker());
    }

    private static MavenArtifactNode node(MavenArtifactState state, String premanagedVersion)
    {
        return new MavenArtifactNode(
            null, artifact("com.lib", "shared", "2.0"), state, null, "compile", premanagedVersion, null);
    }
}
