package org.bitstrings.idea.plugins.mavenprime.repository;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.intellij.openapi.progress.ProgressManager;

public final class LocalRepositoryScan
{
    static final String PREFERRED_EXTENSION = "jar";

    static final String DESCRIPTOR_EXTENSION = "pom";

    private static final int COORDINATE_SEGMENTS = 3;

    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    private static final Pattern SNAPSHOT_TIMESTAMP = Pattern.compile("-\\d{8}\\.\\d{6}-\\d+");

    private static final List<String> SIDECAR_SUFFIXES =
        List.of(".sha1", ".sha256", ".sha512", ".md5", ".asc");

    private static final List<String> INCOMPLETE_SUFFIXES = List.of(".lastUpdated", ".part");

    private LocalRepositoryScan()
    {
    }

    public static List<ArtifactCoordinates> scan(Path localRepository)
    {
        List<ArtifactCoordinates> found = new ArrayList<>();

        if (!Files.isDirectory(localRepository))
        {
            return found;
        }

        try
        {
            Path root = localRepository.toRealPath();

            Files.walkFileTree(root, new VersionDirectoryVisitor(root, found::add));
        }
        catch (IOException unreadable)
        {
            return found;
        }

        found.sort(ArtifactCoordinates.BY_ARTIFACT);

        return found;
    }

    static String extensionOf(String fileName, String prefix)
    {
        if (isIncomplete(fileName))
        {
            return StringUtils.EMPTY;
        }

        String rest = stripSidecars(fileName.substring(prefix.length()));

        return StringUtils.substringAfterLast(rest, GROUP_SEPARATOR);
    }

    static boolean isIncomplete(String fileName)
    {
        return INCOMPLETE_SUFFIXES.stream().anyMatch(fileName::endsWith);
    }

    private static String stripSidecars(String rest)
    {
        String stripped = rest;

        for (boolean shortened = true; shortened;)
        {
            shortened = false;

            for (String suffix : SIDECAR_SUFFIXES)
            {
                if (stripped.endsWith(suffix))
                {
                    stripped = stripped.substring(0, stripped.length() - suffix.length());

                    shortened = true;
                }
            }
        }

        return stripped;
    }

    static String preferred(String current, String candidate)
    {
        if (StringUtils.isBlank(candidate))
        {
            return current;
        }

        if (current == null)
        {
            return candidate;
        }

        int byRank = Integer.compare(rankOf(candidate), rankOf(current));

        return ((byRank < 0) || ((byRank == 0) && (candidate.compareTo(current) < 0))) ? candidate : current;
    }

    private static int rankOf(String extension)
    {
        if (PREFERRED_EXTENSION.equals(extension))
        {
            return 0;
        }

        return DESCRIPTOR_EXTENSION.equals(extension) ? 1 : 2;
    }

    private static final class VersionDirectoryVisitor
        extends SimpleFileVisitor<Path>
    {
        private final Path root;

        private final Consumer<ArtifactCoordinates> sink;

        private final Deque<Candidate> stack = new ArrayDeque<>();

        VersionDirectoryVisitor(Path root, Consumer<ArtifactCoordinates> sink)
        {
            this.root = root;
            this.sink = sink;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
        {
            ProgressManager.checkCanceled();

            stack.push(Candidate.of(root, directory));

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
        {
            stack.peek().offer(file.getFileName().toString());

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path directory, IOException failure)
        {
            stack.pop().emit(sink);

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException failure)
        {
            return FileVisitResult.CONTINUE;
        }
    }

    private static final class Candidate
    {
        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String prefix;

        private final String snapshotPrefix;

        private String extension;

        private Candidate(String groupId, String artifactId, String version)
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.prefix = (groupId == null) ? null : (artifactId + '-' + version);
            this.snapshotPrefix = (groupId == null) ? null : snapshotPrefixOf(artifactId, version);
        }

        private static String snapshotPrefixOf(String artifactId, String version)
        {
            return version.endsWith(SNAPSHOT_SUFFIX)
                ? (artifactId + '-' + version.substring(0, version.length() - SNAPSHOT_SUFFIX.length()))
                : null;
        }

        static Candidate of(Path root, Path directory)
        {
            Path relative = root.relativize(directory);

            int segments = relative.getNameCount();

            if ((segments < COORDINATE_SEGMENTS) || StringUtils.isEmpty(relative.toString()))
            {
                return new Candidate(null, null, null);
            }

            return new Candidate(
                relative
                    .subpath(0, segments - 2)
                    .toString()
                    .replace(PATH_SEPARATOR, GROUP_SEPARATOR)
                    .replace('\\', GROUP_SEPARATOR),
                relative.getName(segments - 2).toString(),
                relative.getName(segments - 1).toString());
        }

        void offer(String fileName)
        {
            String matched = prefixOf(fileName);

            if (matched != null)
            {
                extension = preferred(extension, extensionOf(fileName, matched));
            }
        }

        private String prefixOf(String fileName)
        {
            if (prefix == null)
            {
                return null;
            }

            return fileName.startsWith(prefix) ? prefix : timestampedPrefixOf(fileName);
        }

        private String timestampedPrefixOf(String fileName)
        {
            if ((snapshotPrefix == null) || !fileName.startsWith(snapshotPrefix))
            {
                return null;
            }

            Matcher timestamp = SNAPSHOT_TIMESTAMP.matcher(fileName.substring(snapshotPrefix.length()));

            return timestamp.lookingAt() ? (snapshotPrefix + timestamp.group()) : null;
        }

        void emit(Consumer<ArtifactCoordinates> sink)
        {
            if (extension != null)
            {
                sink.accept(
                    new ArtifactCoordinates(groupId, artifactId, extension, StringUtils.EMPTY, version));
            }
        }
    }
}
