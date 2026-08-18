package org.bitstrings.idea.plugins.mavenprime.repository;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenPlugin;

import com.intellij.util.text.VersionComparatorUtil;

public record ArtifactCoordinates(
    String groupId, String artifactId, String extension, String classifier, String version)
{
    public static final Comparator<ArtifactCoordinates> BY_ARTIFACT =
        Comparator
            .comparing(ArtifactCoordinates::artifactId, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ArtifactCoordinates::groupId)
            .thenComparing(
                ArtifactCoordinates::version, (left, right) -> VersionComparatorUtil.compare(right, left));

    private static final String DEFAULT_EXTENSION = "jar";

    public static final String DEFAULT_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

    private static final String METADATA_FILE = "maven-metadata.xml";

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    private static final Pattern TIMESTAMPED_SNAPSHOT = Pattern.compile("(.+)-\\d{8}\\.\\d{6}-\\d+");

    private static final int WITHOUT_CLASSIFIER = 4;

    private static final int WITH_CLASSIFIER = 5;

    private static final int SHORT_FORM = 3;

    public static Optional<ArtifactCoordinates> parse(String text)
    {
        String[] parts = StringUtils.splitPreserveAllTokens(StringUtils.trimToEmpty(text), ':');

        return switch ((parts == null) ? 0 : parts.length)
        {
            case SHORT_FORM ->
                Optional.of(
                    new ArtifactCoordinates(
                        parts[0], parts[1], DEFAULT_EXTENSION, StringUtils.EMPTY, parts[2]));
            case WITHOUT_CLASSIFIER ->
                Optional.of(
                    new ArtifactCoordinates(parts[0], parts[1], parts[2], StringUtils.EMPTY, parts[3]));
            case WITH_CLASSIFIER ->
                Optional.of(new ArtifactCoordinates(parts[0], parts[1], parts[2], parts[3], parts[4]));
            default -> Optional.empty();
        };
    }

    public static ArtifactCoordinates of(MavenArtifact artifact)
    {
        return new ArtifactCoordinates(
            artifact.getGroupId(),
            artifact.getArtifactId(),
            StringUtils.defaultIfBlank(artifact.getExtension(), DEFAULT_EXTENSION),
            StringUtils.defaultString(artifact.getClassifier()),
            artifact.getVersion());
    }

    public static ArtifactCoordinates of(MavenPlugin plugin)
    {
        return new ArtifactCoordinates(
            StringUtils.defaultIfBlank(plugin.getGroupId(), DEFAULT_PLUGIN_GROUP_ID),
            plugin.getArtifactId(),
            DEFAULT_EXTENSION,
            StringUtils.EMPTY,
            StringUtils.defaultString(plugin.getVersion()));
    }

    public String baseVersion()
    {
        Matcher timestamped = TIMESTAMPED_SNAPSHOT.matcher(StringUtils.defaultString(version));

        return timestamped.matches() ? (timestamped.group(1) + SNAPSHOT_SUFFIX) : version;
    }

    public Path directoryIn(Path localRepository)
    {
        return localRepository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(baseVersion());
    }

    public String fileName()
    {
        return fileName(classifier, extension);
    }

    public String fileName(String withClassifier, String withExtension)
    {
        return artifactId
            + '-'
            + version
            + (StringUtils.isBlank(withClassifier) ? StringUtils.EMPTY : ('-' + withClassifier))
            + '.'
            + withExtension;
    }

    public String remotePath()
    {
        return groupPath() + '/' + artifactId + '/' + baseVersion() + '/' + fileName();
    }

    public String metadataPath()
    {
        return groupPath() + '/' + artifactId + '/' + METADATA_FILE;
    }

    private String groupPath()
    {
        return groupId.replace('.', '/');
    }

    public String artifactKey()
    {
        return groupId + ':' + artifactId;
    }

    public String shortForm()
    {
        return groupId + ':' + artifactId + ':' + version;
    }

    @Override
    public String toString()
    {
        return groupId
            + ':'
            + artifactId
            + ':'
            + extension
            + (StringUtils.isBlank(classifier) ? StringUtils.EMPTY : (':' + classifier))
            + ':'
            + version;
    }
}
