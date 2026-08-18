package org.bitstrings.idea.plugins.mavenprime.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;

import com.intellij.ide.trustedProjects.TrustedProjectsListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;

public class MavenPrimeConfigServicePlatformTest
    extends BasePlatformTestCase
{
    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            deleteConfigFile();
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testSetGoals_noConfigFileYet_createsIt()
    {
        service().setGoals(List.of(GoalDefinition.of("Fast build", "clean install")));

        assertNotNull(service().findConfigFile());
    }

    public void testGetGoals_afterSetGoals_readsThemBackFromDisk()
    {
        service().setGoals(List.of(GoalDefinition.of("Fast build", "clean install")));

        service().invalidate();

        List<GoalDefinition> stored = service().getGoals();

        assertEquals(1, stored.size());
        assertEquals("clean install", stored.get(0).getGoalLine());
    }

    public void testSetGoals_goalCarryingFlagsAndProperties_survivesTheRoundTrip()
    {
        GoalDefinition definition = GoalDefinition.of("Quiet", "verify");

        definition.template.flags.add(MavenFlag.SKIP_TESTS);
        definition.template.properties.put("revision", "1.2.3");

        service().setGoals(List.of(definition));
        service().invalidate();

        MavenPrimeRequest restored = service().getGoals().get(0).template;

        assertTrue(restored.flags.contains(MavenFlag.SKIP_TESTS));
        assertEquals("1.2.3", restored.properties.get("revision"));
    }

    public void testGetDefaults_fileDeclaringDefaults_readsProfilesAndProperties()
        throws IOException
    {
        overwriteConfig(
            "{\"version\":1,\"defaults\":{\"profiles\":{\"ci\":true},\"properties\":{\"revision\":\"1.2.3\"}}}");

        ContextConfig defaults = service().getDefaults();

        assertEquals(Map.of("ci", Boolean.TRUE), defaults.profiles);
        assertEquals(Map.of("revision", "1.2.3"), defaults.properties);
    }

    public void testGetGoals_malformedJson_reportsNoGoalsInsteadOfThrowing()
        throws IOException
    {
        overwriteConfig("{ this is not json");

        assertTrue(service().getGoals().isEmpty());
    }

    public void testGetGoals_goalDeclaringNoGoals_isDropped()
        throws IOException
    {
        overwriteConfig("{\"version\":1,\"goals\":[{\"name\":\"empty\",\"request\":{}}]}");

        assertTrue(service().getGoals().isEmpty());
    }

    public void testGetGoals_flagNameFromANewerVersion_isIgnored()
        throws IOException
    {
        overwriteConfig(
            "{\"version\":1,\"goals\":[{\"request\":{\"goals\":[\"verify\"],"
                + "\"flags\":[\"SKIP_TESTS\",\"FLAG_FROM_THE_FUTURE\"]}}]}");

        assertEquals(1, service().getGoals().get(0).template.flags.size());
    }

    public void testSnapshotOf_anUntrustedProject_readsNoDefaultsFromTheFileOnDisk()
        throws IOException
    {
        overwriteConfig("{\"version\":1,\"defaults\":{\"properties\":{\"revision\":\"1.2.3\"}}}");

        assertNull(
            "a repository nobody trusted yet must not reach the importer through its defaults",
            service().snapshotOf(false).config().defaults);
    }

    public void testSnapshotOf_anUntrustedProject_offersNoneOfItsGoals()
        throws IOException
    {
        overwriteConfig("{\"version\":1,\"goals\":[{\"name\":\"Fast\",\"request\":{\"goals\":[\"verify\"]}}]}");

        assertTrue(
            "a goal a stranger wrote must not appear until the project is trusted",
            service().snapshotOf(false).goals().isEmpty());
    }

    public void testOnProjectTrusted_thisProjectBeingTrusted_announcesTheFileAsChanged()
    {
        List<String> reloads = subscribeToReloads();

        publishTrusted(getProject());

        assertEquals(
            "the build context only re-reads the file when this notification arrives",
            List.of("changed"),
            reloads);
    }

    public void testOnProjectTrusted_someOtherProjectBeingTrusted_leavesThisOneAlone()
    {
        List<String> reloads = subscribeToReloads();

        publishTrusted(ProjectManager.getInstance().getDefaultProject());

        assertEquals(List.of(), reloads);
    }

    private List<String> subscribeToReloads()
    {
        List<String> reloads = new ArrayList<>();

        getProject()
            .getMessageBus()
            .connect(getTestRootDisposable())
            .subscribe(
                MavenPrimeConfigService.TOPIC,
                (MavenPrimeConfigService.ConfigListener) () -> reloads.add("changed"));

        return reloads;
    }

    private static void publishTrusted(Project trusted)
    {
        ApplicationManager
            .getApplication()
            .getMessageBus()
            .syncPublisher(TrustedProjectsListener.TOPIC)
            .onProjectTrusted(trusted);

        UIUtil.dispatchAllInvocationEvents();
    }

    private MavenPrimeConfigService service()
    {
        return MavenPrimeConfigService.getInstance(getProject());
    }

    private void overwriteConfig(String json)
        throws IOException
    {
        TestConfigFile.write(getProject(), json);
    }

    private void deleteConfigFile()
        throws IOException
    {
        TestConfigFile.delete(getProject(), this);
    }
}
