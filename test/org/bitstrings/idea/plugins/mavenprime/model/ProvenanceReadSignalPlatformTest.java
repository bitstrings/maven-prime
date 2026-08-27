package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.ArrayList;
import java.util.List;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;

public class ProvenanceReadSignalPlatformTest
    extends BasePlatformTestCase
{
    private final List<String> signals = new ArrayList<>();

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        getProject()
            .getMessageBus()
            .connect(getTestRootDisposable())
            .subscribe(
                ProvenanceLog.READ_TOPIC,
                (ProvenanceLog.ProvenanceReadListener) () -> signals.add("read"));
    }

    public void testStartBuild_aReadThatOnlyJustBegan_reportsNothingReadYet()
    {
        ProvenanceLog.getInstance(getProject()).startBuild(getName(), "");

        UIUtil.dispatchAllInvocationEvents();

        assertEquals(
            "a listener that rereads the model on this signal would restart the read that raised it",
            List.of(),
            signals);
    }

    public void testBuildFinished_aReadThatCompleted_reportsTheModelReadable()
    {
        ProvenanceLog log = ProvenanceLog.getInstance(getProject());

        log.startBuild(getName(), "");
        log.buildFinished();

        UIUtil.dispatchAllInvocationEvents();

        assertEquals(List.of("read"), signals);
    }
}
