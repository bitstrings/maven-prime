package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCacheStatus;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.bitstrings.idea.plugins.mavenprime.repository.LocalRepositoryIndex;
import org.bitstrings.idea.plugins.mavenprime.repository.RemoteArtifactIndex;
import org.bitstrings.idea.plugins.mavenprime.repository.RemoteAvailability;
import org.bitstrings.idea.plugins.mavenprime.repository.RemoteProbeResult;
import org.bitstrings.idea.plugins.mavenprime.repository.RepositoryAttempt;
import org.bitstrings.idea.plugins.mavenprime.repository.RepositoryDiagnosis;
import org.bitstrings.idea.plugins.mavenprime.repository.RepositoryFiles;
import org.bitstrings.idea.plugins.mavenprime.ui.DetailsPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.SimpleTextAttributes;

public final class RepositoryDetailsView
{
    private static final int MAX_LISTED_VERSIONS = 12;

    private final Project project;

    private final DetailsPanel panel = new DetailsPanel();

    private RepositoryDiagnosis diagnosed;

    private ArtifactCoordinates remoteSubject;

    private List<RemoteProbeResult> remoteProbes = List.of();

    private List<String> remoteVersions = List.of();

    public RepositoryDetailsView(Project project)
    {
        this.project = project;
    }

    public JComponent getComponent()
    {
        return panel;
    }

    public RepositoryDiagnosis getDiagnosis()
    {
        return diagnosed;
    }

    public ArtifactCoordinates getRemoteSubject()
    {
        return remoteSubject;
    }

    public void clearRemote()
    {
        remoteSubject = null;
        remoteProbes = List.of();
        remoteVersions = List.of();
    }

    public void setRemote(
        ArtifactCoordinates subject, List<RemoteProbeResult> probes, List<String> versions)
    {
        remoteSubject = subject;
        remoteProbes = List.copyOf(probes);
        remoteVersions = List.copyOf(versions);
    }

    public RemoteProbeResult firstUnauthorized()
    {
        if ((diagnosed == null) || !diagnosed.status().coordinates().equals(remoteSubject))
        {
            return null;
        }

        for (RemoteProbeResult probe : remoteProbes)
        {
            if (probe.availability() == RemoteAvailability.UNAUTHORIZED)
            {
                return probe;
            }
        }

        return null;
    }

    public void showHint()
    {
        diagnosed = null;

        panel.clear();
        panel.note(
            null,
            MavenPrimeBundle.message("mavenprime.repository.hint"),
            SimpleTextAttributes.GRAYED_ATTRIBUTES);
        panel.done();
    }

    public void show(RepositoryDiagnosis inspected)
    {
        diagnosed = inspected;

        panel.clear();
        panel.title(inspected.status().coordinates().toString());

        showVerdict();
        showFiles();
        showVersions();
        showAttempts();
        showRemote();

        panel.done();
    }

    private void showVerdict()
    {
        ArtifactCacheStatus status = diagnosed.status();

        panel.section(MavenPrimeBundle.message("mavenprime.repository.section.status"));

        switch (status.verdict())
        {
            case RESOLVED ->
                panel.note(
                    AllIcons.General.InspectionsOK,
                    MavenPrimeBundle.message(
                        "mavenprime.repository.verdict.resolved",
                        StringUtils.defaultIfBlank(
                            status.originRepository(),
                            MavenPrimeBundle.message("mavenprime.repository.origin.local"))),
                    SimpleTextAttributes.REGULAR_ATTRIBUTES);
            case NEVER_ATTEMPTED ->
                panel.note(
                    AllIcons.General.BalloonInformation,
                    MavenPrimeBundle.message("mavenprime.repository.verdict.neverAttempted"),
                    SimpleTextAttributes.GRAYED_ATTRIBUTES);
            case NOT_FOUND ->
                panel.note(
                    AllIcons.General.ShowWarning,
                    MavenPrimeBundle.message("mavenprime.repository.verdict.notFound"),
                    SimpleTextAttributes.REGULAR_ATTRIBUTES);
            case TRANSFER_FAILED ->
                panel.note(
                    AllIcons.General.BalloonError,
                    MavenPrimeBundle.message("mavenprime.repository.verdict.transferFailed"),
                    SimpleTextAttributes.ERROR_ATTRIBUTES);
        }

        if (status.isNegativelyCached())
        {
            status
                .lastAttemptMillis()
                .ifPresent(
                    millis ->
                        panel.note(
                            null,
                            MavenPrimeBundle.message(
                                "mavenprime.repository.negativeCache", new Date(millis)),
                            SimpleTextAttributes.REGULAR_ATTRIBUTES));
        }
    }

