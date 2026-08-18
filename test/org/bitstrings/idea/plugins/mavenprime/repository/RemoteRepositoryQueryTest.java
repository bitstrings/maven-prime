package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenRemoteRepository;
import org.junit.Test;

public class RemoteRepositoryQueryTest
{
    private static final ArtifactCoordinates WIDGET =
        new ArtifactCoordinates("org.example", "widget", "jar", StringUtils.EMPTY, "1.2.3");

    @Test
    public void parseVersions_metadataListingVersions_returnsThemInDocumentOrder()
    {
        List<String> versions =
            RemoteRepositoryQuery.parseVersions(
                "<metadata><versioning><versions>"
                    + "<version>1.0</version><version>2.0</version>"
                    + "</versions></versioning></metadata>");

        assertEquals(List.of("1.0", "2.0"), versions);
    }

    @Test
    public void parseVersions_metadataWithoutAVersioningElement_returnsNothing()
    {
        assertTrue(RemoteRepositoryQuery.parseVersions("<metadata/>").isEmpty());
    }

    @Test
    public void parseVersions_blankVersionEntry_isSkipped()
    {
        List<String> versions =
            RemoteRepositoryQuery.parseVersions(
                "<metadata><versioning><versions>"
                    + "<version>1.0</version><version>  </version>"
                    + "</versions></versioning></metadata>");

        assertEquals(List.of("1.0"), versions);
    }

    @Test
    public void parseVersions_malformedXml_returnsNothingInsteadOfFailing()
    {
        assertTrue(RemoteRepositoryQuery.parseVersions("<metadata>").isEmpty());
    }

    @Test
    public void availabilityOf_okStatus_reportsTheArtifactFound()
    {
        assertEquals(
            RemoteAvailability.FOUND, RemoteRepositoryQuery.availabilityOf(HttpURLConnection.HTTP_OK));
    }

    @Test
    public void availabilityOf_notFoundStatus_reportsTheArtifactAbsent()
    {
        assertEquals(
            RemoteAvailability.NOT_FOUND,
            RemoteRepositoryQuery.availabilityOf(HttpURLConnection.HTTP_NOT_FOUND));
    }

    @Test
    public void availabilityOf_unauthorizedStatus_reportsAnAuthenticationProblem()
    {
        assertEquals(
            RemoteAvailability.UNAUTHORIZED,
            RemoteRepositoryQuery.availabilityOf(HttpURLConnection.HTTP_UNAUTHORIZED));
    }

    @Test
    public void availabilityOf_forbiddenStatus_reportsAnAuthenticationProblem()
    {
        assertEquals(
            RemoteAvailability.UNAUTHORIZED,
            RemoteRepositoryQuery.availabilityOf(HttpURLConnection.HTTP_FORBIDDEN));
    }

    @Test
    public void availabilityOf_movedPermanently_reportsARedirectRatherThanUnreachable()
    {
        assertEquals(
            RemoteAvailability.REDIRECTED,
            RemoteRepositoryQuery.availabilityOf(HttpURLConnection.HTTP_MOVED_PERM));
    }

    @Test
    public void availabilityOf_temporaryRedirect_reportsARedirectRatherThanUnreachable()
    {
        assertEquals(
            RemoteAvailability.REDIRECTED,
            RemoteRepositoryQuery.availabilityOf(HttpURLConnection.HTTP_MOVED_TEMP));
    }

    @Test
    public void availabilityOf_serverError_isNotMistakenForAMissingArtifact()
    {
        assertEquals(
            RemoteAvailability.UNREACHABLE,
            RemoteRepositoryQuery.availabilityOf(HttpURLConnection.HTTP_INTERNAL_ERROR));
    }

    @Test
    public void urlOf_repositoryUrlWithoutATrailingSlash_insertsTheSeparator()
    {
        assertEquals(
            "https://repo.example.org/org/example/widget/1.2.3/widget-1.2.3.jar",
            RemoteRepositoryQuery.urlOf(repository("https://repo.example.org"), WIDGET.remotePath()));
    }

    @Test
    public void urlOf_repositoryUrlEndingWithASlash_doesNotDoubleTheSeparator()
    {
        assertEquals(
            "https://repo.example.org/org/example/widget/1.2.3/widget-1.2.3.jar",
            RemoteRepositoryQuery.urlOf(repository("https://repo.example.org/"), WIDGET.remotePath()));
    }

    @Test
    public void urlOf_metadataPath_addressesTheArtifactLevelMetadata()
    {
        assertEquals(
            "https://repo.example.org/org/example/widget/maven-metadata.xml",
            RemoteRepositoryQuery.urlOf(repository("https://repo.example.org"), WIDGET.metadataPath()));
    }

    private static MavenRemoteRepository repository(String url)
    {
        return new MavenRemoteRepository("example", "Example", url, "default", null, null);
    }

    @Test
    public void carriesCredentials_anHttpsRepository_isTrue()
        throws MalformedURLException
    {
        assertTrue(RemoteRepositoryQuery.carriesCredentials(url("https://repo.example.invalid/m2")));
    }

    @Test
    public void carriesCredentials_aCleartextRepository_isFalse()
        throws MalformedURLException
    {
        assertFalse(RemoteRepositoryQuery.carriesCredentials(url("http://repo.example.invalid/m2")));
    }

    @Test
    public void authorize_anHttpsRepository_sendsTheCredentials()
        throws MalformedURLException
    {
        RecordingConnection connection = new RecordingConnection("https://repo.example.invalid/m2");

        RemoteRepositoryQuery.authorize(connection, usableCredentials());

        assertNotNull(connection.authorization);
    }

    @Test
    public void authorize_aCleartextRepository_withholdsThemRatherThanLeakingThePassword()
        throws MalformedURLException
    {
        RecordingConnection connection = new RecordingConnection("http://repo.example.invalid/m2");

        RemoteRepositoryQuery.authorize(connection, usableCredentials());

        assertNull(
            "Basic auth over http puts the password on the wire in clear",
            connection.authorization);
    }

    private static URL url(String address)
        throws MalformedURLException
    {
        try
        {
            return URI.create(address).toURL();
        }
        catch (IllegalArgumentException notAUrl)
        {
            throw new MalformedURLException(notAUrl.getMessage());
        }
    }

    private static RepositoryCredentials usableCredentials()
    {
        return new RepositoryCredentials("build", "secret", RepositoryCredentials.Source.SETTINGS_XML);
    }

    private static final class RecordingConnection
        extends URLConnection
    {
        private String authorization;

        RecordingConnection(String address)
            throws MalformedURLException
        {
            super(url(address));
        }

        @Override
        public void setRequestProperty(String key, String value)
        {
            if (RemoteRepositoryQuery.AUTHORIZATION_HEADER.equals(key))
            {
                authorization = value;
            }
        }

        @Override
        public void connect()
        {
        }
    }
}
