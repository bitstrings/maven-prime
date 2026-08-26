package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.model.ModelStaleness;
import org.bitstrings.idea.plugins.mavenprime.tests.IdeReloadPrompt;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.ui.UIUtil;

public class ToolWindowStaleMarkerPlatformTest
    extends BasePlatformTestCase
{
    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            ModelStaleness.getInstance(getProject()).provenanceRead();
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testMarkModelRead_noReadOwed_leavesTheModelTabUnmarked()
    {
        ModelStaleness.getInstance(getProject()).provenanceRead();

        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            assertNull(modelTabOf(panel).getIcon());
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    public void testMarkModelRead_aReadOwed_marksTheModelTabFromAnyOtherTab()
    {
        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            raise();

            panel.updateStaleMarker();

            assertNotNull(
                "the marker is the only thing that reports a stale model without opening the Model tab",
                modelTabOf(panel).getIcon());
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    public void testMarkModelRead_theReadDone_clearsTheMarker()
    {
        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            raise();

            panel.updateStaleMarker();

            ModelStaleness.getInstance(getProject()).provenanceRead();

            panel.updateStaleMarker();

            assertNull(modelTabOf(panel).getIcon());
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    public void testMarkModelRead_pomSourcesChangingWhileAnotherTabIsOpen_marksWithoutBeingAsked()
    {
        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            raise();

            UIUtil.dispatchAllInvocationEvents();

            assertNotNull(
                "nothing calls the panel when a pom changes, so a marker that only updates on demand "
                    + "stays dark exactly when the reader needs it",
                modelTabOf(panel).getIcon());
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    public void testMarkModelRead_theIdeRaisingItsOwnPomPrompt_marksTheModelTabTheSameMoment()
    {
        ModelStaleness.getInstance(getProject()).provenanceRead();

        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            IdeReloadPrompt.raise(getProject());

            UIUtil.dispatchAllInvocationEvents();

            assertNotNull(
                "the IDE knows a pom changed before any import runs, and that is the moment the reader "
                    + "needs to be told the model no longer matches",
                modelTabOf(panel).getIcon());
        }
        finally
        {
            IdeReloadPrompt.clear(getProject());

            Disposer.dispose(panel);
        }
    }

    public void testUpdateStaleMarker_aToolWindowOpenedAfterTheEdit_showsTheMarkerOnFirstPaint()
    {
        raise();

        MavenPrimeToolWindowPanel panel = new MavenPrimeToolWindowPanel(getProject());

        try
        {
            assertNotNull(
                "opening the tool window is exactly when the reader looks, and a marker that waits for "
                    + "the next event is dark at that moment",
                modelTabOf(panel).getIcon());
        }
        finally
        {
            Disposer.dispose(panel);
        }
    }

    private void raise()
    {
        ModelStaleness.getInstance(getProject()).provenanceRead();

        UIUtil.dispatchAllInvocationEvents();

        ModelStaleness.getInstance(getProject()).pomSourcesChanged();
    }

    private static TabInfo modelTabOf(MavenPrimeToolWindowPanel panel)
    {
        return panel
            .tabs
            .getTabs()
            .stream()
            .filter(info -> info.getObject() == ToolWindowTab.MODEL)
            .findFirst()
            .orElseThrow();
    }
}
