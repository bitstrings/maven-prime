package org.bitstrings.idea.plugins.mavenprime.dependencies;

import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.vfs.VirtualFile;

public class DependencyAnalyzerEditorProviderImportingTest
    extends MavenImportingTestCase
{
    private static final String POM =
        "<groupId>org.bitstrings.test</groupId>"
            + "<artifactId>parent</artifactId>"
            + "<version>1.0</version>";

    private final DependencyAnalyzerEditorProvider provider = new DependencyAnalyzerEditorProvider();

    public void testAccept_theModulePom_isAccepted()
    {
        importProject(POM);

        assertTrue(
            "a provider that rejects the POM leaves the analyzer tab off the only file it can analyse",
            provider.accept(getProject(), getProjectPom()));
    }

    public void testAccept_anXmlFileInsideTheModule_isRejected()
        throws Exception
    {
        importProject(POM);

        VirtualFile beans = createProjectSubFile("src/main/resources/beans.xml", "<beans/>");

        assertFalse(
            "the analyzer reads one POM, so resolving the module a file merely sits in would put the tab "
                + "on files that have no dependency tree",
            provider.accept(getProject(), beans));
    }
}
