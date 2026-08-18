package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.artifact;

import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;

public class PomDependenciesPlatformTest
    extends BasePlatformTestCase
{
    private static final String GUAVA_GROUP = "com.google.guava";

    private static final String GUAVA = "guava";

    public void testFindDeclaration_aDependencyDeclaredInThisPom_isFound()
    {
        assertNotNull(PomDependencies.findDeclaration(model(dependencies()), GUAVA_GROUP, GUAVA));
    }

    public void testFindDeclaration_aDependencyOnlyInDependencyManagement_isStillFound()
    {
        MavenDomProjectModel model =
            model("<dependencyManagement>" + dependencies() + "</dependencyManagement>");

        assertNotNull(PomDependencies.findDeclaration(model, GUAVA_GROUP, GUAVA));
    }

    public void testFindDeclaration_aDependencyDeclaredInsideAProfile_isStillFound()
    {
        MavenDomProjectModel model =
            model("<profiles><profile><id>extra</id>" + dependencies() + "</profile></profiles>");

        assertNotNull(PomDependencies.findDeclaration(model, GUAVA_GROUP, GUAVA));
    }

    public void testFindDeclaration_anArtifactThisPomNeverMentions_isNotFound()
    {
        assertNull(PomDependencies.findDeclaration(model(dependencies()), GUAVA_GROUP, "guava-testlib"));
    }

    public void testFindDeclaration_byResolvedArtifact_reachesTheSameDeclaration()
    {
        MavenDomProjectModel model = model(dependencies());

        assertNotNull(PomDependencies.findDeclaration(model, artifact(GUAVA_GROUP, GUAVA, "33.0.0-jre")));
    }

    private static String dependencies()
    {
        return "<dependencies><dependency><groupId>" + GUAVA_GROUP + "</groupId>"
            + "<artifactId>" + GUAVA + "</artifactId></dependency></dependencies>";
    }

    private MavenDomProjectModel model(String body)
    {
        PsiFile file = myFixture.configureByText("pom.xml", "<project>" + body + "</project>");

        DomFileElement<MavenDomProjectModel> element =
            DomManager.getDomManager(getProject()).getFileElement((XmlFile) file, MavenDomProjectModel.class);

        assertNotNull("the fixture pom was not recognized as a Maven model", element);

        return element.getRootElement();
    }
}
