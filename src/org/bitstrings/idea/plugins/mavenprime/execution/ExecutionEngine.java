package org.bitstrings.idea.plugins.mavenprime.execution;

import org.bitstrings.idea.plugins.mavenprime.distribution.MavenInstallation;

import com.intellij.execution.RunnerAndConfigurationSettings;

public interface ExecutionEngine
{
    RunnerAndConfigurationSettings createSettings(MavenPrimeRequest request, MavenInstallation installation);
}
