package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;

import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.PsiModificationTracker;

public class EffectiveModelHintRefresherImportingTest
    extends MavenImportingTestCase
{
    private static final String POM =
        "<groupId>org.bitstrings.test</groupId>"
            + "<artifactId>parent</artifactId>"
            + "<version>1.0</version>"
            + "<properties><version.groovy>4.0.32</version.groovy></properties>";

    // Importing with auto-refresh on starts a real Maven read, whose own signals would drive the reparse.
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        MavenPrimeSettings.getInstance(getProject()).autoRefresh = false;
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            FileEditorManager editors = FileEditorManager.getInstance(getProject());

            for (VirtualFile file : editors.getOpenFiles())
            {
                editors.closeFile(file);
            }
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testProvenanceRead_aPomOpenWhenTheModelChanged_movesTheStampInlayHintsAreCachedAgainst()
    {
        importProject(POM);

        FileEditorManager.getInstance(getProject()).openFile(getProjectPom(), true);

        long before = PsiModificationTracker.getInstance(getProject()).getModificationCount();

        EffectiveModelHintRefresher.getInstance(getProject()).provenanceRead();

        assertTrue(
            "declarative inlays are recomputed only when the project's PSI modification count moves, "
                + "so a resolved value that changes with no edit to this file never reaches the editor",
            PsiModificationTracker.getInstance(getProject()).getModificationCount() > before);
    }

    public void testOpenPoms_anOpenFileThatIsNotAModulePom_isLeftOutOfTheReparse()
        throws Exception
    {
        importProject(POM);

        VirtualFile source = createProjectSubFile("src/main/java/Sample.java", "class Sample {}");

        FileEditorManager.getInstance(getProject()).openFile(getProjectPom(), true);
        FileEditorManager.getInstance(getProject()).openFile(source, true);

        assertEquals(
            "reparsing a file the effective model cannot appear in throws away its highlighting for "
                + "nothing",
            List.of(getProjectPom()),
            EffectiveModelHintRefresher.getInstance(getProject()).openPoms());
    }
}
