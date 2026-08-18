package org.bitstrings.idea.plugins.mavenprime.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;

public enum ExecutionMode
{
    RUN,
    DEBUG;

    public Executor getExecutor()
    {
        return (this == DEBUG)
            ? DefaultDebugExecutor.getDebugExecutorInstance()
            : DefaultRunExecutor.getRunExecutorInstance();
    }

    public boolean isDebug()
    {
        return this == DEBUG;
    }
}
