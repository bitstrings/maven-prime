package org.bitstrings.idea.plugins.mavenprime.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;
import org.junit.Test;

public class ProfileGroupsTest
{
    private static final String CORE = "org.example:core";

    private static final String APP = "org.example:app";

    private static final String SUREFIRE = "maven-surefire-plugin:test";

    private static final String COMPILE = "maven-compiler-plugin:compile";

    @Test
    public void of_groupedByModule_reportsTheModulesOwnSpanRatherThanTheSumOfItsGoals()
    {
        assertEquals(
            "a module's span covers the resolution it did before any goal ran, and the timeline draws "
                + "that span, so summing the goals would make the two views disagree",
            500L,
            group(build(), ProfileGrouping.MODULE, CORE).durationMillis());
    }

    @Test
    public void of_groupedByPlugin_addsThatPluginUpAcrossEveryModule()
    {
        assertEquals(
            300L, group(build(), ProfileGrouping.PLUGIN, "maven-surefire-plugin").durationMillis());
    }

    @Test
    public void of_groupedByPlugin_countsEveryExecutionItFound()
    {
        assertEquals(2, group(build(), ProfileGrouping.PLUGIN, "maven-surefire-plugin").mojos().size());
    }

    @Test
    public void of_groupedByPhase_gathersTheGoalsBoundToThatPhase()
    {
        assertEquals(300L, group(build(), ProfileGrouping.PHASE, "test").durationMillis());
    }

    @Test
    public void of_groupedByWorker_gathersWhatEachBuilderThreadRan()
    {
        assertEquals(2, group(build(), ProfileGrouping.WORKER, "w-1").mojos().size());
    }

    @Test
    public void of_groupedByModule_keepsAModuleThatRanNoGoalsAtAll()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming("g:silent", 0L, 40L));

        assertEquals(1, ProfileGroups.of(profile, ProfileView.defaults()).size());
    }

    @Test
    public void of_aFilterMatchingOneGoalOnly_keepsItsGroupWithNothingElseUnderIt()
    {
        List<ProfileGroup> groups =
            ProfileGroups.of(build(), ProfileView.defaults().withFilter("surefire"));

        assertEquals(List.of(SUREFIRE), named(groups, CORE).mojos().stream().map(MojoTiming::goal).toList());
    }

    @Test
    public void of_aFilterMatchingTheGroupItself_keepsEverythingUnderIt()
    {
        assertEquals(
            2,
            named(ProfileGroups.of(build(), ProfileView.defaults().withFilter("core")), CORE).mojos().size());
    }

    @Test
    public void of_aFilterMatchingNothing_leavesNoGroupsAtAll()
    {
        assertTrue(ProfileGroups.of(build(), ProfileView.defaults().withFilter("nothing")).isEmpty());
    }

    @Test
    public void of_sortedByDurationLargestFirst_leadsWithTheLongestModule()
    {
        assertEquals(CORE, ProfileGroups.of(build(), ProfileView.defaults()).get(0).name());
    }

    @Test
    public void of_sortedByDurationSmallestFirst_leadsWithTheShortestModule()
    {
        assertEquals(
            APP, ProfileGroups.of(build(), ProfileView.defaults().withDescending(false)).get(0).name());
    }

    @Test
    public void of_sortedByStart_followsTheOrderTheBuildRanThemIn()
    {
        assertEquals(
            CORE,
            ProfileGroups
                .of(build(), ProfileView.defaults().withSort(ProfileSort.STARTED).withDescending(false))
                .get(0)
                .name());
    }

    @Test
    public void of_groupedByModule_carriesWhatShorteningTheModuleWouldSave()
    {
        assertEquals(500L, group(build(), ProfileGrouping.MODULE, CORE).payoffMillis());
    }

    @Test
    public void of_groupedByPlugin_carriesNoPayoffBecauseAPluginIsNotAReactorNode()
    {
        assertFalse(
            "a payoff answers what the build would save if one node of the graph were free, and a "
                + "plugin spread over the reactor is not one of those nodes",
            group(build(), ProfileGrouping.PLUGIN, "maven-surefire-plugin").hasPayoff());
    }

    private static BuildProfile build()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming(CORE, 0L, 500L, "w-1"));
        profile.accept(new ModuleTiming(APP, 500L, 200L, "w-2"));
        profile.accept(new ProfileEvent.ReactorEdge(APP, CORE));
        profile.accept(new MojoTiming(CORE, COMPILE, "default-compile", 0L, 120L, "compile", "w-1"));
        profile.accept(new MojoTiming(CORE, SUREFIRE, "default-test", 120L, 200L, "test", "w-1"));
        profile.accept(new MojoTiming(APP, SUREFIRE, "default-test", 500L, 100L, "test", "w-2"));

        return profile;
    }

    private static ProfileGroup group(BuildProfile profile, ProfileGrouping grouping, String name)
    {
        return named(ProfileGroups.of(profile, ProfileView.defaults().withGrouping(grouping)), name);
    }

    private static ProfileGroup named(List<ProfileGroup> groups, String name)
    {
        return groups.stream().filter(group -> group.name().equals(name)).findFirst().orElseThrow();
    }
}
