package org.bitstrings.idea.plugins.mavenprime.context;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;

public class BuildContextReimportPlatformTest
    extends BasePlatformTestCase
{
    private static final String DAEMON_HOME = "/opt/maven-mvnd";

    private static final String PROFILE = "release";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        context().reimportQueue.deactivate();

        UIUtil.dispatchAllInvocationEvents();

        context().reimportQueue.cancelAllUpdates();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            RunAll.runAll(
                () -> context().reimportQueue.cancelAllUpdates(),
                () -> context().reimportQueue.activate(),
                () -> context().loadState(new BuildContextProperties()));
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testSetEnvironment_switchingToTheDaemon_doesNotForceAProjectReimport()
    {
        BuildContextEnvironment environment = context().getEnvironment();

        environment.distribution = DistributionSpec.daemonHome(DAEMON_HOME);

        context().setEnvironment(environment);

        assertTrue(
            "the importer only ever sees the IDE's own Maven home, so re-importing every project"
                + " rebuilds the whole model for a change nothing downstream can read",
            context().reimportQueue.isEmpty());
    }

    public void testSetProfile_activatingAProfile_stillForcesAProjectReimport()
    {
        context().setProfile(PROFILE, Boolean.TRUE);

        assertFalse(
            "a profile does change the effective POM, so the model the IDE holds is now stale",
            context().reimportQueue.isEmpty());
    }

    private BuildContext context()
    {
        return BuildContext.getInstance(getProject());
    }
}
