package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.model.ModelStaleness;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ModelStaleBannerPlatformTest
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

    public void testRenderStale_noReadOwed_keepsTheBannerOutOfTheWay()
    {
        ModelStaleness.getInstance(getProject()).provenanceRead();

        assertFalse(panel().stale.isVisible());
    }

    public void testRenderStale_aReadOwed_saysWhatChangedWithoutOfferingOurOwnRead()
    {
        raise();

        ModelProvenancePanel panel = panel();

        panel.renderStale();

        assertTrue(panel.stale.isVisible());
        assertEquals(
            "our read is not what reconciles the model with the poms, so offering it sends the reader "
                + "somewhere that changes nothing",
            MavenPrimeBundle.message("mavenprime.model.stale"),
            panel.stale.getCharSequence(false).toString());
    }

    public void testRenderStale_aReadOwedThenDone_takesTheBannerBackDown()
    {
        raise();

        ModelProvenancePanel panel = panel();

        panel.renderStale();

        ModelStaleness.getInstance(getProject()).provenanceRead();

        panel.renderStale();

        assertFalse(panel.stale.isVisible());
    }

    public void testRenderStale_aModelTabOpenedAfterTheEdit_showsTheBannerOnFirstPaint()
    {
        raise();

        assertTrue(
            "opening the tab is when the reader looks, and a banner that waits for the next event is "
                + "invisible at that moment",
            panel().stale.isVisible());
    }

    private void raise()
    {
        ModelStaleness.getInstance(getProject()).provenanceRead();
        ModelStaleness.getInstance(getProject()).pomSourcesChanged();
    }

    private ModelProvenancePanel panel()
    {
        ModelProvenancePanel panel = new ModelProvenancePanel(getProject(), () -> null);

        Disposer.register(getTestRootDisposable(), panel);

        return panel;
    }
}
