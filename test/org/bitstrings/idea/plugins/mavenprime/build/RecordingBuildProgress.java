package org.bitstrings.idea.plugins.mavenprime.build;

import java.util.ArrayList;
import java.util.List;

import com.intellij.build.FilePosition;
import com.intellij.build.events.MessageEvent;
import com.intellij.build.issue.BuildIssue;
import com.intellij.build.progress.BuildProgress;
import com.intellij.build.progress.BuildProgressDescriptor;
import com.intellij.pom.Navigatable;

public final class RecordingBuildProgress
    implements BuildProgress<BuildProgressDescriptor>
{
    public enum Outcome
    {
        OPEN,
        STARTED,
        FINISHED,
        FAILED,
        CANCELED
    }

    private final String title;

    private final List<RecordingBuildProgress> children = new ArrayList<>();

    private final List<String> messages = new ArrayList<>();

    private Outcome outcome = Outcome.OPEN;

    public RecordingBuildProgress(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public List<RecordingBuildProgress> getChildren()
    {
        return List.copyOf(children);
    }

    public RecordingBuildProgress getChild(int index)
    {
        return children.get(index);
    }

    public List<String> getMessages()
    {
        return List.copyOf(messages);
    }

    public Outcome getOutcome()
    {
        return outcome;
    }

    @Override
    public Object getId()
    {
        return title;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> start(BuildProgressDescriptor descriptor)
    {
        outcome = Outcome.STARTED;

        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> progress(String title)
    {
        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> progress(
        String title, long total, long progress, String unit)
    {
        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> output(String text, boolean stdOut)
    {
        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> message(
        String title, String description, MessageEvent.Kind kind, Navigatable navigatable)
    {
        messages.add(kind + ": " + title + " | " + description);

        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> fileMessage(
        String title, String description, MessageEvent.Kind kind, FilePosition position)
    {
        messages.add(kind + ": " + title + " | " + description);

        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> finish()
    {
        outcome = Outcome.FINISHED;

        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> finish(long timestamp)
    {
        return finish();
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> finish(boolean isUpToDate)
    {
        return finish();
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> finish(long timestamp, boolean isUpToDate, String message)
    {
        return finish();
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> fail()
    {
        outcome = Outcome.FAILED;

        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> fail(long timestamp, String message)
    {
        messages.add("FAIL: " + message);

        return fail();
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> cancel()
    {
        outcome = Outcome.CANCELED;

        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> cancel(long timestamp, String message)
    {
        return cancel();
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> startChildProgress(String title)
    {
        RecordingBuildProgress child = new RecordingBuildProgress(title);

        children.add(child);

        return child;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> buildIssue(BuildIssue issue, MessageEvent.Kind kind)
    {
        return this;
    }
}
