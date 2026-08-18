package org.bitstrings.idea.plugins.mavenprime.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionKind;
import org.bitstrings.idea.plugins.mavenprime.execution.FailureBehavior;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.execution.OutputLevel;
import org.junit.Test;

import com.google.gson.Gson;

public class RequestConfigTest
{
    @Test
    public void of_requestWithDefaultFailureBehavior_omitsIt()
    {
        assertNull(RequestConfig.of(new MavenPrimeRequest()).failureBehavior);
    }

    @Test
    public void of_requestWithoutFlags_omitsTheFlagList()
    {
        assertNull(RequestConfig.of(new MavenPrimeRequest()).flags);
    }

    @Test
    public void of_defaultPomFileName_omitsIt()
    {
        assertNull(RequestConfig.of(new MavenPrimeRequest()).pomFileName);
    }

    @Test
    public void applyTo_populatedConfig_roundTripsThroughGson()
    {
        MavenPrimeRequest original = new MavenPrimeRequest();

        original.setGoalLine("clean install");
        original.flags.add(MavenFlag.SKIP_TESTS);
        original.flags.add(MavenFlag.BATCH_MODE);
        original.properties.put("maven.build.cache.enabled", "true");
        original.profiles.put("ci", Boolean.TRUE);
        original.failureBehavior = FailureBehavior.FAIL_AT_END;
        original.outputLevel = OutputLevel.DEBUG;
        original.settingsFile = "/team/settings.xml";
        original.threads = "1C";

        String json = new Gson().toJson(RequestConfig.of(original));

        MavenPrimeRequest restored = new Gson().fromJson(json, RequestConfig.class).toRequest();

        assertEquals(List.of("clean", "install"), restored.goals);
        assertEquals(Set.of(MavenFlag.SKIP_TESTS, MavenFlag.BATCH_MODE), restored.flags);
        assertEquals(Map.of("maven.build.cache.enabled", "true"), restored.properties);
        assertEquals(Map.of("ci", Boolean.TRUE), restored.profiles);
        assertEquals(FailureBehavior.FAIL_AT_END, restored.failureBehavior);
        assertEquals(OutputLevel.DEBUG, restored.outputLevel);
        assertEquals("/team/settings.xml", restored.settingsFile);
        assertEquals("1C", restored.threads);
    }

    @Test
    public void applyTo_unknownFlagName_isIgnoredRatherThanFailing()
    {
        RequestConfig config = new RequestConfig();

        config.flags = List.of("SKIP_TESTS", "FLAG_FROM_A_NEWER_VERSION");

        assertEquals(Set.of(MavenFlag.SKIP_TESTS), config.toRequest().flags);
    }

    @Test
    public void applyTo_unknownFailureBehavior_keepsTheExistingValue()
    {
        RequestConfig config = new RequestConfig();

        config.failureBehavior = "NOT_A_MODE";

        assertEquals(FailureBehavior.DEFAULT, config.toRequest().failureBehavior);
    }

    @Test
    public void applyTo_profilesAlreadySet_mergesInsteadOfReplacing()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.profiles.put("ci", Boolean.TRUE);

        new Gson().fromJson("{\"profiles\":{\"release\":false}}", RequestConfig.class).applyTo(request);

