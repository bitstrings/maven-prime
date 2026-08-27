package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionData;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactDownloading;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactFailed;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionLog;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class RepositorySourcePlatformTest
    extends BasePlatformTestCase
{
    private static final ArtifactDownloading DOWNLOAD =
        new ArtifactDownloading("commons-io:commons-io:jar:2.16.1", "central", "https://example.invalid");

    private static final ArtifactFailed FAILURE =
        new ArtifactFailed("org.example:missing:jar:1.0", "central", "not found");

    public void testRecordedIn_downloads_readsWhatTheBuildFetched()
    {
        ResolutionLog log = logWithBothRecorded();

        assertEquals(List.of(DOWNLOAD), RepositorySource.DOWNLOADS.recordedIn(log));
    }

    public void testRecordedIn_failures_readsWhatTheBuildCouldNotResolve()
    {
        ResolutionLog log = logWithBothRecorded();

        assertEquals(List.of(FAILURE), RepositorySource.FAILURES.recordedIn(log));
    }

    public void testRecordedIn_aSourceNoBuildFeeds_isEmptyRatherThanSomebodyElsesList()
    {
        ResolutionLog log = logWithBothRecorded();

        assertEquals(List.of(), RepositorySource.SEARCH.recordedIn(log));
    }

    private ResolutionLog logWithBothRecorded()
    {
        ResolutionLog log = ResolutionLog.getInstance(getProject());

        ResolutionData recording = log.startBuild(getName(), "");

        recording.accept(DOWNLOAD);
        recording.accept(FAILURE);

        return log;
    }
}
