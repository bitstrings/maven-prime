package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.List;
import java.util.function.Function;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactKind;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionLog;

public enum RepositorySource
{
    SEARCH("search", ArtifactKind.DEPENDENCY, null),
    INDEX("index", ArtifactKind.DEPENDENCY, null),
    MODULE("module", ArtifactKind.DEPENDENCY, null),
    PLUGINS("plugins", ArtifactKind.PLUGIN, null),
    FAILURES("failures", ArtifactKind.DEPENDENCY, ResolutionLog::getFailures),
    DOWNLOADS("downloads", ArtifactKind.DEPENDENCY, ResolutionLog::getDownloads);

    private final String bundleKey;

    private final ArtifactKind kind;

    private final transient Function<ResolutionLog, List<? extends ResolutionEvent>> recorded;

    RepositorySource(
        String bundleKey, ArtifactKind kind, Function<ResolutionLog, List<? extends ResolutionEvent>> recorded)
    {
        this.bundleKey = bundleKey;
        this.kind = kind;
        this.recorded = recorded;
    }

    public ArtifactKind getKind()
    {
        return kind;
    }

    public boolean isFromResolutionLog()
    {
        return recorded != null;
    }

    public List<? extends ResolutionEvent> recordedIn(ResolutionLog log)
    {
        return isFromResolutionLog() ? recorded.apply(log) : List.of();
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.repository.source." + bundleKey);
    }

    public String getEmptyText()
    {
        return MavenPrimeBundle.message("mavenprime.repository.empty." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
