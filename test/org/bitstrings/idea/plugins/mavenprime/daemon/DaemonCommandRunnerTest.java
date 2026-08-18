package org.bitstrings.idea.plugins.mavenprime.daemon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.intellij.execution.process.ProcessOutput;

public class DaemonCommandRunnerTest
{
    private static final String STATUS_TABLE = "ID  PID  Address  Status";

    private static final String JLINE_NOISE =
        "WARNING: Unable to create a system terminal, creating a dumb terminal";

    @Test
    public void reportOf_aSuccessfulCommand_showsOnlyWhatTheCommandPrinted()
    {
        assertEquals(STATUS_TABLE, DaemonCommandRunner.reportOf(output(0, STATUS_TABLE, JLINE_NOISE)));
    }

    @Test
    public void reportOf_aFailedCommand_keepsStderrSoTheCauseIsVisible()
    {
        assertTrue(DaemonCommandRunner.reportOf(output(1, "", "mvnd: command failed")).contains("failed"));
    }

    private static ProcessOutput output(int exitCode, String stdout, String stderr)
    {
        ProcessOutput output = new ProcessOutput();

        output.appendStdout(stdout);
        output.appendStderr(stderr);
        output.setExitCode(exitCode);

        return output;
    }
}
