package org.bitstrings.idea.plugins.mavenprime.run;

import com.intellij.build.BuildViewManager;
import com.intellij.build.progress.BuildProgress;
import com.intellij.build.progress.BuildProgressDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenBuildProgressPlatformTest
    extends BasePlatformTestCase
{
    public void testStart_aDescriptorBuiltForAMavenPrimeRun_startsTheBuildProgress()
    {
        BuildProgress<BuildProgressDescriptor> progress =
            BuildViewManager.createBuildProgress(getProject());

        progress.start(
            MavenPrimeRunProfileState.descriptorFor(new Object(), "clean install", getProject().getBasePath()));

        assertNotNull("a started build progress must have an id", progress.getId());
    }

    public void testGetId_aBuildProgressThatWasNeverStarted_cannotSupplyABuildId()
    {
        BuildProgress<BuildProgressDescriptor> progress =
            BuildViewManager.createBuildProgress(getProject());

        boolean refused;

        try
        {
            progress.getId();

            refused = false;
        }
        catch (RuntimeException | AssertionError expected)
        {
            refused = true;
        }

        assertTrue(
            "a progress with no descriptor must not hand out an id; Maven Prime relied on this and broke",
            refused);
    }
}