        assertEquals(Map.of("ci", Boolean.TRUE, "release", Boolean.FALSE), request.profiles);
    }

    @Test
    public void applyTo_nullProfileValue_removesTheProfile()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.profiles.put("ci", Boolean.TRUE);

        new Gson().fromJson("{\"profiles\":{\"ci\":null}}", RequestConfig.class).applyTo(request);

        assertTrue(request.profiles.isEmpty());
    }

    @Test
    public void applyTo_nullPropertyValue_removesTheProperty()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.properties.put("skipTests", "true");

        new Gson().fromJson("{\"properties\":{\"skipTests\":null}}", RequestConfig.class).applyTo(request);

        assertTrue(request.properties.isEmpty());
    }

    @Test
    public void applyTo_goalsArrayCarryingANullEntry_dropsThatEntry()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        new Gson().fromJson("{\"goals\":[\"clean\",null,\"install\"]}", RequestConfig.class).applyTo(request);

        assertEquals(List.of("clean", "install"), request.goals);
    }

    @Test
    public void applyTo_projectsArrayCarryingANullEntry_dropsThatEntry()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        new Gson().fromJson("{\"projects\":[null,\"core\"]}", RequestConfig.class).applyTo(request);

        assertEquals(List.of("core"), request.projects);
    }

    @Test
    public void applyTo_goalsArrayCarryingABlankEntry_dropsThatEntry()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        new Gson().fromJson("{\"goals\":[\"clean\",\"  \"]}", RequestConfig.class).applyTo(request);

        assertEquals(List.of("clean"), request.goals);
    }

    @Test
    public void applyTo_goalsAlreadySet_replacesThemInsteadOfMerging()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.setGoalLine("clean install");

        new Gson().fromJson("{\"goals\":[\"verify\"]}", RequestConfig.class).applyTo(request);

        assertEquals(List.of("verify"), request.goals);
    }

    @Test
    public void applyTo_emptyConfig_leavesTheRequestUntouched()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.setGoalLine("verify");
        request.threads = "4";

        new RequestConfig().applyTo(request);

        assertEquals(List.of("verify"), request.goals);
        assertEquals("4", request.threads);
    }

    @Test
    public void toRequest_daemonDistribution_restoresKindAndPath()
    {
        RequestConfig config = new RequestConfig();

        config.distribution = new DistributionConfig();
        config.distribution.kind = "MVND_HOME";
        config.distribution.path = "/opt/mvnd";

        assertEquals(DistributionKind.MVND_HOME, config.toRequest().distribution.kind);
        assertEquals("/opt/mvnd", config.toRequest().distribution.path);
    }

    @Test
    public void toRequest_unknownDistributionKind_fallsBackToTheBuildContext()
    {
        RequestConfig config = new RequestConfig();

        config.distribution = new DistributionConfig();
        config.distribution.kind = "SOMETHING_ELSE";

        assertEquals(DistributionKind.CONTEXT, config.toRequest().distribution.kind);
    }

    @Test
    public void toRequest_noDistribution_followsTheBuildContext()
    {
        assertEquals(DistributionKind.CONTEXT, new RequestConfig().toRequest().distribution.kind);
    }

    @Test
    public void of_requestFollowingTheBuildContext_omitsTheDistribution()
    {
        assertNull(RequestConfig.of(new MavenPrimeRequest()).distribution);
    }

    @Test
    public void getGoalDefinitions_goalWithoutGoals_isDropped()
    {
        MavenPrimeConfig config = new Gson().fromJson("{\"goals\":[{\"name\":\"empty\"}]}", MavenPrimeConfig.class);

        assertTrue(config.getGoalDefinitions().isEmpty());
    }

    @Test
    public void getGoalDefinitions_nullElementInTheGoalsArray_isSkippedRatherThanThrowing()
    {
        MavenPrimeConfig config = new Gson().fromJson("{\"goals\":[null]}", MavenPrimeConfig.class);

        assertTrue(config.getGoalDefinitions().isEmpty());
    }

    @Test
    public void getGoalDefinitions_nullElementBesideARealGoal_keepsTheRealGoal()
    {
        MavenPrimeConfig config =
            new Gson()
                .fromJson(
                    "{\"goals\":[null,{\"name\":\"Fast build\","
                        + "\"request\":{\"goals\":[\"clean\",\"install\"]}}]}",
                    MavenPrimeConfig.class);

        assertEquals(1, config.getGoalDefinitions().size());
    }

    @Test
    public void getGoalDefinitions_goalRenamed_keepsTheSameGeneratedId()
    {
        MavenPrimeConfig before =
            new Gson()
                .fromJson(
                    "{\"goals\":[{\"name\":\"Fast build\",\"request\":{\"goals\":[\"clean\"]}}]}",
                    MavenPrimeConfig.class);
        MavenPrimeConfig after =
            new Gson()
                .fromJson(
                    "{\"goals\":[{\"name\":\"Full build\",\"request\":{\"goals\":[\"clean\"]}}]}",
                    MavenPrimeConfig.class);

        assertEquals(
            before.getGoalDefinitions().get(0).id, after.getGoalDefinitions().get(0).id);
    }

    @Test
    public void getGoalDefinitions_twoGoals_getDistinctGeneratedIds()
    {
        MavenPrimeConfig config =
            new Gson()
                .fromJson(
                    "{\"goals\":[{\"name\":\"a\",\"request\":{\"goals\":[\"clean\"]}},"
                        + "{\"name\":\"b\",\"request\":{\"goals\":[\"verify\"]}}]}",
                    MavenPrimeConfig.class);

        assertNotEquals(
            config.getGoalDefinitions().get(0).id, config.getGoalDefinitions().get(1).id);
    }

    @Test
    public void getGoalDefinitions_handWrittenFile_readsNameAndGoals()
    {
        MavenPrimeConfig config =
            new Gson()
                .fromJson(
                    "{\"version\":1,\"goals\":[{\"name\":\"Fast build\","
                        + "\"request\":{\"goals\":[\"clean\",\"install\"],\"flags\":[\"SKIP_TESTS\"]}}]}",
                    MavenPrimeConfig.class);

        assertEquals(1, config.getGoalDefinitions().size());
        assertEquals("Fast build", config.getGoalDefinitions().get(0).name);
        assertEquals("clean install", config.getGoalDefinitions().get(0).getGoalLine());
    }
}
