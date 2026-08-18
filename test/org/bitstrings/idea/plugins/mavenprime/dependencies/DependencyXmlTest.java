package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.junit.Test;

public class DependencyXmlTest
{
    @Test
    public void of_compileScopedJar_omitsTypeAndScope()
    {
        String xml = DependencyXml.of(artifact("jar", null, "compile"));

        assertEquals(
            "<dependency>\n"
                + "    <groupId>com.lib</groupId>\n"
                + "    <artifactId>widget</artifactId>\n"
                + "    <version>1.0</version>\n"
                + "</dependency>",
            xml);
    }

    @Test
    public void of_testScopedArtifact_writesTheScope()
    {
        assertTrue(DependencyXml.of(artifact("jar", null, "test")).contains("<scope>test</scope>"));
    }

    @Test
    public void of_blankScope_omitsTheScope()
    {
        assertFalse(DependencyXml.of(artifact("jar", null, null)).contains("<scope>"));
    }

    @Test
    public void of_nonJarType_writesTheType()
    {
        assertTrue(DependencyXml.of(artifact("pom", null, "import")).contains("<type>pom</type>"));
    }

    @Test
    public void of_classifiedArtifact_writesTheClassifier()
    {
        assertTrue(
            DependencyXml.of(artifact("jar", "sources", "compile")).contains("<classifier>sources</classifier>"));
    }

    @Test
    public void of_coordinatesForAPlainJar_rendersGroupArtifactAndVersionOnly()
    {
        String xml = DependencyXml.of(coordinates("jar", StringUtils.EMPTY));

        assertEquals(
            "<dependency>\n"
                + "    <groupId>com.lib</groupId>\n"
                + "    <artifactId>widget</artifactId>\n"
                + "    <version>1.0</version>\n"
                + "</dependency>",
            xml);
    }

    @Test
    public void of_coordinatesWithANonJarExtension_writesTheType()
    {
        assertTrue(DependencyXml.of(coordinates("zip", StringUtils.EMPTY)).contains("<type>zip</type>"));
    }

    @Test
    public void of_classifiedCoordinates_writesTheClassifier()
    {
        assertTrue(
            DependencyXml.of(coordinates("jar", "sources")).contains("<classifier>sources</classifier>"));
    }

    @Test
    public void of_coordinates_neverWritesAScopeBecauseTheLocalRepositoryHasNone()
    {
        assertFalse(DependencyXml.of(coordinates("jar", StringUtils.EMPTY)).contains("<scope>"));
    }

    private static ArtifactCoordinates coordinates(String extension, String classifier)
    {
        return new ArtifactCoordinates("com.lib", "widget", extension, classifier, "1.0");
    }

    private static MavenArtifact artifact(String type, String classifier, String scope)
    {
        return new MavenArtifact(
            "com.lib", "widget", "1.0", "1.0", type, classifier, scope, false, type, null, null, false, false);
    }
}
