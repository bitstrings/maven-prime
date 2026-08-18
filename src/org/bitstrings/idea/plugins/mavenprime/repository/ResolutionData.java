package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactDownloading;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactFailed;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.MetadataFailed;
import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;

public final class ResolutionData
{
    private static final int MAX_RETAINED = 500;

    private final Object lock = new Object();

    private final Map<String, ResolutionEvent> failures = new LinkedHashMap<>();

    private final Map<String, ArtifactDownloading> downloads = new LinkedHashMap<>();

    private final BuildCompleteness completeness = new BuildCompleteness();

    public BuildCompleteness getCompleteness()
    {
        return completeness;
    }

    public void accept(ResolutionEvent event)
    {
        synchronized (lock)
        {
            switch (event)
            {
                case ArtifactDownloading downloading -> retain(downloads, downloading.coordinates(), downloading);
                case ArtifactFailed failed -> retain(failures, failed.coordinates(), failed);
                case MetadataFailed failed -> retain(failures, failed.coordinates(), failed);
            }
        }
    }

    public List<ResolutionEvent> getFailures()
    {
        synchronized (lock)
        {
            return List.copyOf(failures.values());
        }
    }

    public List<ArtifactDownloading> getDownloads()
    {
        synchronized (lock)
        {
            return List.copyOf(downloads.values());
        }
    }

    private static <T extends ResolutionEvent> void retain(Map<String, T> sink, String key, T event)
    {
        if (!sink.containsKey(key) && (sink.size() >= MAX_RETAINED))
        {
            Iterator<String> oldest = sink.keySet().iterator();

            oldest.next();
            oldest.remove();
        }

        sink.put(key, event);
    }
}
