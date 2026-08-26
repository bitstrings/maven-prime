package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionKind;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;

public class BuildContextPlatformTest
    extends BasePlatformTestCase
{
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        MavenProjectsManager.getInstance(getProject()).initForTests();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            RunAll.runAll(
                () -> BuildContext.getInstance(getProject()).loadState(new BuildContextProperties()),
                () -> MavenRunner.getInstance(getProject()).getSettings().setSkipTests(false),
                () -> MavenProjectsManager
                    .getInstance(getProject())
                    .getGeneralSettings()
                    .setWorkOffline(false));
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testApplyTo_ideSkipTestsToggleOn_addsTheSkipTestsFlagToTheRequest()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        MavenRunner.getInstance(getProject()).getSettings().setSkipTests(true);

        BuildContext.getInstance(getProject()).applyTo(request);

        assertTrue(
            "the Skip Tests toggle shown in the Maven Prime toolbar must reach the build",
            request.flags.contains(MavenFlag.SKIP_TESTS));
    }

    public void testApplyTo_ideSkipTestsToggleOnAndTheRunSelectsATest_runsThatTestAnyway()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.properties.put(MavenPrimeRequest.TEST_PROPERTY, "CartTest");

        MavenRunner.getInstance(getProject()).getSettings().setSkipTests(true);

        BuildContext.getInstance(getProject()).applyTo(request);

        assertFalse(
            "naming a test and then skipping it runs nothing, and the test tree can only say it found "
                + "nothing",
            request.flags.contains(MavenFlag.SKIP_TESTS));
    }

    public void testApplyTo_ideSkipTestsToggleOnAndTheRunSelectsAnIntegrationTest_runsThatTestAnyway()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.properties.put(MavenPrimeRequest.INTEGRATION_TEST_PROPERTY, "CartIT");

        MavenRunner.getInstance(getProject()).getSettings().setSkipTests(true);

        BuildContext.getInstance(getProject()).applyTo(request);

        assertFalse(request.flags.contains(MavenFlag.SKIP_TESTS));
    }

    public void testApplyTo_ideSkipTestsToggleOff_leavesTheRequestFlagsAlone()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        MavenRunner.getInstance(getProject()).getSettings().setSkipTests(false);

        BuildContext.getInstance(getProject()).applyTo(request);

        assertFalse(request.flags.contains(MavenFlag.SKIP_TESTS));
    }

    public void testApplyTo_ideOfflineToggleOn_addsTheOfflineFlagToTheRequest()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        MavenProjectsManager.getInstance(getProject()).getGeneralSettings().setWorkOffline(true);

        BuildContext.getInstance(getProject()).applyTo(request);

        assertTrue(request.flags.contains(MavenFlag.OFFLINE));
    }

    public void testEffective_ideSkipTestsToggleOn_appliesToASavedGoalTemplate()
    {
        MavenRunner.getInstance(getProject()).getSettings().setSkipTests(true);

        MavenPrimeRequest effective =
            BuildContext.getInstance(getProject()).effective(new MavenPrimeRequest());

        assertTrue(effective.flags.contains(MavenFlag.SKIP_TESTS));
    }

    public void testEffective_forceCleanOnAndALifecyclePhaseRequested_prependsTheCleanPhase()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("install"));

        assertEquals(List.of("clean", "install"), forcingClean().effective(request).goals);
    }

    public void testEffective_forceCleanOnAndCleanAlreadyRequested_doesNotRepeatIt()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("clean", "install"));

        assertEquals(List.of("clean", "install"), forcingClean().effective(request).goals);
    }

    public void testEffective_forceCleanOnAndOnlyAPluginGoalRequested_leavesTheGoalsAlone()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("dependency:tree"));

        assertEquals(
            "wiping target dirs to print a dependency tree only costs the user a rebuild",
            List.of("dependency:tree"),
            forcingClean().effective(request).goals);
    }

    public void testEffective_forceCleanOff_leavesTheGoalsAlone()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("install"));

        assertEquals(List.of("install"), loaded(new BuildContextProperties()).effective(request).goals);
    }

    public void testEffective_forceCleanOn_leavesTheGivenRequestUntouched()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("install"));

        forcingClean().effective(request);

        assertEquals(List.of("install"), request.goals);
    }

    public void testApplyTo_forceCleanOn_leavesGoalsAloneSoBackgroundRefreshesNeverWipeTargetDirs()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("validate"));

        forcingClean().applyTo(request);

        assertEquals(
            "ModelRefresher inherits the context through applyTo and runs unattended",
            List.of("validate"),
            request.goals);
    }

    public void testApplyTo_propertyAbsentFromTheRequest_isFilledFromTheContext()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        contextWith(BuildContextProperty.of("env", "ci", true)).applyTo(request);

        assertEquals("ci", request.properties.get("env"));
    }

    public void testApplyTo_propertyAlreadyOnTheRequest_keepsTheRequestValue()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.properties.put("env", "local");

        contextWith(BuildContextProperty.of("env", "ci", true)).applyTo(request);

        assertEquals("local", request.properties.get("env"));
    }

    public void testApplyTo_propertyExcludedFromTheImport_stillReachesTheBuild()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        contextWith(BuildContextProperty.of("skipTests", "true", false)).applyTo(request);

        assertEquals("true", request.properties.get("skipTests"));
    }

    public void testApplyTo_blankKey_isIgnored()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        contextWith(BuildContextProperty.of("  ", "value", true)).applyTo(request);

        assertTrue(request.properties.isEmpty());
    }

    public void testApplyTo_settingsFileAbsentFromTheRequest_isFilledFromTheContext()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        BuildContextEnvironment environment = new BuildContextEnvironment();

        environment.settingsFile = "/team/settings.xml";

        contextWith(environment).applyTo(request);

        assertEquals("/team/settings.xml", request.settingsFile);
    }

    public void testApplyTo_settingsFileAlreadyOnTheRequest_keepsTheRequestValue()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.settingsFile = "/mine/settings.xml";

        BuildContextEnvironment environment = new BuildContextEnvironment();

        environment.settingsFile = "/team/settings.xml";

        contextWith(environment).applyTo(request);

        assertEquals("/mine/settings.xml", request.settingsFile);
    }

    public void testApplyTo_vmOptionsAbsentFromTheRequest_isFilledFromTheContext()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        BuildContextEnvironment environment = new BuildContextEnvironment();

        environment.vmOptions = "-Xmx2g";

        contextWith(environment).applyTo(request);

        assertEquals("-Xmx2g", request.vmOptions);
    }

    public void testApplyTo_emptyContextEnvironment_leavesTheRequestUnset()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        contextWith(new BuildContextEnvironment()).applyTo(request);

        assertNull(request.settingsFile);
        assertNull(request.jreName);
        assertNull(request.vmOptions);
    }

    public void testGetDistribution_environmentWithDaemonHome_returnsThatDaemonHome()
    {
        BuildContextEnvironment environment = new BuildContextEnvironment();

        environment.distribution = DistributionSpec.daemonHome("/opt/mvnd");

        assertEquals(DistributionSpec.daemonHome("/opt/mvnd"), contextWith(environment).getDistribution());
    }

    public void testGetDistribution_environmentPointingBackAtTheContext_clampsToTheIdeSettings()
    {
        BuildContextEnvironment environment = new BuildContextEnvironment();

        environment.distribution = DistributionSpec.context();

        assertEquals(DistributionKind.IDE, contextWith(environment).getDistribution().kind);
    }

    public void testEffective_aContextProperty_appearsOnTheReturnedRequest()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        MavenPrimeRequest effective =
            contextWith(BuildContextProperty.of("env", "ci", true)).effective(request);

        assertEquals("ci", effective.properties.get("env"));
    }

    public void testEffective_aContextProperty_leavesTheGivenRequestUntouched()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        contextWith(BuildContextProperty.of("env", "ci", true)).effective(request);

        assertTrue(request.properties.isEmpty());
    }

    public void testSetForceClean_toggledFromTheToolbar_tellsTheContextViewsToRedraw()
    {
        List<String> notified = new ArrayList<>();

        getProject()
            .getMessageBus()
            .connect(getTestRootDisposable())
            .subscribe(
                BuildContext.TOPIC,
                (BuildContext.BuildContextListener) () -> notified.add("changed"));

        BuildContext.getInstance(getProject()).setForceClean(true);

        UIUtil.dispatchAllInvocationEvents();

        assertEquals(
            "the status bar summary and the context tab only redraw on this notification",
            List.of("changed"),
            notified);
    }

    public void testAbsorb_anImportThatFinished_tellsTheContextViewsToRedraw()
    {
        List<String> notified = new ArrayList<>();

        getProject()
            .getMessageBus()
            .connect(getTestRootDisposable())
            .subscribe(
                BuildContext.TOPIC,
                (BuildContext.BuildContextListener) () -> notified.add("changed"));

        BuildContext.getInstance(getProject()).absorb();

        UIUtil.dispatchAllInvocationEvents();

        assertEquals(
            "an import is the only thing that changes which profiles exist, so a view opened before "
                + "it keeps listing the ones the modules it added or removed brought with them",
            List.of("changed"),
            notified);
    }

    private BuildContext contextWith(BuildContextProperty property)
    {
        BuildContextProperties state = new BuildContextProperties();

        state.properties.add(
            PropertyOverride.of(
                property.key, property.value, property.appliesToImport ? Boolean.TRUE : null));

        return loaded(state);
    }

    private BuildContext contextWith(BuildContextEnvironment environment)
    {
        BuildContextProperties state = new BuildContextProperties();

        state.environment = new EnvironmentOverride();
        state.environment.distribution =
            environment.rawDistribution().isContext() ? null : environment.getDistribution();
        state.environment.settingsFile = environment.settingsFile;
        state.environment.jreName = environment.jreName;
        state.environment.vmOptions = environment.vmOptions;

        return loaded(state);
    }

    private BuildContext forcingClean()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.forceClean = true;

        return loaded(state);
    }

    private BuildContext loaded(BuildContextProperties state)
    {
        BuildContext context = BuildContext.getInstance(getProject());

        context.loadState(state);

        return context;
    }
}
