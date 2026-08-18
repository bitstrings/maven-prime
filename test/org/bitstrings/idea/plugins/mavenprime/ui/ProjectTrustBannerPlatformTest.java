package org.bitstrings.idea.plugins.mavenprime.ui;

import org.bitstrings.idea.plugins.mavenprime.config.MavenPrimeConfigService;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.EditorNotificationPanel;

public class ProjectTrustBannerPlatformTest
    extends BasePlatformTestCase
{
    public void testCreate_aBannerNobodyHasUpdatedYet_staysDown()
    {
        assertFalse(
            "a banner that shows before anything asked about trust is a warning on every project",
            ProjectTrustBanner.create().isVisible());
    }

    public void testUpdate_anUntrustedProject_showsTheNotice()
    {
        EditorNotificationPanel banner = ProjectTrustBanner.create();

        ProjectTrustBanner.update(banner, false);

        assertTrue(
            "an untrusted project drops its shared goals and properties, so it has to say so",
            banner.isVisible());
    }

    public void testUpdate_aProjectTrustedAfterTheNoticeWasShown_takesTheNoticeBackDown()
    {
        EditorNotificationPanel banner = ProjectTrustBanner.create();

        ProjectTrustBanner.update(banner, false);
        ProjectTrustBanner.update(banner, true);

        assertFalse("a trusted project must not keep a warning it no longer earns", banner.isVisible());
    }

    public void testMessageOf_anUntrustedProject_namesTheFileThatStaysOut()
    {
        assertTrue(
            "the notice is useless if it does not name what is missing",
            ProjectTrustBanner.messageOf().contains(MavenPrimeConfigService.FILE_NAME));
    }

    public void testMessageOf_anUntrustedProject_resolvesItsBundleText()
    {
        String message = ProjectTrustBanner.messageOf();

        assertFalse("unresolved bundle key: " + message, message.startsWith("!") && message.endsWith("!"));
    }
}
