package org.bitstrings.idea.plugins.mavenprime.util;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ProjectTrustPlatformTest
    extends BasePlatformTestCase
{
    public void testIsTrusted_aProjectTheIdeOpenedItself_readsAsTrusted()
    {
        assertTrue(
            "either no trusted-project entry point resolved on this IDE, or it disagreed with the platform",
            ProjectTrust.isTrusted(getProject()));
    }

    public void testResolvedApi_theSdkThisPluginBuildsAgainst_readsThroughItsOwnStaticTrustApi()
    {
        assertEquals(
            "2024.3 exposes the read only on " + ProjectTrust.LEGACY_API + "; once the base SDK moves to"
                + " 2026.2 this becomes " + ProjectTrust.CURRENT_API + " and the fallback serves old IDEs only",
            ProjectTrust.LEGACY_API,
            ProjectTrust.resolvedApi());
    }
}
