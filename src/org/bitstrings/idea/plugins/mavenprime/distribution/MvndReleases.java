package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.text.VersionComparatorUtil;

public final class MvndReleases
{
    private static final List<String> PRERELEASE_MARKERS =
        List.of("-rc", "-alpha", "-beta", "-m", "-snapshot");

    private static final Logger LOG = Logger.getInstance(MvndReleases.class);

    private static final Pattern VERSION_DIRECTORY = Pattern.compile("href=\"(\\d[^\"/]*)/\"");

    private MvndReleases()
    {
    }

    public static List<String> available()
    {
        for (String index : MvndDownload.indexUrls())
        {
            List<String> versions = parse(read(index));

            if (!versions.isEmpty())
            {
                return versions;
            }
        }

        return List.of(MvndDownload.DEFAULT_VERSION);
    }

    public static String newestStable(List<String> versions)
    {
        for (String version : versions)
        {
            if (isStable(version))
            {
                return version;
            }
        }

        return versions.isEmpty() ? MvndDownload.DEFAULT_VERSION : versions.get(0);
    }

    static boolean isStable(String version)
    {
        String lower = StringUtils.lowerCase(version);

        for (String marker : PRERELEASE_MARKERS)
        {
            if (lower.contains(marker))
            {
                return false;
            }
        }

        return true;
    }

    static List<String> parse(String html)
    {
        if (html == null)
        {
            return List.of();
        }

        Set<String> versions = new LinkedHashSet<>();

        Matcher directory = VERSION_DIRECTORY.matcher(html);

        while (directory.find())
        {
            versions.add(directory.group(1));
        }

        List<String> sorted = new ArrayList<>(versions);

        sorted.sort((left, right) -> VersionComparatorUtil.compare(right, left));

        return List.copyOf(sorted);
    }

    private static String read(String url)
    {
        try
        {
            return HttpRequests.request(url).readString();
        }
        catch (IOException unreachable)
        {
            LOG.debug("Maven Prime could not list mvnd releases at " + url, unreachable);

            return null;
        }
    }
}
