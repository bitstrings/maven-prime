package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

import java.util.Collection;
import java.util.Set;

import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

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

        MavenProjectsManager.getInstance(getProject())
            .setExplicitProfiles(new MavenExplicitProfiles(Set.of(), Set.of()));

        context().reimportQueue.deactivate();

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

    public void testSetProperties_editingAProperty_doesNotForceAProjectReimport()
    {
        context().setProperties(List.of(BuildContextProperty.of("revision", "1.2.3", true)));

        assertTrue(
            "every forced reimport lets the IDE re-detect the compiler and overwrite the one the user "
                + "chose, and a property edit changes no POM",
            context().reimportQueue.isEmpty());
    }

    public void testSetProfile_activatingAProfile_pushesItToTheIde()
    {
        context().setProfile(PROFILE, Boolean.TRUE);

        assertTrue(
            "a profile the IDE never hears about leaves it resolving the model the user just changed",
            explicitProfiles().contains(PROFILE));
    }

    public void testSetProfile_aProfileThatCanAddAModule_asksForThePomsToBeFoundAgain()
    {
        context().setProfile(PROFILE, Boolean.TRUE);

        assertFalse(
            "a profile can add a module, and only a forced update looks for POMs the IDE has not "
                + "seen yet, so without it the new module and its own profiles never appear",
            context().reimportQueue.isEmpty());
    }

    private BuildContext context()
    {
        return BuildContext.getInstance(getProject());
    }

    private Collection<String> explicitProfiles()
    {
        return MavenProjectsManager.getInstance(getProject()).getExplicitProfiles().getEnabledProfiles();
    }
}
