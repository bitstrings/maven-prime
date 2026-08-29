package org.bitstrings.idea.plugins.mavenprime.util;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;

public class MavenProjectsImportingTest
    extends MavenImportingTestCase
{
    private static final int TIMEOUT_SECONDS = 60;

    private static final String PARENT_POM =
        "<groupId>org.bitstrings.test</groupId>"
            + "<artifactId>parent</artifactId>"
            + "<version>1.0</version>"
            + "<packaging>pom</packaging>"
            + "<modules><module>core</module></modules>";

    private static final String MODULE_POM =
        "<groupId>org.bitstrings.test</groupId>"
            + "<artifactId>core</artifactId>"
            + "<version>1.0</version>";

    public void testForFile_pomOfAnImportedModule_resolvesThatModule()
    {
        importProject(PARENT_POM);

        MavenProject resolved = MavenProjects.forFile(getProject(), getProjectPom());

        assertNotNull(resolved);
        assertEquals("parent", resolved.getMavenId().getArtifactId());
    }

    public void testForFile_sourceFileInsideAModule_resolvesTheContainingModule()
        throws Exception
    {
        createModulePom("core", MODULE_POM);
        importProject(PARENT_POM);

        VirtualFile source = createProjectSubFile("core/src/main/java/Sample.java", "class Sample {}");

        MavenProject resolved = MavenProjects.forFile(getProject(), source);

        assertNotNull(resolved);
        assertEquals("core", resolved.getMavenId().getArtifactId());
    }

    public void testForFile_calledFromABackgroundThread_holdsItsOwnReadAccess()
        throws Exception
    {
        createModulePom("core", MODULE_POM);
        importProject(PARENT_POM);

        VirtualFile source = createProjectSubFile("core/src/main/java/Sample.java", "class Sample {}");

        Future<MavenProject> resolved =
            ApplicationManager
                .getApplication()
                .executeOnPooledThread(() -> MavenProjects.forFile(getProject(), source));

        assertEquals("core", resolved.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).getMavenId().getArtifactId());
    }

    public void testByPomFile_pomOfAnImportedModule_resolvesThatModule()
    {
        importProject(PARENT_POM);

        MavenProject resolved = MavenProjects.byPomFile(getProject(), getProjectPom());

        assertNotNull(resolved);
        assertEquals("parent", resolved.getMavenId().getArtifactId());
    }

    public void testByPomFile_sourceFileInsideAModule_resolvesNothing()
        throws Exception
    {
        createModulePom("core", MODULE_POM);
        importProject(PARENT_POM);

        VirtualFile source = createProjectSubFile("core/src/main/java/Sample.java", "class Sample {}");

        assertNull(MavenProjects.byPomFile(getProject(), source));
    }

    public void testByPomFile_nullFile_resolvesNothing()
    {
        importProject(PARENT_POM);

        assertNull(MavenProjects.byPomFile(getProject(), null));
    }

    public void testAllProfiles_pomDeclaringProfiles_listsThem()
    {
        importProject(
            PARENT_POM
                + "<profiles>"
                + "  <profile><id>release</id></profile>"
                + "  <profile><id>dev</id></profile>"
                + "</profiles>");

        assertTrue(MavenProjects.allProfiles(getProject()).containsAll(List.of("release", "dev")));
    }

    public void testRoots_singleModuleReactor_hasOneRoot()
    {
        importProject(PARENT_POM);

        assertEquals(1, MavenProjects.roots(getProject()).size());
    }
}
