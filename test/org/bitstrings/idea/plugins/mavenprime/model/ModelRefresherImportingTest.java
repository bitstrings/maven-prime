package org.bitstrings.idea.plugins.mavenprime.model;

import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;

import com.intellij.maven.testFramework.MavenImportingTestCase;

public class ModelRefresherImportingTest
    extends MavenImportingTestCase
{
    private static final String POM =
        "<groupId>org.bitstrings.test</groupId>"
            + "<artifactId>parent</artifactId>"
            + "<version>1.0</version>";

    // Importing with auto-refresh on starts a real Maven read, which would decide these assertions.
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        MavenPrimeSettings.getInstance(getProject()).autoRefresh = false;

        importProject(POM);
    }

    public void testIsModelUnread_aProjectWhereNothingHasReadTheModel_asksForARead()
    {
        MavenPrimeSettings.getInstance(getProject()).autoRefresh = true;

        assertTrue(
            "the hints have nothing to show until something reads the model once",
            ModelRefresher.getInstance(getProject()).isModelUnread());
    }

    public void testIsModelUnread_aReadThatCameBackWithNoModules_doesNotAskForAnother()
    {
        MavenPrimeSettings.getInstance(getProject()).autoRefresh = true;

        ProvenanceLog.getInstance(getProject()).startBuild(getName());

        assertTrue(
            "the fixture has to leave the read empty",
            ProvenanceLog.getInstance(getProject()).getModules().isEmpty());
        assertFalse(
            "a read that came back with nothing has still happened, and asking again every time a "
                + "hint recomputes runs Maven without end",
            ModelRefresher.getInstance(getProject()).isModelUnread());
    }

    public void testIsModelUnread_autoRefreshTurnedOff_neverAsksForARead()
    {
        assertFalse(ModelRefresher.getInstance(getProject()).isModelUnread());
    }
}
