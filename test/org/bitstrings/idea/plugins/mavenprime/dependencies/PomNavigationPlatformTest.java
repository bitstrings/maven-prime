package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;

public class PomNavigationPlatformTest
    extends BasePlatformTestCase
{
    public void testFindDeclaration_aDependencyDeclaredInThisPom_isFound()
    {
        MavenDomProjectModel model =
            model(
                "<dependencies><dependency><groupId>com.google.guava</groupId>"
                    + "<artifactId>guava</artifactId></dependency></dependencies>");

        assertNotNull(PomDependencies.findDeclaration(model, "com.google.guava", "guava"));
    }

    public void testNavigateTo_aDependencyDeclaredInThisPom_reachesIt()
    {
        MavenDomProjectModel model =
            model(
                "<dependencies><dependency><groupId>com.google.guava</groupId>"
                    + "<artifactId>guava</artifactId></dependency></dependencies>");

        assertTrue(
            PomDependencies.navigateTo(
                PomDependencies.findDeclaration(model, "com.google.guava", "guava")));
    }

    public void testNavigateTo_aPluginDeclaredInThisPom_reachesIt()
    {
        MavenDomProjectModel model =
            model(
                "<build><plugins><plugin><artifactId>maven-surefire-plugin</artifactId>"
                    + "</plugin></plugins></build>");

        assertTrue(
            PomDependencies.navigateTo(
                PomPlugins.findDeclaration(model, "org.apache.maven.plugins", "maven-surefire-plugin")));
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
