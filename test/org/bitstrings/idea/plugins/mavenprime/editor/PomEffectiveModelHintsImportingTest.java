package org.bitstrings.idea.plugins.mavenprime.editor;

import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;

public class PomEffectiveModelHintsImportingTest
    extends MavenImportingTestCase
{
    private static final String POM =
        "<groupId>org.bitstrings.test</groupId>"
            + "<artifactId>parent</artifactId>"
            + "<version>1.0</version>";

    private final PomEffectiveModelHints provider = new PomEffectiveModelHints();

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

    public void testCreateCollector_theModulePom_yieldsACollector()
    {
        importProject(POM);

        assertNotNull(
            "no collector means no effective-model hint is ever drawn in a POM",
            collectorFor(getProjectPom()));
    }

    public void testCreateCollector_anXmlFileInsideTheModule_yieldsNoCollector()
        throws Exception
    {
        importProject(POM);

        VirtualFile beans = createProjectSubFile("src/main/resources/beans.xml", "<beans/>");

        assertNull(
            "the provider is registered for the whole XML language, so resolving the module a file merely "
                + "sits in decorates unrelated XML with that module's resolved values",
            collectorFor(beans));
    }

    private InlayHintsCollector collectorFor(VirtualFile file)
    {
        Editor editor =
            FileEditorManager
                .getInstance(getProject())
                .openTextEditor(new OpenFileDescriptor(getProject(), file), true);

        return provider.createCollector(PsiManager.getInstance(getProject()).findFile(file), editor);
    }
}
