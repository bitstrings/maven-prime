package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MavenServersTest
{
    private static final String NAMESPACE = "http://maven.apache.org/SETTINGS/1.0.0";

    private static final String LATIN_1_USERNAME = "josé";

    @Rule
    public final TemporaryFolder home = new TemporaryFolder();

    @Test
    public void in_settingsDeclaringLatin1_readsItInThatEncodingRatherThanUtf8()
        throws IOException
    {
        Path settingsFile = home.getRoot().toPath().resolve("settings.xml");

        Files.write(
            settingsFile,
            ("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><settings><servers><server>"
                + "<id>nexus</id><username>" + LATIN_1_USERNAME + "</username><password>s3cret</password>"
                + "</server></servers></settings>").getBytes(StandardCharsets.ISO_8859_1));

        assertEquals(LATIN_1_USERNAME, MavenServers.in(settingsFile).get("nexus").username());
    }

    @Test
    public void in_missingSettingsFile_readsNoServers()
    {
        assertTrue(MavenServers.in(home.getRoot().toPath().resolve("absent.xml")).isEmpty());
    }

    @Test
    public void parse_serverWithAPlaintextPassword_readsItFromSettings()
    {
        RepositoryCredentials credentials = MavenServers.parse(settings("nexus", "deploy", "s3cret")).get("nexus");

        assertEquals(RepositoryCredentials.Source.SETTINGS_XML, credentials.source());
    }

    @Test
    public void parse_serverWithAPlaintextPassword_isUsable()
    {
        assertTrue(MavenServers.parse(settings("nexus", "deploy", "s3cret")).get("nexus").isUsable());
    }

    @Test
    public void parse_serverWithAnEncryptedPassword_reportsItAsEncryptedRatherThanUsable()
    {
        RepositoryCredentials credentials =
            MavenServers.parse(settings("nexus", "deploy", "{kP5cLbGxYQ=}")).get("nexus");

        assertEquals(RepositoryCredentials.Source.ENCRYPTED, credentials.source());
    }

    @Test
    public void parse_serverWithAnEncryptedPassword_neverExposesTheCiphertextAsAPassword()
    {
        assertFalse(MavenServers.parse(settings("nexus", "deploy", "{kP5cLbGxYQ=}")).get("nexus").isUsable());
    }

    @Test
    public void parse_settingsDeclaringTheMavenNamespace_stillFindsTheServer()
    {
        String namespaced =
            "<settings xmlns=\"" + NAMESPACE + "\"><servers><server>"
                + "<id>nexus</id><username>deploy</username><password>s3cret</password>"
                + "</server></servers></settings>";

        assertTrue(MavenServers.parse(namespaced).containsKey("nexus"));
    }

    @Test
    public void parse_serverWithoutAnId_isIgnored()
    {
        String withoutId =
            "<settings><servers><server><username>deploy</username></server></servers></settings>";

        assertTrue(MavenServers.parse(withoutId).isEmpty());
    }

    @Test
    public void parse_serverWithoutAUsername_yieldsNoUsableCredentials()
    {
        String withoutUsername = "<settings><servers><server><id>nexus</id></server></servers></settings>";

        assertEquals(
            RepositoryCredentials.Source.NONE, MavenServers.parse(withoutUsername).get("nexus").source());
    }

    @Test
    public void parse_malformedSettings_returnsNothingInsteadOfFailing()
    {
        assertTrue(MavenServers.parse("<settings>").isEmpty());
    }

    @Test
    public void parse_severalServers_keepsThemAllById()
    {
        String two =
            "<settings><servers>"
                + "<server><id>a</id><username>ua</username><password>pa</password></server>"
                + "<server><id>b</id><username>ub</username><password>pb</password></server>"
                + "</servers></settings>";

        Map<String, RepositoryCredentials> servers = MavenServers.parse(two);

        assertEquals(2, servers.size());
    }

    @Test
    public void isEncrypted_bracedValue_isRecognized()
    {
        assertTrue(MavenServers.isEncrypted("{kP5cLbGxYQ=}"));
    }

    @Test
    public void isEncrypted_plainValue_isNotMistakenForCiphertext()
    {
        assertFalse(MavenServers.isEncrypted("s3cret"));
    }

    private static String settings(String id, String username, String password)
    {
        return "<settings><servers><server>"
            + "<id>" + id + "</id>"
            + "<username>" + username + "</username>"
            + "<password>" + password + "</password>"
            + "</server></servers></settings>";
    }
}
