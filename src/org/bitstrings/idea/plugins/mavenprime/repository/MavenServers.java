package org.bitstrings.idea.plugins.mavenprime.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;

public final class MavenServers
{
    static final String SERVERS = "servers";

    static final String SERVER = "server";

    static final String ID = "id";

    static final String USERNAME = "username";

    static final String PASSWORD = "password";

    private static final Logger LOG = Logger.getInstance(MavenServers.class);

    private static final char ENCRYPTED_PREFIX = '{';

    private static final char ENCRYPTED_SUFFIX = '}';

    private MavenServers()
    {
    }

    public static Map<String, RepositoryCredentials> of(Project project)
    {
        Map<String, RepositoryCredentials> byId = new LinkedHashMap<>();

        for (Path file : MavenSettingsFiles.of(project))
        {
            byId.putAll(in(file));
        }

        return byId;
    }

    static Map<String, RepositoryCredentials> in(Path settingsFile)
    {
        if (!Files.isRegularFile(settingsFile))
        {
            return Map.of();
        }

        try
        {
            return serversIn(JDOMUtil.load(settingsFile));
        }
        catch (IOException | JDOMException unreadable)
        {
            LOG.debug("Maven Prime could not read " + settingsFile, unreadable);

            return Map.of();
        }
    }

    static Map<String, RepositoryCredentials> parse(String settings)
    {
        try
        {
            return serversIn(JDOMUtil.load(settings));
        }
        catch (IOException | JDOMException malformed)
        {
            LOG.debug("Maven Prime could not parse Maven settings", malformed);

            return Map.of();
        }
    }

    private static Map<String, RepositoryCredentials> serversIn(Element root)
    {
        Map<String, RepositoryCredentials> byId = new LinkedHashMap<>();

        for (Element servers : childrenNamed(root, SERVERS))
        {
            for (Element server : childrenNamed(servers, SERVER))
            {
                String id = textOf(server, ID);

                if (StringUtils.isNotBlank(id))
                {
                    byId.put(id, credentialsOf(server));
                }
            }
        }

        return byId;
    }

    static boolean isEncrypted(String value)
    {
        String trimmed = StringUtils.trimToEmpty(value);

        return (trimmed.length() > 1)
            && (trimmed.charAt(0) == ENCRYPTED_PREFIX)
            && (trimmed.charAt(trimmed.length() - 1) == ENCRYPTED_SUFFIX);
    }

    private static RepositoryCredentials credentialsOf(Element server)
    {
        String username = textOf(server, USERNAME);
        String password = textOf(server, PASSWORD);

        if (isEncrypted(username) || isEncrypted(password))
        {
            return RepositoryCredentials.encrypted();
        }

        if (StringUtils.isBlank(username) || (password == null))
        {
            return RepositoryCredentials.none();
        }

        return new RepositoryCredentials(username, password, RepositoryCredentials.Source.SETTINGS_XML);
    }

    private static List<Element> childrenNamed(Element parent, String name)
    {
        List<Element> found = new ArrayList<>();

        for (Element child : parent.getChildren())
        {
            if (name.equals(child.getName()))
            {
                found.add(child);
            }
        }

        return found;
    }

    private static String textOf(Element parent, String name)
    {
        for (Element child : parent.getChildren())
        {
            if (name.equals(child.getName()))
            {
                return child.getTextTrim();
            }
        }

        return null;
    }
}
