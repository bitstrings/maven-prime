package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.util.List;

public record MvndDownload(String version, MvndPlatform platform)
{
    public static final String DEFAULT_VERSION = "1.0.6";

    private static final String CHECKSUM_SUFFIX = ".sha512";

    // downloads.apache.org drops superseded releases; the archive keeps them, so a pinned version resolves.
    private static final List<String> MIRRORS =
        List.of("https://downloads.apache.org/maven/mvnd/", "https://archive.apache.org/dist/maven/mvnd/");

    public static MvndDownload of(String version)
    {
        return new MvndDownload(version, MvndPlatform.current());
    }

    public String archiveName()
    {
        return "maven-mvnd-" + version + '-' + platform.getClassifier() + ".zip";
    }

    public static List<String> indexUrls()
    {
        return List.copyOf(MIRRORS);
    }

    public List<String> urls()
    {
        return MIRRORS.stream().map(mirror -> mirror + version + '/' + archiveName()).toList();
    }

    public static String checksumUrl(String archiveUrl)
    {
        return archiveUrl + CHECKSUM_SUFFIX;
    }
}
