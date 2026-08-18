package org.bitstrings.idea.plugins.mavenprime.build;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvents;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceData;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceLog;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfileService;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvents;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvents;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionData;
import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionLog;
import org.bitstrings.idea.plugins.mavenprime.util.BuildCompleteness;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;

public final class SpySession
    implements Disposable
{
    private static final long DRAIN_TIMEOUT_MILLIS = 2000L;

    private final SpyScope scope;

    private final BuildProfileService profiles;

    private final ResolutionLog resolutions;

    private final ProvenanceLog provenances;

    private final ResolutionData resolutionData;

    private final ProvenanceData provenanceData;

    private final BuildProfile profile;

    private final String buildName;

    private final Consumer<String> additional;

    private final SpyServer server;

    private final String token = UUID.randomUUID().toString();

    private final AtomicBoolean finished = new AtomicBoolean();

    private SpySession(Project project, SpyScope scope, String buildName, Consumer<String> additional)
    {
        this.scope = scope;
        this.profiles = BuildProfileService.getInstance(project);
        this.resolutions = ResolutionLog.getInstance(project);
        this.provenances = ProvenanceLog.getInstance(project);
        this.buildName = StringUtils.defaultString(buildName);
        this.additional = additional;
        this.profile = scope.isTimed() ? profiles.startBuild(buildName) : null;
        this.resolutionData = scope.collectsResolution() ? resolutions.startBuild(buildName) : null;
        this.provenanceData = provenances.startBuild(buildName);

        this.server = SpyServer.getInstance(project);

        server.register(token, this::accept);

        Disposer.register(project, this);
    }

    public static SpySession timed(Project project, String buildName, Consumer<String> additional)
    {
        return new SpySession(project, SpyScope.BUILD, buildName, additional);
    }

    public static SpySession untimed(Project project, String buildName)
    {
        return new SpySession(project, SpyScope.MODEL, buildName, null);
    }

    static SpySession adopt(Project project, String buildName)
    {
        return new SpySession(project, SpyScope.BUILD, buildName, null);
    }

    public SpyHandshake handshake()
    {
        return new SpyHandshake(server.getPort(), token, buildName);
    }

    public String getToken()
    {
        return token;
    }

    public void finish()
    {
        finish(EmptyRunnable.getInstance());
    }

    public void finish(Runnable afterDrain)
    {
        if (!finished.compareAndSet(false, true))
        {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> drainAndPublish(afterDrain));
    }

    @Override
    public void dispose()
    {
        finish();
    }

    void accept(String line)
    {
        long dropped = SpyEvents.droppedCount(line);

        if (dropped > 0L)
        {
            eachCompleteness(completeness -> completeness.recordDropped(dropped));
        }

        if (profile != null)
        {
            ProfileEvents.parse(line).ifPresent(profile::accept);
        }

        if (resolutionData != null)
        {
            ResolutionEvents.parse(line).ifPresent(resolutionData::accept);
        }

        ProvenanceEvents.parse(line).ifPresent(provenanceData::accept);

        if (additional != null)
        {
            additional.accept(line);
        }
    }

    private void eachCompleteness(Consumer<BuildCompleteness> operation)
    {
        if (profile != null)
        {
            operation.accept(profile.getCompleteness());
        }

        if (resolutionData != null)
        {
            operation.accept(resolutionData.getCompleteness());
        }

        operation.accept(provenanceData.getCompleteness());
    }

    void recordDrainOutcome(boolean completed)
    {
        if (!completed)
        {
            eachCompleteness(BuildCompleteness::recordTruncated);
        }
    }

    private void drainAndPublish(Runnable afterDrain)
    {
        recordDrainOutcome(server.awaitCompletion(token, DRAIN_TIMEOUT_MILLIS));

        server.unregister(token);

        afterDrain.run();

        if (profile != null)
        {
            profiles.buildFinished();
        }

        if (resolutionData != null)
        {
            resolutions.buildFinished();
        }

        provenances.buildFinished();

        Disposer.dispose(this);
    }
}
