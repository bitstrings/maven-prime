package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.ide.plugins.UIComponentFileEditor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BuildProfileEditorPlatformTest
    extends BasePlatformTestCase
{
    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            for (VirtualFile file : editors().getOpenFiles())
            {
                editors().closeFile(file);
            }
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testOpen_aProfilerAskedForTheFullEditorWidth_putsItUpAsAnEditorTab()
    {
        BuildProfileEditor.getInstance(getProject()).open();

        assertEquals(
            List.of(MavenPrimeBundle.message("mavenprime.profile.editorTab")),
            Arrays.stream(editors().getOpenFiles()).map(VirtualFile::getName).toList());
    }

    public void testOpen_aProfilerAlreadyOpenInTheEditor_reusesTheTabThatIsUpRatherThanStackingAnother()
    {
        BuildProfileEditor.getInstance(getProject()).open();
        BuildProfileEditor.getInstance(getProject()).open();

        assertEquals(
            "the profiler shows whichever build is selected, so a second copy of it would only ever "
                + "duplicate the first",
            1,
            editors().getOpenFiles().length);
    }

    public void testGetFile_anEditorOverItThatCloses_disposesThePanelListeningForBuilds()
    {
        List<String> disposed = new ArrayList<>();

        UIComponentFileEditor editor =
            new UIComponentFileEditor(BuildProfileEditor.getInstance(getProject()).getFile());

        Disposer.register((Disposable) editor.getComponent(), () -> disposed.add("panel"));

        Disposer.dispose(editor);

        assertEquals(
            "the panel subscribes to the profile topic for the whole project, so a closed tab that "
                + "never disposes it keeps redrawing a view nobody can see",
            List.of("panel"),
            disposed);
    }

    private FileEditorManager editors()
    {
        return FileEditorManager.getInstance(getProject());
    }
}
