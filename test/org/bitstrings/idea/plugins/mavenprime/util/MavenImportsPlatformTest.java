package org.bitstrings.idea.plugins.mavenprime.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.idea.maven.project.MavenImportListener;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;

public class MavenImportsPlatformTest
    extends BasePlatformTestCase
{
    public void testOnImported_importFinishes_runsTheCallback()
    {
        AtomicInteger runs = new AtomicInteger();

        Disposable parent = Disposer.newDisposable();

        try
        {
            MavenImports.onImported(getProject(), parent, runs::incrementAndGet);

            publishImportFinished();

            assertEquals(1, runs.get());
        }
        finally
        {
            Disposer.dispose(parent);
        }
    }

    public void testOnImported_subscriberDisposed_stopsRunningTheCallback()
    {
        AtomicInteger runs = new AtomicInteger();

        Disposable parent = Disposer.newDisposable();

        MavenImports.onImported(getProject(), parent, runs::incrementAndGet);

        Disposer.dispose(parent);

        publishImportFinished();

        assertEquals("a disposed panel must not be refreshed by a later import", 0, runs.get());
    }

    private void publishImportFinished()
    {
        getProject()
            .getMessageBus()
            .syncPublisher(MavenImportListener.TOPIC)
            .importFinished(List.of(), List.of());

        UIUtil.dispatchAllInvocationEvents();
    }
}
