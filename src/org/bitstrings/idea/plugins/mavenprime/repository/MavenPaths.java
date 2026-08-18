package org.bitstrings.idea.plugins.mavenprime.repository;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;

public final class MavenPaths
{
    private static final Logger LOG = Logger.getInstance(MavenPaths.class);

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    private MavenPaths()
    {
    }

    public static Path toPath(Path userHome, String text)
    {
        String expanded = expand(StringUtils.trimToEmpty(text), userHome);

        if (StringUtils.isBlank(expanded))
        {
            return null;
        }

        try
        {
            Path path = Paths.get(SystemInfo.isWindows ? expanded : expanded.replace('\\', '/'));

            return path.isAbsolute() ? path.normalize() : userHome.resolve(path).normalize();
        }
        catch (InvalidPathException malformed)
        {
            LOG.debug("Maven Prime ignored a malformed path: " + expanded, malformed);

            return null;
        }
    }

    private static String expand(String text, Path userHome)
    {
        Matcher placeholder = PLACEHOLDER.matcher(text);

        StringBuilder expanded = new StringBuilder();

        while (placeholder.find())
        {
            placeholder.appendReplacement(
                expanded, Matcher.quoteReplacement(valueOf(placeholder.group(1), userHome)));
        }

        placeholder.appendTail(expanded);

        return expanded.toString();
    }

    private static String valueOf(String name, Path userHome)
    {
        if ("user.home".equals(name))
        {
            return userHome.toString();
        }

        if (name.startsWith("env."))
        {
            return StringUtils.defaultString(System.getenv(name.substring("env.".length())));
        }

        return StringUtils.defaultString(System.getProperty(name));
    }
}
