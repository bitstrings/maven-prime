package org.bitstrings.idea.plugins.mavenprime.settings;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceLog;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionLog;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class RetainedBuildsPlatformTest
    extends BasePlatformTestCase
{
    private static final int KEEP = 2;

    private static final int RECORDED = 5;

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            MavenPrimeSettings.getInstance(getProject()).retainedBuilds = BuildHistory.DEFAULT_RETAINED;
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testStartBuild_aLoweredRetention_trimsTheProfilerHistory()
    {
        BuildProfileService profiles = BuildProfileService.getInstance(getProject());

        assertEquals(KEEP, retainedAfter(name -> profiles.startBuild(name, ""), () -> profiles.getBuilds().size()));
    }

    public void testStartBuild_aLoweredRetention_trimsTheModelHistory()
    {
        ProvenanceLog provenances = ProvenanceLog.getInstance(getProject());

        assertEquals(
            KEEP,
            retainedAfter(name -> provenances.startBuild(name, ""), () -> provenances.getBuilds().size()));
    }

    public void testStartBuild_aLoweredRetention_trimsTheRepositoryHistory()
    {
        ResolutionLog resolutions = ResolutionLog.getInstance(getProject());

        assertEquals(
            KEEP,
            retainedAfter(name -> resolutions.startBuild(name, ""), () -> resolutions.getBuilds().size()));
    }

    // A history handed a fixed limit instead of the setting passes every test that only exercises
    // BuildHistory, so the count has to be read back through the service that owns it.
    private int retainedAfter(Consumer<String> startBuild, IntSupplier count)
    {
        MavenPrimeSettings.getInstance(getProject()).retainedBuilds = KEEP;

        for (int index = 0; index < RECORDED; index++)
        {
            startBuild.accept("build" + index);
        }

        return count.getAsInt();
    }
}
