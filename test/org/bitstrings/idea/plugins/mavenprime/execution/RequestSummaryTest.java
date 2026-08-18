package org.bitstrings.idea.plugins.mavenprime.execution;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionKind;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class RequestSummaryTest
    extends BasePlatformTestCase
{
    public void testOf_requestCarryingNothingButGoals_isBlankSoTheColumnStaysEmpty()
    {
        assertEquals(StringUtils.EMPTY, RequestSummary.of(cleanInstall()));
    }

    public void testOf_enabledAndDisabledProfiles_rendersOneProfileOption()
    {
        MavenPrimeRequest request = cleanInstall();

        request.profiles.put("prod", Boolean.TRUE);
        request.profiles.put("slow", Boolean.FALSE);

        assertEquals("-P prod,!slow", RequestSummary.of(request));
    }

    public void testOf_userProperty_rendersItAsADefinition()
    {
        MavenPrimeRequest request = cleanInstall();

        request.properties.put("revision", "1.2.3");

        assertEquals("-Drevision=1.2.3", RequestSummary.of(request));
    }

    public void testOf_skipTestsFlag_rendersTheFlagOption()
    {
        MavenPrimeRequest request = cleanInstall();

        request.flags.add(MavenFlag.SKIP_TESTS);

        assertEquals(MavenFlag.SKIP_TESTS.getOption(), RequestSummary.of(request));
    }

    public void testOf_buildContextDistribution_leavesItOutBecauseItIsWhatEveryGoalDefaultsTo()
    {
        MavenPrimeRequest request = cleanInstall();

        request.distribution = DistributionSpec.context();

        assertEquals(StringUtils.EMPTY, RequestSummary.of(request));
    }

    public void testOf_explicitDaemonDistribution_namesTheDistribution()
    {
        MavenPrimeRequest request = cleanInstall();

        request.distribution = DistributionSpec.daemonHome("/opt/mvnd");

        assertEquals(DistributionKind.MVND_HOME.getTitle(), RequestSummary.of(request));
    }

    public void testOf_goalsAreNeverRepeated_becauseTheGoalColumnAlreadyShowsThem()
    {
        MavenPrimeRequest request = cleanInstall();

        request.flags.add(MavenFlag.SKIP_TESTS);

        assertFalse(RequestSummary.of(request).contains("install"));
    }

    public void testOf_twoRequestsSharingAGoalLine_produceDifferentSummaries()
    {
        MavenPrimeRequest production = cleanInstall();
        MavenPrimeRequest development = cleanInstall();

        production.profiles.put("prod", Boolean.TRUE);
        development.profiles.put("dev", Boolean.TRUE);

        assertFalse(RequestSummary.of(production).equals(RequestSummary.of(development)));
    }

    private static MavenPrimeRequest cleanInstall()
    {
        return MavenPrimeRequest.in("/tmp", List.of("clean", "install"));
    }
}
