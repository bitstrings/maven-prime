package org.bitstrings.idea.plugins.mavenprime.ui;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.config.MavenPrimeConfigService;

import com.intellij.ui.EditorNotificationPanel;

public final class ProjectTrustBanner
{
    private ProjectTrustBanner()
    {
    }

    public static EditorNotificationPanel create()
    {
        EditorNotificationPanel banner =
            new EditorNotificationPanel(EditorNotificationPanel.Status.Warning);

        banner.setText(messageOf());
        banner.setVisible(false);

        return banner;
    }

    public static void update(EditorNotificationPanel banner, boolean trusted)
    {
        banner.setVisible(!trusted);
    }

    static String messageOf()
    {
        return MavenPrimeBundle.message("mavenprime.config.untrustedNotice", MavenPrimeConfigService.FILE_NAME);
    }
}
