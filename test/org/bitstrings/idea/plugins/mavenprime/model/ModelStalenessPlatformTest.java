package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.tests.IdeReloadPrompt;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;

public class ModelStalenessPlatformTest
    extends BasePlatformTestCase
{
    private final List<String> published = new ArrayList<>();

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        getProject()
            .getMessageBus()
            .connect(getTestRootDisposable())
            .subscribe(
                ModelStaleness.TOPIC,
                (ModelStaleness.StalenessListener) () -> published.add("changed"));
    }

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

    // The service is project scoped and this fixture's project outlives the test, so the starting
    // state is whatever ran before unless each test sets it.
    private void readAndSettle()
    {
        ModelStaleness.getInstance(getProject()).provenanceRead();

        UIUtil.dispatchAllInvocationEvents();

        published.clear();
    }

    public void testIsStale_theIdeHoldingUnloadedPomChanges_agreesWithoutWaitingForTheImport()
    {
        readAndSettle();

        IdeReloadPrompt.raise(getProject());

        try
        {
            assertTrue(
                "the IDE raises its prompt the moment a pom changes, and our model reads the same "
                    + "files, so waiting for the import to finish reports stale too late",
                ModelStaleness.getInstance(getProject()).isStale());
        }
        finally
        {
            IdeReloadPrompt.clear(getProject());
        }
    }

    public void testPomSourcesChanged_anImportThatBroughtNewPomContent_marksTheModelStale()
    {
        readAndSettle();

        ModelStaleness.getInstance(getProject()).pomSourcesChanged();

        assertTrue(ModelStaleness.getInstance(getProject()).isStale());
    }

    public void testProvenanceRead_aReadThatLandedAfterTheEdit_clearsTheMark()
    {
        readAndSettle();

        ModelStaleness staleness = ModelStaleness.getInstance(getProject());

        staleness.pomSourcesChanged();
        staleness.provenanceRead();

        assertFalse(staleness.isStale());
    }

    public void testIsStale_theIdePromptArrivingWhileAReadIsAlreadyOwed_tellsTheViewsNothingNew()
    {
        readAndSettle();

        ModelStaleness.getInstance(getProject()).pomSourcesChanged();

        UIUtil.dispatchAllInvocationEvents();

        published.clear();

        IdeReloadPrompt.raise(getProject());

        try
        {
            UIUtil.dispatchAllInvocationEvents();

            assertEquals(
                "the views already say stale, so repainting them for the same answer is pure cost",
                List.of(),
                published);
        }
        finally
        {
            IdeReloadPrompt.clear(getProject());
        }
    }

    public void testPomSourcesChanged_theFirstEditAfterARead_tellsTheViewsOnce()
    {
        readAndSettle();

        ModelStaleness staleness = ModelStaleness.getInstance(getProject());

        staleness.pomSourcesChanged();
        staleness.pomSourcesChanged();

        UIUtil.dispatchAllInvocationEvents();

        assertEquals(
            "an import storm would otherwise repaint every view once per import",
            List.of("changed"),
            published);
    }

}
