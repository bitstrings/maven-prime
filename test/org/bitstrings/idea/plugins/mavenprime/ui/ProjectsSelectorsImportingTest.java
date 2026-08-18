package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.List;

import com.intellij.maven.testFramework.MavenImportingTestCase;

public class ProjectsSelectorsImportingTest
    extends MavenImportingTestCase
{
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

    public void testSelectorsIn_animportedReactor_offersTheColonFormForEveryModule()
        throws Exception
    {
        createModulePom("core", MODULE_POM);
        importProject(PARENT_POM);

        List<String> selectors = ProjectsTextField.selectorsIn(getProject());

        assertTrue(
            "-pl accepts :artifactId, so completion has to offer it: " + selectors,
            selectors.containsAll(List.of(":parent", ":core")));
    }

    public void testSelectorsIn_aModuleBelowTheRoot_offersItsRelativePath()
        throws Exception
    {
        createModulePom("core", MODULE_POM);
        importProject(PARENT_POM);

        assertTrue(
            "-pl also accepts a path relative to the reactor root",
            ProjectsTextField.selectorsIn(getProject()).contains("core"));
    }

    public void testSelectorsIn_theRootItself_isNotOfferedAsAnEmptyPath()
        throws Exception
    {
        createModulePom("core", MODULE_POM);
        importProject(PARENT_POM);

        assertFalse(ProjectsTextField.selectorsIn(getProject()).contains(""));
    }
}
