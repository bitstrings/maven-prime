package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;

public class PomPluginsPlatformTest
    extends BasePlatformTestCase
{
    private static final String PLUGIN_GROUP = "org.apache.maven.plugins";

    private static final String SUREFIRE = "maven-surefire-plugin";

    public void testFindDeclaration_aPluginWithoutAGroupId_matchesMavensDefaultPluginGroup()
    {
        MavenDomProjectModel model =
            model("<build><plugins><plugin><artifactId>" + SUREFIRE + "</artifactId></plugin></plugins></build>");

        assertNotNull(PomPlugins.findDeclaration(model, PLUGIN_GROUP, SUREFIRE));
    }

    public void testFindDeclaration_aPluginWithAnExplicitGroupId_matchesThatGroup()
    {
        MavenDomProjectModel model =
            model(
                "<build><plugins><plugin><groupId>com.example</groupId>"
                    + "<artifactId>custom-plugin</artifactId></plugin></plugins></build>");

        assertNotNull(PomPlugins.findDeclaration(model, "com.example", "custom-plugin"));
    }

    public void testFindDeclaration_aPluginOnlyInPluginManagement_isStillFound()
    {
        MavenDomProjectModel model =
            model(
                "<build><pluginManagement><plugins><plugin><artifactId>" + SUREFIRE
                    + "</artifactId></plugin></plugins></pluginManagement></build>");

        assertNotNull(PomPlugins.findDeclaration(model, PLUGIN_GROUP, SUREFIRE));
    }

    public void testFindDeclaration_aPluginDeclaredInsideAProfile_isStillFound()
    {
        MavenDomProjectModel model =
            model(
                "<profiles><profile><id>release</id><build><plugins><plugin><artifactId>" + SUREFIRE
                    + "</artifactId></plugin></plugins></build></profile></profiles>");

        assertNotNull(PomPlugins.findDeclaration(model, PLUGIN_GROUP, SUREFIRE));
    }

    public void testFindDeclaration_anArtifactDeclaredOnlyAsADependency_isNotAPluginDeclaration()
    {
        MavenDomProjectModel model =
            model(
                "<dependencies><dependency><groupId>" + PLUGIN_GROUP + "</groupId>"
                    + "<artifactId>" + SUREFIRE + "</artifactId></dependency></dependencies>");

        assertNull(PomPlugins.findDeclaration(model, PLUGIN_GROUP, SUREFIRE));
    }

    public void testFindDeclaration_aPluginThisPomNeverMentions_isNotFound()
    {
        MavenDomProjectModel model =
            model("<build><plugins><plugin><artifactId>maven-jar-plugin</artifactId></plugin></plugins></build>");

        assertNull(PomPlugins.findDeclaration(model, PLUGIN_GROUP, SUREFIRE));
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
