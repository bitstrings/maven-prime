package org.bitstrings.idea.plugins.mavenprime.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.junit.Test;

public class MavenPrimeRequestTest
{
    @Test
    public void setGoalLine_severalGoals_splitsThemInOrder()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.setGoalLine("clean install");

        assertEquals(List.of("clean", "install"), request.goals);
    }

    @Test
    public void setGoalLine_quotedArgument_keepsItAsOneGoal()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.setGoalLine("test \"-Dmessage=hello world\"");

        assertEquals(List.of("test", "-Dmessage=hello world"), request.goals);
    }

    @Test
    public void setGoalLine_blankLine_clearsTheGoals()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("install"));

        request.setGoalLine("   ");

        assertTrue(request.goals.isEmpty());
    }

    @Test
    public void getPomFileName_blankName_fallsBackToTheDefault()
    {
        MavenPrimeRequest request = new MavenPrimeRequest();

        request.pomFileName = "  ";

        assertEquals(MavenPrimeRequest.DEFAULT_POM_FILE_NAME, request.getPomFileName());
    }

    @Test
    public void copy_populatedRequest_equalsTheOriginal()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("clean", "verify"));

        request.flags.add(MavenFlag.OFFLINE);
        request.properties.put("skipTests", "true");
        request.profiles.put("release", Boolean.TRUE);

        assertEquals(request, request.copy());
        assertEquals(request.hashCode(), request.copy().hashCode());
    }

    @Test
    public void copy_mutatedCopy_leavesTheOriginalUntouched()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("clean"));

        MavenPrimeRequest copy = request.copy();

        copy.goals.add("install");

        assertEquals(List.of("clean"), request.goals);
    }

    @Test
    public void equals_differentGoals_isNotEqual()
    {
        assertNotEquals(
            MavenPrimeRequest.in("/project", List.of("clean")),
            MavenPrimeRequest.in("/project", List.of("install")));
    }

    @Test
    public void equals_differingOnlyInName_isStillEqual()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("clean"));

        MavenPrimeRequest renamed = request.copy();

        renamed.name = "Full build";

        assertEquals(request, renamed);
    }

    @Test
    public void equals_differingWorkingDirectory_isNotEqual()
    {
        assertNotEquals(
            MavenPrimeRequest.in("/project", List.of("clean")),
            MavenPrimeRequest.in("/other", List.of("clean")));
    }

    @Test
    public void equals_differingDistribution_isNotEqual()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in("/project", List.of("clean"));

        MavenPrimeRequest daemon = request.copy();

        daemon.distribution = DistributionSpec.daemonHome("/opt/mvnd");

        assertNotEquals(request, daemon);
    }

    @Test
    public void parseGoals_blankLine_yieldsNoGoals()
    {
        assertTrue(MavenPrimeRequest.parseGoals(null).isEmpty());
    }

    @Test
    public void validate_noGoals_reportsTheMissingGoals()
    {
        assertEquals(RequestProblem.NO_GOALS, new MavenPrimeRequest().validate());
    }

    @Test
    public void validate_goalsWithoutAWorkingDirectory_reportsTheMissingDirectory()
    {
        assertEquals(
            RequestProblem.NO_WORKING_DIRECTORY,
            MavenPrimeRequest.in(null, List.of("clean")).validate());
    }

    @Test
    public void validate_blankWorkingDirectory_reportsTheMissingDirectory()
    {
        assertEquals(
            RequestProblem.NO_WORKING_DIRECTORY,
            MavenPrimeRequest.in("   ", List.of("clean")).validate());
    }

    @Test
    public void validate_goalsAndWorkingDirectoryPresent_reportsNoProblem()
    {
        assertNull(MavenPrimeRequest.in("/project", List.of("clean")).validate());
    }
}
