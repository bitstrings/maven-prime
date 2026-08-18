package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.io.Decompressor;
import com.intellij.util.io.HttpRequests;

public final class MvndInstaller
{
    private static final String DIGEST_ALGORITHM = "SHA-512";

    private static final String STAGING_PREFIX = ".download-";

    private static final Logger LOG = Logger.getInstance(MvndInstaller.class);

    private MvndInstaller()
    {
    }

    public static Path install(MvndDownload download, Path userHome, ProgressIndicator indicator)
        throws IOException
    {
        Path installed = MvndInstallations.rootIn(userHome).resolve(download.version());

        if (MvndLayout.isDaemonHome(installed))
        {
            return installed;
        }

        Files.createDirectories(installed.getParent());

        // Staged beside the destination, not in the system temp dir, so the final move is atomic.
        Path staging = Files.createTempDirectory(installed.getParent(), STAGING_PREFIX);

        try
        {
            Path archive = staging.resolve(download.archiveName());

            verify(archive, fetch(download, archive, indicator));

            Files.move(
                unpack(archive, staging),
                installed,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

            return installed;
        }
        finally
        {
            discard(staging);
        }
    }

    private static String fetch(MvndDownload download, Path archive, ProgressIndicator indicator)
        throws IOException
    {
        IOException lastFailure = null;

        for (String url : download.urls())
        {
            try
            {
                HttpRequests.request(url).saveToFile(archive.toFile(), indicator);

                return url;
            }
            catch (IOException unreachable)
            {
                lastFailure = unreachable;
            }
        }

        throw (lastFailure == null) ? new IOException(download.archiveName()) : lastFailure;
    }

    private static void verify(Path archive, String source)
        throws IOException
    {
        String published = StringUtils.substringBefore(
            HttpRequests.request(MvndDownload.checksumUrl(source)).readString().trim(), " ");

        String actual = digestOf(archive);

        if (!actual.equalsIgnoreCase(published.trim()))
        {
            throw new IOException(
                MavenPrimeBundle.message("mavenprime.daemon.download.checksum", source));
        }
    }

    private static String digestOf(Path archive)
        throws IOException
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);

            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(archive)));
        }
        catch (NoSuchAlgorithmException unsupported)
        {
            throw new IOException(DIGEST_ALGORITHM, unsupported);
        }
    }

    private static Path unpack(Path archive, Path staging)
        throws IOException
    {
        Path unpacked = staging.resolve("unpacked");

        new Decompressor.Zip(archive).withZipExtensions().extract(unpacked);

        try (var children = Files.list(unpacked))
        {
            List<Path> roots = children.filter(Files::isDirectory).toList();

            if (roots.size() != 1)
            {
                throw new IOException(MavenPrimeBundle.message("mavenprime.daemon.download.layout"));
            }

            return roots.get(0);
        }
    }

    private static void discard(Path directory)
    {
        if (!Files.exists(directory))
        {
            return;
        }

        try (var paths = Files.walk(directory))
        {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
            {
                Files.deleteIfExists(path);
            }
        }
        catch (IOException undeleted)
        {
            LOG.warn("Maven Prime could not remove the staging directory " + directory, undeleted);
        }
    }
}
