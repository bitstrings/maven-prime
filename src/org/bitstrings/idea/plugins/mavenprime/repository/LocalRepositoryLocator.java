package org.bitstrings.idea.plugins.mavenprime.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.idea.maven.indices.MavenIndexUtils;
import org.jetbrains.idea.maven.model.MavenRepositoryInfo;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.SystemProperties;

public final class LocalRepositoryLocator
{
    public static final String REPOSITORY_PROPERTY = "maven.repo.local";

    static final String DEFAULT_DIRECTORY = ".m2/repository";

    private static final Logger LOG = Logger.getInstance(LocalRepositoryLocator.class);

    private static final String LOCAL_REPOSITORY = "localRepository";

    private static final Map<Path, ParsedSettings> PARSED = new ConcurrentHashMap<>();

    private LocalRepositoryLocator()
    {
    }

    public static Path locate(Project project)
    {
        Path userHome = Paths.get(SystemProperties.getUserHome());

        BuildContext context = BuildContext.getInstance(project);

        Path declared = MavenPaths.toPath(userHome, context.getProperty(REPOSITORY_PROPERTY));

        if (declared != null)
        {
            return declared;
        }

        Path chosen =
            declaredIn(userHome, MavenPaths.toPath(userHome, context.getEnvironment().settingsFile));

        return (chosen == null) ? resolvedByIde(project, userHome) : chosen;
    }

    static Path declaredIn(Path userHome, Path settingsFile)
    {
        if (settingsFile == null)
        {
            return null;
        }

        BasicFileAttributes attributes = attributesOf(settingsFile);

        if ((attributes == null) || !attributes.isRegularFile())
        {
            return null;
        }

        ParsedSettings parsed = PARSED.get(settingsFile);

        if ((parsed != null) && parsed.describes(userHome, settingsFile, attributes))
        {
            return parsed.localRepository();
        }

        Path declared = parse(userHome, settingsFile);

        PARSED.put(settingsFile, new ParsedSettings(userHome, settingsFile, attributes, declared));

        return declared;
    }

    private static BasicFileAttributes attributesOf(Path settingsFile)
    {
        try
        {
            return Files.readAttributes(settingsFile, BasicFileAttributes.class);
        }
        catch (IOException absent)
        {
            LOG.debug("Maven Prime could not stat " + settingsFile, absent);

            return null;
        }
    }

    private static Path parse(Path userHome, Path settingsFile)
    {
        try
        {
            for (Element child : JDOMUtil.load(settingsFile).getChildren())
            {
                if (LOCAL_REPOSITORY.equals(child.getName()))
                {
                    return MavenPaths.toPath(userHome, child.getTextTrim());
                }
            }
        }
        catch (IOException | JDOMException unreadable)
        {
            LOG.debug("Maven Prime could not read " + settingsFile, unreadable);
        }

        return null;
    }

    private static Path resolvedByIde(Project project, Path userHome)
    {
        MavenRepositoryInfo local = MavenIndexUtils.getLocalRepository(project);

        Path effective = (local == null) ? null : MavenPaths.toPath(userHome, local.getUrl());

        return (effective == null) ? userHome.resolve(DEFAULT_DIRECTORY) : effective;
    }

    private record ParsedSettings(
        Path userHome, Path settingsFile, FileTime modified, long size, Path localRepository)
    {
        ParsedSettings(Path userHome, Path settingsFile, BasicFileAttributes attributes, Path localRepository)
        {
            this(userHome, settingsFile, attributes.lastModifiedTime(), attributes.size(), localRepository);
        }

        boolean describes(Path otherHome, Path otherFile, BasicFileAttributes attributes)
        {
            return userHome.equals(otherHome)
                && settingsFile.equals(otherFile)
                && modified.equals(attributes.lastModifiedTime())
                && (size == attributes.size());
        }
    }
}
