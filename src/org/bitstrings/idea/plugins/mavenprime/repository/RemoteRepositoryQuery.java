package org.bitstrings.idea.plugins.mavenprime.repository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.idea.maven.model.MavenRemoteRepository;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.text.VersionComparatorUtil;

public final class RemoteRepositoryQuery
{
    private static final String HTTPS_PROTOCOL = "https";

    static final String VERSIONING = "versioning";

    static final String VERSIONS = "versions";

    static final String VERSION = "version";

    private static final Logger LOG = Logger.getInstance(RemoteRepositoryQuery.class);

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;

    private static final int READ_TIMEOUT_MILLIS = 5_000;

    private static final String URL_SEPARATOR = "/";

    static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BASIC_PREFIX = "Basic ";

    private RemoteRepositoryQuery()
    {
    }

    public static List<RemoteProbeResult> probe(
        List<MavenRemoteRepository> repositories,
        ArtifactCoordinates coordinates,
        Function<MavenRemoteRepository, RepositoryCredentials> credentials)
    {
        List<RemoteProbeResult> results = new ArrayList<>();

        for (MavenRemoteRepository repository : repositories)
        {
            ProgressManager.checkCanceled();

            results.add(probeIn(repository, coordinates, credentials.apply(repository)));
        }

        return List.copyOf(results);
    }

    public static List<String> versions(
        List<MavenRemoteRepository> repositories,
        ArtifactCoordinates coordinates,
        Function<MavenRemoteRepository, RepositoryCredentials> credentials)
    {
        Set<String> versions = new TreeSet<>((left, right) -> VersionComparatorUtil.compare(right, left));

        for (MavenRemoteRepository repository : repositories)
        {
            ProgressManager.checkCanceled();

            versions.addAll(versionsIn(repository, coordinates, credentials.apply(repository)));
        }

        return List.copyOf(versions);
    }

    static String basicAuthorization(RepositoryCredentials credentials)
    {
        return BASIC_PREFIX
            + Base64
                .getEncoder()
                .encodeToString(
                    (credentials.username() + ':' + credentials.password())
                        .getBytes(StandardCharsets.UTF_8));
    }

    static List<String> parseVersions(String metadata)
    {
        try
        {
            Element versioning = JDOMUtil.load(metadata).getChild(VERSIONING);

            Element versions = (versioning == null) ? null : versioning.getChild(VERSIONS);

            if (versions == null)
            {
                return List.of();
            }

            List<String> parsed = new ArrayList<>();

            for (Element version : versions.getChildren(VERSION))
            {
                if (StringUtils.isNotBlank(version.getTextTrim()))
                {
                    parsed.add(version.getTextTrim());
                }
            }

            return List.copyOf(parsed);
        }
        catch (IOException | JDOMException malformed)
        {
            LOG.debug("Maven Prime could not parse Maven metadata", malformed);

            return List.of();
        }
    }

    static RemoteAvailability availabilityOf(int statusCode)
    {
        if ((statusCode >= HttpURLConnection.HTTP_OK)
            && (statusCode < HttpURLConnection.HTTP_MULT_CHOICE))
        {
            return RemoteAvailability.FOUND;
        }

        if ((statusCode >= HttpURLConnection.HTTP_MULT_CHOICE)
            && (statusCode < HttpURLConnection.HTTP_BAD_REQUEST))
        {
            return RemoteAvailability.REDIRECTED;
        }

        return switch (statusCode)
        {
            case HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                RemoteAvailability.UNAUTHORIZED;
            case HttpURLConnection.HTTP_NOT_FOUND -> RemoteAvailability.NOT_FOUND;
            default -> RemoteAvailability.UNREACHABLE;
        };
    }

    static String urlOf(MavenRemoteRepository repository, String path)
    {
        String base = StringUtils.trimToEmpty(repository.getUrl());

        return base.endsWith(URL_SEPARATOR) ? (base + path) : (base + URL_SEPARATOR + path);
    }

    private static RemoteProbeResult probeIn(
        MavenRemoteRepository repository,
        ArtifactCoordinates coordinates,
        RepositoryCredentials credentials)
    {
        String url = urlOf(repository, coordinates.remotePath());

        try
        {
            int statusCode =
                HttpRequests
                    .head(url)
                    .connectTimeout(CONNECT_TIMEOUT_MILLIS)
                    .readTimeout(READ_TIMEOUT_MILLIS)
                    .productNameAsUserAgent()
                    .throwStatusCodeException(false)
                    .tuner(connection -> authorize(connection, credentials))
                    .tryConnect();

            return new RemoteProbeResult(
                repository.getId(),
                url,
                availabilityOf(statusCode),
                statusCode,
                null,
                credentials.source());
        }
        catch (IOException unreachable)
        {
            return new RemoteProbeResult(
                repository.getId(),
                url,
                RemoteAvailability.UNREACHABLE,
                0,
                unreachable.getMessage(),
                credentials.source());
        }
    }

    private static List<String> versionsIn(
        MavenRemoteRepository repository,
        ArtifactCoordinates coordinates,
        RepositoryCredentials credentials)
    {
        String url = urlOf(repository, coordinates.metadataPath());

        try
        {
            return parseVersions(
                HttpRequests
                    .request(url)
                    .connectTimeout(CONNECT_TIMEOUT_MILLIS)
                    .readTimeout(READ_TIMEOUT_MILLIS)
                    .productNameAsUserAgent()
                    .tuner(connection -> authorize(connection, credentials))
                    .readString());
        }
        catch (IOException unavailable)
        {
            LOG.debug("Maven Prime could not read " + url, unavailable);

            return List.of();
        }
    }

    static boolean carriesCredentials(URL url)
    {
        return HTTPS_PROTOCOL.equalsIgnoreCase(url.getProtocol());
    }

    static void authorize(URLConnection connection, RepositoryCredentials credentials)
    {
        if (credentials.isUsable() && carriesCredentials(connection.getURL()))
        {
            connection.setRequestProperty(AUTHORIZATION_HEADER, basicAuthorization(credentials));
        }
    }
}
