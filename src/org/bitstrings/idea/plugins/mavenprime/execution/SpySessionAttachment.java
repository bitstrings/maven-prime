package org.bitstrings.idea.plugins.mavenprime.execution;

import org.bitstrings.idea.plugins.mavenprime.build.SpySession;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;

final class SpySessionAttachment
    implements ExecutionListener
{
    private final RunProfile profile;

    private final SpySession session;

    private final MessageBusConnection connection;

    private SpySessionAttachment(RunProfile profile, SpySession session, MessageBusConnection connection)
    {
        this.profile = profile;
        this.session = session;
        this.connection = connection;
    }

    static SpySessionAttachment attach(Project project, RunProfile profile, SpySession session)
    {
        MessageBusConnection connection = project.getMessageBus().connect(project);

        SpySessionAttachment attachment = new SpySessionAttachment(profile, session, connection);

        connection.subscribe(ExecutionManager.EXECUTION_TOPIC, attachment);

        return attachment;
    }

    void detach()
    {
        connection.disconnect();
    }

    @Override
    public void processNotStarted(String executorId, ExecutionEnvironment environment)
    {
        close(environment);
    }

    @Override
    public void processNotStarted(String executorId, ExecutionEnvironment environment, Throwable cause)
    {
        close(environment);
    }

    @Override
    public void processTerminated(
        String executorId, ExecutionEnvironment environment, ProcessHandler handler, int exitCode)
    {
        close(environment);
    }

    private void close(ExecutionEnvironment environment)
    {
        if (environment.getRunProfile() != profile)
        {
            return;
        }

        connection.disconnect();

        session.finish();
    }
}
