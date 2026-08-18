package org.bitstrings.idea.plugins.mavenprime.run;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.util.net.NetUtils;

public final class MavenPrimeTestDebugRunner
    extends GenericDebuggerRunner
{
    static final String RUNNER_ID = "MavenPrimeTestDebug";

    private static final String LISTEN_HOST = "127.0.0.1";

    @Override
    public String getRunnerId()
    {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile)
    {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)
            && (profile instanceof MavenPrimeRunConfiguration)
            && ((MavenPrimeRunConfiguration) profile).getRequest().selectsTests();
    }

    @Override
    protected RunContentDescriptor createContentDescriptor(
        RunProfileState state, ExecutionEnvironment environment)
        throws ExecutionException
    {
        if (!(state instanceof MavenPrimeRunProfileState))
        {
            return super.createContentDescriptor(state, environment);
        }

        int port = NetUtils.tryToFindAvailableSocketPort();

        if (port <= 0)
        {
            throw new ExecutionException(MavenPrimeBundle.message("mavenprime.test.debug.error.noPort"));
        }

        ((MavenPrimeRunProfileState) state).debugForkedTestsAt(LISTEN_HOST, port);

        return attachVirtualMachine(
            state,
            environment,
            new RemoteConnection(true, LISTEN_HOST, Integer.toString(port), true),
            false);
    }
}
