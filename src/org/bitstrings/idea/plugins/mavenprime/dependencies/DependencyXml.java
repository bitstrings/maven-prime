package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenConstants;

import com.intellij.openapi.util.text.StringUtil;

public final class DependencyXml
{
    private static final String INDENT = "    ";

    private DependencyXml()
    {
    }

    public static String of(MavenArtifact artifact)
    {
        String scope = DependencyScopeFilter.effectiveScope(artifact);

        return render(
            artifact.getGroupId(),
            artifact.getArtifactId(),
            artifact.getVersion(),
            artifact.getType(),
            artifact.getClassifier(),
            MavenConstants.SCOPE_COMPILE.equals(scope) ? null : artifact.getScope());
    }

    public static String of(ArtifactCoordinates coordinates)
    {
        return render(
            coordinates.groupId(),
            coordinates.artifactId(),
            coordinates.version(),
            coordinates.extension(),
            coordinates.classifier(),
            null);
    }

    private static String render(
        String groupId, String artifactId, String version, String type, String classifier, String scope)
    {
        StringBuilder xml = new StringBuilder("<dependency>\n");

        appendElement(xml, "groupId", groupId);
        appendElement(xml, "artifactId", artifactId);
        appendElement(xml, "version", version);

        if (!MavenConstants.TYPE_JAR.equals(type))
        {
            appendElement(xml, "type", type);
        }

        appendElement(xml, "classifier", classifier);
        appendElement(xml, "scope", scope);

        return xml.append("</dependency>").toString();
    }

    private static void appendElement(StringBuilder xml, String name, String value)
    {
        if (StringUtils.isBlank(value))
        {
            return;
        }

        xml
            .append(INDENT)
            .append('<')
            .append(name)
            .append('>')
            .append(StringUtil.escapeXmlEntities(value))
            .append("</")
            .append(name)
            .append(">\n");
    }
}
