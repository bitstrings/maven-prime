package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.artifact;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class PomMutationPlatformTest
    extends BasePlatformTestCase
{
    private static final String POM =
        "<project>\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>org.example</groupId>\n"
            + "  <artifactId>app</artifactId>\n"
            + "  <version>1.0</version>\n"
            + "  <dependencies>\n"
            + "    <dependency>\n"
            + "      <groupId>com.mid</groupId>\n"
            + "      <artifactId>mid</artifactId>\n"
            + "      <version>1.0</version>\n"
            + "    </dependency>\n"
            + "  </dependencies>\n"
            + "</project>\n";

    public void testAddExclusion_aTransitiveArtifact_writesItUnderTheDeclarationThatBringsItIn()
    {
        MavenDomDependency declaration = onlyDeclaration();

        write(() -> DependencyExcluder.addExclusion(declaration, artifact("com.lib", "shared", "2.0")));

        assertEquals("com.lib", exclusionCoordinates(declaration)[0]);
        assertEquals("shared", exclusionCoordinates(declaration)[1]);
    }

    public void testAddExclusion_theSameArtifactTwice_leavesOneExclusionRatherThanTwo()
    {
        MavenDomDependency declaration = onlyDeclaration();

        write(() -> DependencyExcluder.addExclusion(declaration, artifact("com.lib", "shared", "2.0")));
        write(() -> DependencyExcluder.addExclusion(declaration, artifact("com.lib", "shared", "2.0")));

        assertEquals(1, declaration.getExclusions().getExclusions().size());
    }

    public void testAddExclusion_aSecondDistinctArtifact_keepsTheFirstExclusion()
    {
        MavenDomDependency declaration = onlyDeclaration();

        write(() -> DependencyExcluder.addExclusion(declaration, artifact("com.lib", "shared", "2.0")));
        write(() -> DependencyExcluder.addExclusion(declaration, artifact("com.other", "other", "1.0")));

        assertEquals(2, declaration.getExclusions().getExclusions().size());
    }

    public void testIsExcludable_aDirectDependency_isFalseBecauseThePomDeclaresItAlready()
    {
        assertFalse(DependencyExcluder.isExcludable(declared()));
    }

    public void testIsExcludable_aTransitiveDependency_isTrueBecauseOnlyAnExclusionCanDropIt()
    {
        MavenArtifactNode direct = declared();

        MavenArtifactNode transitive =
            new MavenArtifactNode(
                direct,
                artifact("com.lib", "shared", "2.0"),
                MavenArtifactState.ADDED,
                null,
                "compile",
                null,
                null);

        direct.setDependencies(List.of(transitive));

        assertTrue(DependencyExcluder.isExcludable(transitive));
    }

    private static MavenArtifactNode declared()
    {
        return new MavenArtifactNode(
            null, artifact("com.mid", "mid", "1.0"), MavenArtifactState.ADDED, null, "compile", null, null);
    }

    public void testAddDependency_newCoordinates_writesTheFullGav()
    {
        MavenDomProjectModel model = model();

        write(() -> DependencyInserter.addDependency(model, coordinates("jar", "")));

        MavenDomDependency added = lastDependency(model);

        assertEquals("commons-io", added.getGroupId().getStringValue());
        assertEquals("commons-io", added.getArtifactId().getStringValue());
        assertEquals("2.16.1", added.getVersion().getStringValue());
    }

    public void testAddDependency_aJarArtifact_leavesTheTypeOutBecauseJarIsMavensDefault()
    {
        MavenDomProjectModel model = model();

        write(() -> DependencyInserter.addDependency(model, coordinates("jar", "")));

        assertNull(lastDependency(model).getType().getStringValue());
    }

    public void testAddDependency_aNonJarArtifact_writesTheTypeSoMavenResolvesTheRightFile()
    {
        MavenDomProjectModel model = model();

        write(() -> DependencyInserter.addDependency(model, coordinates("zip", "")));

        assertEquals("zip", lastDependency(model).getType().getStringValue());
    }

    public void testAddDependency_aClassifiedArtifact_writesTheClassifier()
    {
        MavenDomProjectModel model = model();

        write(() -> DependencyInserter.addDependency(model, coordinates("jar", "linux-x86_64")));

        assertEquals("linux-x86_64", lastDependency(model).getClassifier().getStringValue());
    }

    public void testAddDependency_anUnclassifiedArtifact_leavesTheClassifierOut()
    {
        MavenDomProjectModel model = model();

        write(() -> DependencyInserter.addDependency(model, coordinates("jar", "")));

        assertNull(lastDependency(model).getClassifier().getStringValue());
    }

    private static ArtifactCoordinates coordinates(String extension, String classifier)
    {
        return new ArtifactCoordinates("commons-io", "commons-io", extension, classifier, "2.16.1");
    }

    private static String[] exclusionCoordinates(MavenDomDependency declaration)
    {
        return new String[]
        {
            declaration.getExclusions().getExclusions().get(0).getGroupId().getStringValue(),
            declaration.getExclusions().getExclusions().get(0).getArtifactId().getStringValue()
        };
    }

    private static MavenDomDependency lastDependency(MavenDomProjectModel model)
    {
        List<MavenDomDependency> dependencies = model.getDependencies().getDependencies();

        return dependencies.get(dependencies.size() - 1);
    }

    private MavenDomDependency onlyDeclaration()
    {
        return model().getDependencies().getDependencies().get(0);
    }

    private MavenDomProjectModel model()
    {
        PsiFile pom = myFixture.configureByText("pom.xml", POM);

        return MavenDomUtil.getMavenDomProjectModel(getProject(), pom.getVirtualFile());
    }

    private void write(Runnable mutation)
    {
        WriteCommandAction.runWriteCommandAction(getProject(), mutation);
    }
}
