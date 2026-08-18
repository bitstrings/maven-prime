package org.bitstrings.idea.plugins.mavenprime.context;

import java.io.IOException;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.config.TestConfigFile;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;

import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class SharedBuildContextPlatformTest
    extends BasePlatformTestCase
{
    private static final String TEAM_REVISION =
        "{\"version\":1,\"defaults\":{\"properties\":{\"revision\":\"1.2.3\"}}}";

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            RunAll.runAll(
                () -> BuildContext.getInstance(getProject()).loadState(new BuildContextProperties()),
                () -> MavenPrimeSettings.getInstance(getProject()).sharedContext = true,
                () -> TestConfigFile.delete(getProject(), this));
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testGetProperties_aTeamPropertyAndNoLocalChange_listsTheTeamValue()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        assertEquals(
            "a teammate's committed property has to arrive without anyone pressing Reset",
            "1.2.3",
            context().getProperty("revision"));
    }

    public void testGetProperties_theTeamChangesAValueNobodyTouchedLocally_showsTheNewValue()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().getProperty("revision");

        TestConfigFile.write(
            getProject(), "{\"version\":1,\"defaults\":{\"properties\":{\"revision\":\"2.0.0\"}}}");

        assertEquals("2.0.0", context().getProperty("revision"));
    }

    public void testGetProperties_aTeamPropertyOverriddenLocally_keepsTheLocalValue()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().setProperties(List.of(BuildContextProperty.of("revision", "9.9.9", true)));

        assertEquals("9.9.9", context().getProperty("revision"));
    }

    public void testGetProperties_aLocalOverrideAndThenATeamChange_stillKeepsTheLocalValue()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().setProperties(List.of(BuildContextProperty.of("revision", "9.9.9", true)));

        TestConfigFile.write(
            getProject(), "{\"version\":1,\"defaults\":{\"properties\":{\"revision\":\"2.0.0\"}}}");

        assertEquals(
            "a teammate's commit must never overwrite a value this developer chose",
            "9.9.9",
            context().getProperty("revision"));
    }

    public void testApplyTo_aTeamProperty_reachesTheBuild()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        MavenPrimeRequest request = new MavenPrimeRequest();

        context().applyTo(request);

        assertEquals("1.2.3", request.properties.get("revision"));
    }

    public void testIsForceClean_theTeamTurnedItOn_isOnWithoutAReset()
        throws IOException
    {
        TestConfigFile.write(getProject(), "{\"version\":1,\"defaults\":{\"forceClean\":true}}");

        assertTrue(context().isForceClean());
    }

    public void testIsForceClean_theTeamTurnedItOnAndTheDeveloperTurnedItOff_staysOff()
        throws IOException
    {
        TestConfigFile.write(getProject(), "{\"version\":1,\"defaults\":{\"forceClean\":true}}");

        context().setForceClean(false);

        assertFalse(context().isForceClean());
    }

    public void testGetEnvironment_aTeamVmOptionAndABlankLocalField_takesTheTeamValue()
        throws IOException
    {
        TestConfigFile.write(
            getProject(), "{\"version\":1,\"defaults\":{\"environment\":{\"vmOptions\":\"-Xmx2g\"}}}");

        assertEquals("-Xmx2g", context().getEnvironment().vmOptions);
    }

    public void testGetEnvironment_aTeamJreAndALocalJre_keepsTheLocalJre()
        throws IOException
    {
        TestConfigFile.write(
            getProject(), "{\"version\":1,\"defaults\":{\"environment\":{\"jre\":\"corretto-21\"}}}");

        BuildContextEnvironment mine = context().getEnvironment();

        mine.jreName = "temurin-21";

        context().setEnvironment(mine);

        assertEquals(
            "a JDK name is machine-local, so a shared one can only ever be a fallback",
            "temurin-21",
            context().getEnvironment().jreName);
    }

    public void testPropertyOrigins_aTeamPropertyNobodyTouched_isReportedAsShared()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        assertEquals(ContextOrigin.SHARED, context().propertyOrigins().get("revision"));
    }

    public void testPropertyOrigins_aTeamPropertyOverriddenLocally_isReportedAsOverridden()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().setProperties(List.of(BuildContextProperty.of("revision", "9.9.9", true)));

        assertEquals(ContextOrigin.OVERRIDDEN, context().propertyOrigins().get("revision"));
    }

    public void testPropertyOrigins_aPropertyOnlyThisDeveloperDeclared_carriesNoTeamOrigin()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().setProperties(List.of(BuildContextProperty.of("mine", "yes", true)));

        assertNull(
            "a key the team never declared has no shared origin, so its row reads as local",
            context().propertyOrigins().get("mine"));
    }

    public void testClearOverrides_afterALocalOverride_goesBackToTheTeamValue()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().setProperties(List.of(BuildContextProperty.of("revision", "9.9.9", true)));

        context().clearOverrides();

        assertEquals("1.2.3", context().getProperty("revision"));
    }

    public void testGetProperties_theSharedContextTurnedOff_dropsTheTeamValue()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        MavenPrimeSettings.getInstance(getProject()).sharedContext = false;

        assertNull(context().getProperty("revision"));
    }

    public void testIsForceClean_theSharedContextTurnedOff_ignoresTheTeamValue()
        throws IOException
    {
        TestConfigFile.write(getProject(), "{\"version\":1,\"defaults\":{\"forceClean\":true}}");

        MavenPrimeSettings.getInstance(getProject()).sharedContext = false;

        assertFalse(context().isForceClean());
    }

    public void testGetEnvironment_theSharedContextTurnedOff_ignoresTheTeamEnvironment()
        throws IOException
    {
        TestConfigFile.write(
            getProject(), "{\"version\":1,\"defaults\":{\"environment\":{\"vmOptions\":\"-Xmx2g\"}}}");

        MavenPrimeSettings.getInstance(getProject()).sharedContext = false;

        assertEquals("", context().getEnvironment().vmOptions);
    }

    public void testPropertyOrigins_theSharedContextTurnedOff_reportsEveryRowAsLocal()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        MavenPrimeSettings.getInstance(getProject()).sharedContext = false;

        assertTrue(
            "no row may claim a team origin once the team layer is switched off",
            context().propertyOrigins().isEmpty());
    }

    public void testGetProjectGoals_theSharedContextTurnedOff_stillLoadsThem()
        throws IOException
    {
        TestConfigFile.write(
            getProject(),
            "{\"version\":1,\"defaults\":{\"properties\":{\"revision\":\"1.2.3\"}},"
                + "\"goals\":[{\"name\":\"Fast\",\"request\":{\"goals\":[\"verify\"]}}]}");

        MavenPrimeSettings.getInstance(getProject()).sharedContext = false;

        assertEquals(
            "the switch covers the build context only, so a shared goal list is unaffected",
            1,
            GoalRegistry.getInstance(getProject()).projectScoped().size());
    }

    public void testHasSharedContext_noConfigFileAtAll_isFalseSoResetCannotDestroyTheLocalContext()
    {
        assertFalse(
            "Reset to Defaults restores nothing when nothing is declared, so it must not be offered",
            context().hasSharedContext());
    }

    public void testHasSharedContext_aTeamProperty_isTrue()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        assertTrue(context().hasSharedContext());
    }

    public void testHasSharedContext_aTeamEnvironmentOnly_isTrue()
        throws IOException
    {
        TestConfigFile.write(
            getProject(), "{\"version\":1,\"defaults\":{\"environment\":{\"vmOptions\":\"-Xmx2g\"}}}");

        assertTrue(context().hasSharedContext());
    }

    public void testHasSharedContext_aFileDeclaringGoalsButNoDefaults_isFalse()
        throws IOException
    {
        TestConfigFile.write(
            getProject(),
            "{\"version\":1,\"goals\":[{\"name\":\"Fast\",\"request\":{\"goals\":[\"verify\"]}}]}");

        assertFalse(
            "a goal list is not a build context, so Reset would still restore nothing",
            context().hasSharedContext());
    }

    public void testHasSharedContext_theSharedContextTurnedOff_isFalse()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        MavenPrimeSettings.getInstance(getProject()).sharedContext = false;

        assertFalse(context().hasSharedContext());
    }

    public void testSetProperties_aSnapshotThatOmitsATeamProperty_doesNotSuppressIt()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().setProperties(List.of(BuildContextProperty.of("mine", "yes", true)));

        assertEquals(
            "a panel snapshot taken before the teammate's commit landed is not a deletion",
            "1.2.3",
            context().getProperty("revision"));
    }

    public void testRemoveProperty_aTeamProperty_suppressesItForThisDeveloperOnly()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().removeProperty("revision");

        assertNull(context().getProperty("revision"));
    }

    public void testRemoveProperty_aTeamPropertyAddedBackByHand_stopsBeingSuppressed()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().removeProperty("revision");
        context().setProperties(List.of(BuildContextProperty.of("revision", "1.2.3", true)));

        assertEquals("1.2.3", context().getProperty("revision"));
    }

    public void testRemoveProperty_aPropertyOnlyThisDeveloperDeclared_justDropsIt()
        throws IOException
    {
        TestConfigFile.write(getProject(), TEAM_REVISION);

        context().setProperties(List.of(BuildContextProperty.of("mine", "yes", true)));
        context().removeProperty("mine");

        assertNull(context().getProperty("mine"));
        assertEquals("1.2.3", context().getProperty("revision"));
    }

    private BuildContext context()
    {
        return BuildContext.getInstance(getProject());
    }
}