    private void showFiles()
    {
        RepositoryFiles files = diagnosed.files();

        panel.section(MavenPrimeBundle.message("mavenprime.repository.section.files"));

        if (files.sizeBytes() > 0L)
        {
            panel.row(
                null,
                MavenPrimeBundle.message("mavenprime.repository.label.size"),
                Formats.formatFileSize(files.sizeBytes()),
                SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        showSidecar(
            "mavenprime.repository.files.sources.present",
            "mavenprime.repository.files.sources.missing",
            files.sources());
        showSidecar(
            "mavenprime.repository.files.javadoc.present",
            "mavenprime.repository.files.javadoc.missing",
            files.javadoc());

        panel.row(
            null,
            MavenPrimeBundle.message("mavenprime.repository.label.directory"),
            String.valueOf(diagnosed.status().directory()),
            SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    private void showSidecar(String presentKey, String missingKey, Path file)
    {
        panel.note(
            (file == null) ? AllIcons.General.BalloonInformation : AllIcons.General.InspectionsOK,
            MavenPrimeBundle.message((file == null) ? missingKey : presentKey),
            (file == null)
                ? SimpleTextAttributes.GRAYED_ATTRIBUTES
                : SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    private void showVersions()
    {
        panel.section(MavenPrimeBundle.message("mavenprime.repository.section.versions"));

        List<String> localVersions = new ArrayList<>();

        for (ArtifactCoordinates version :
            LocalRepositoryIndex.getInstance(project).versionsOf(diagnosed.status().coordinates()))
        {
            localVersions.add(version.version());
        }

        panel.row(
            null,
            MavenPrimeBundle.message("mavenprime.repository.label.onDisk"),
            summarize(localVersions),
            SimpleTextAttributes.REGULAR_ATTRIBUTES);

        List<String> indexed = diagnosed.indexedVersions();

        if (indexed.isEmpty())
        {
            panel.row(
                null,
                MavenPrimeBundle.message("mavenprime.repository.label.indexed"),
                MavenPrimeBundle.message("mavenprime.repository.versions.notIndexed"),
                SimpleTextAttributes.GRAYED_ATTRIBUTES);

            return;
        }

        panel.row(
            null,
            MavenPrimeBundle.message("mavenprime.repository.label.indexed"),
            summarize(indexed),
            SimpleTextAttributes.REGULAR_ATTRIBUTES);

        String newer =
            RemoteArtifactIndex.newestNewerThan(indexed, diagnosed.status().coordinates().version());

        if (newer != null)
        {
            panel.note(
                AllIcons.General.ShowWarning,
                MavenPrimeBundle.message("mavenprime.repository.versions.newer", newer),
                SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    private void showAttempts()
    {
        List<RepositoryAttempt> attempts = diagnosed.status().attempts();

        if (attempts.isEmpty())
        {
            return;
        }

        panel.section(MavenPrimeBundle.message("mavenprime.repository.section.attempts"));

        for (RepositoryAttempt attempt : attempts)
        {
            panel.note(
                null,
                MavenPrimeBundle.message(
                    attempt.notFound()
                        ? "mavenprime.repository.attempt.notFound"
                        : "mavenprime.repository.attempt.error",
                    attempt.repository(),
                    new Date(attempt.lastUpdatedMillis()),
                    StringUtils.defaultString(attempt.error())),
                SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    private void showRemote()
    {
        if (!diagnosed.status().coordinates().equals(remoteSubject))
        {
            return;
        }

        panel.section(MavenPrimeBundle.message("mavenprime.repository.remote.header"));

        if (remoteProbes.isEmpty())
        {
            panel.note(
                null,
                MavenPrimeBundle.message("mavenprime.repository.remote.none"),
                SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        for (RemoteProbeResult probe : remoteProbes)
        {
            panel.note(
                null,
                probe.getMessage(),
                (probe.availability() == RemoteAvailability.UNAUTHORIZED)
                    ? SimpleTextAttributes.REGULAR_ATTRIBUTES
                    : SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        if (!remoteVersions.isEmpty())
        {
            panel.row(
                null,
                MavenPrimeBundle.message("mavenprime.repository.label.published"),
                summarize(remoteVersions),
                SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    private static String summarize(List<String> versions)
    {
        if (versions.isEmpty())
        {
            return MavenPrimeBundle.message("mavenprime.repository.versions.none");
        }

        List<String> shown = versions.subList(0, Math.min(versions.size(), MAX_LISTED_VERSIONS));

        String listed = String.join(", ", shown);

        return (shown.size() == versions.size())
            ? MavenPrimeBundle.message(
                "mavenprime.repository.versions.all", Integer.valueOf(versions.size()), listed)
            : MavenPrimeBundle.message(
                "mavenprime.repository.versions.truncated", Integer.valueOf(versions.size()), listed);
    }
}
