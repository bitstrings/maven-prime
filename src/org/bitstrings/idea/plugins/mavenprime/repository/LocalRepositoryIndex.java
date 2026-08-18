package org.bitstrings.idea.plugins.mavenprime.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

@Service(Service.Level.PROJECT)
public final class LocalRepositoryIndex
{
    public static final Topic<IndexListener> TOPIC =
        Topic.create("Maven Prime local repository index", IndexListener.class);

    public static final int DEFAULT_LIMIT = 300;

    private final Project project;

    private final Object scanLock = new Object();

    private final AtomicBoolean scanning = new AtomicBoolean();

    private volatile Path indexedRoot;

    private volatile IndexedArtifacts indexed = IndexedArtifacts.EMPTY;

    public LocalRepositoryIndex(Project project)
    {
        this.project = project;
    }

    public static LocalRepositoryIndex getInstance(Project project)
    {
        return project.getService(LocalRepositoryIndex.class);
    }

    public boolean isScanning()
    {
        return scanning.get();
    }

    public int size()
    {
        return indexed.all().size();
    }

    public void ensureIndexed(Path localRepository)
    {
        if (!localRepository.equals(indexedRoot))
        {
            rebuild(localRepository);
        }
    }

    public void rebuild(Path localRepository)
    {
        if (!scanning.compareAndSet(false, true))
        {
            return;
        }

        ProgressManager
            .getInstance()
            .run(
                new Task.Backgroundable(
                    project, MavenPrimeBundle.message("mavenprime.repository.indexing"), true)
                {
                    @Override
                    public void run(ProgressIndicator indicator)
                    {
                        scan(localRepository);
                    }
                });
    }

    public List<ArtifactCoordinates> search(String fragment, int limit)
    {
        String needle = StringUtils.trimToEmpty(fragment);

        return needle.isEmpty() ? List.of() : ArtifactRanking.bestMatches(indexed.all(), needle, limit);
    }

    public List<ArtifactCoordinates> versionsOf(ArtifactCoordinates coordinates)
    {
        return indexed.versionsOf(coordinates);
    }

    private void scan(Path localRepository)
    {
        scanning.set(true);

        try
        {
            synchronized (scanLock)
            {
                if (localRepository.equals(indexedRoot))
                {
                    return;
                }

                indexed = IndexedArtifacts.of(LocalRepositoryScan.scan(localRepository));
                indexedRoot = localRepository;
            }
        }
        finally
        {
            scanning.set(false);

            UiTopics.publish(project, TOPIC, IndexListener::indexUpdated);
        }
    }

    public interface IndexListener
    {
        void indexUpdated();
    }
}
