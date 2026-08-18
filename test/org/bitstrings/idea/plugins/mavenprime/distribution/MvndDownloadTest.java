package org.bitstrings.idea.plugins.mavenprime.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class MvndDownloadTest
{
    private static final String VERSION = "1.0.6";

    @Test
    public void of_windows_picksTheWindowsBuildWhateverTheArchitecture()
    {
        assertEquals(MvndPlatform.WINDOWS_AMD64, MvndPlatform.of(true, false, true));
    }

    @Test
    public void of_appleSilicon_picksTheAarch64MacBuild()
    {
        assertEquals(MvndPlatform.DARWIN_AARCH64, MvndPlatform.of(false, true, true));
    }

    @Test
    public void of_intelMac_picksTheAmd64MacBuild()
    {
        assertEquals(MvndPlatform.DARWIN_AMD64, MvndPlatform.of(false, true, false));
    }

    @Test
    public void of_linuxOnArm_picksTheAarch64LinuxBuild()
    {
        assertEquals(MvndPlatform.LINUX_AARCH64, MvndPlatform.of(false, false, true));
    }

    @Test
    public void of_linuxOnIntel_picksTheAmd64LinuxBuild()
    {
        assertEquals(MvndPlatform.LINUX_AMD64, MvndPlatform.of(false, false, false));
    }

    @Test
    public void archiveName_aVersionAndPlatform_matchesApacheReleaseNaming()
    {
        assertEquals(
            "maven-mvnd-1.0.6-linux-amd64.zip",
            new MvndDownload(VERSION, MvndPlatform.LINUX_AMD64).archiveName());
    }

    @Test
    public void urls_anyVersion_triesTheCurrentMirrorBeforeTheArchive()
    {
        List<String> urls = new MvndDownload(VERSION, MvndPlatform.LINUX_AMD64).urls();

        assertEquals(
            "https://downloads.apache.org/maven/mvnd/1.0.6/maven-mvnd-1.0.6-linux-amd64.zip", urls.get(0));
        assertEquals(
            "https://archive.apache.org/dist/maven/mvnd/1.0.6/maven-mvnd-1.0.6-linux-amd64.zip",
            urls.get(1));
    }

    @Test
    public void checksumUrl_anArchiveUrl_pointsAtItsPublishedDigest()
    {
        assertTrue(MvndDownload.checksumUrl("https://example.org/a.zip").endsWith("a.zip.sha512"));
    }
}
