package org.bitstrings.idea.plugins.mavenprime.editor;

import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;

public class PomModulesImportingTest
    extends MavenImportingTestCase
{
    private static final String POM =
        "<groupId>org.bitstrings.test</groupId>"
            + "<artifactId>parent</artifactId>"
            + "<version>1.0</version>";

    public void testOf_theRootTagOfTheModulePom_resolvesThatModule()
    {
        importProject(POM);

        MavenProject module = moduleOfRootTag(getProjectPom());

        assertNotNull("every POM marker and reference hangs off this lookup", module);
        assertEquals("parent", module.getMavenId().getArtifactId());
    }

    public void testOf_theRootTagOfAnXmlFileThatIsNotAPom_resolvesNothing()
        throws Exception
    {
        importProject(POM);

        VirtualFile beans = createProjectSubFile("src/main/resources/beans.xml", "<beans/>");

        assertNull(
            "POM markers on a file that is not a POM navigate to coordinates the file never declared",
            moduleOfRootTag(beans));
    }

    private MavenProject moduleOfRootTag(VirtualFile file)
    {
        return ApplicationManager
            .getApplication()
            .runReadAction(
                (Computable<MavenProject>) () ->
                    PomModules.of(
                        ((XmlFile) PsiManager.getInstance(getProject()).findFile(file)).getRootTag()));
    }
}
