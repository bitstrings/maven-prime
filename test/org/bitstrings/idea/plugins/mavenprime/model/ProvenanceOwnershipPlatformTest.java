package org.bitstrings.idea.plugins.mavenprime.model;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ProvenanceOwnershipPlatformTest
    extends BasePlatformTestCase
{
    public void testStartBuild_secondBuildStarted_leavesTheFirstBuildsDataIntact()
    {
        ProvenanceLog log = ProvenanceLog.getInstance(getProject());

        ProvenanceData running = log.startBuild("clean install", "");

        running.accept(origin("core", "commons-io"));

        log.startBuild("clean verify", "");

        assertEquals(
            "a later build must not erase a running build's provenance",
            1,
            running.getDependencies("core").size());
    }

    public void testStartBuild_secondBuildStarted_eventsOfTheFirstKeepLandingInItsOwnData()
    {
        ProvenanceLog log = ProvenanceLog.getInstance(getProject());

        ProvenanceData running = log.startBuild("clean install", "");
        ProvenanceData later = log.startBuild("clean verify", "");

        running.accept(origin("core", "commons-io"));

        assertEquals(0, later.getDependencies("core").size());
    }

    public void testGetDependencies_afterASecondBuildStarted_readsTheNewestData()
    {
        ProvenanceLog log = ProvenanceLog.getInstance(getProject());

        log.startBuild("clean install", "");

        ProvenanceData newest = log.startBuild("clean verify", "");

        newest.accept(origin("core", "commons-io"));

        assertEquals(1, log.getDependencies("core").size());
    }

    private static DependencyOrigin origin(String module, String name)
    {
        return new DependencyOrigin(module, name, "1.0", ModelOrigin.UNKNOWN);
    }
}
