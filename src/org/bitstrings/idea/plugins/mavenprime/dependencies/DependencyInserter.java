package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public final class DependencyInserter
{
    private DependencyInserter()
    {
    }

    public static void insert(Project project, MavenProject mavenProject, ArtifactCoordinates coordinates)
    {
        MavenDomProjectModel model = PomDependencies.modelOf(project, mavenProject);

        PsiFile pomPsiFile = fileOf(model);

        if (pomPsiFile == null)
        {
            MavenPrimeNotifications.warning(
                project, MavenPrimeBundle.message("mavenprime.repository.add.unavailable"));

            return;
        }

        MavenDomDependency declared =
            PomDependencies.findDeclaredDependency(
                model, coordinates.groupId(), coordinates.artifactId());

        if ((declared != null) && mavenProject.getFile().equals(PomDependencies.fileOf(declared)))
        {
            MavenPrimeNotifications.info(
                project,
                MavenPrimeBundle.message("mavenprime.repository.add.already", coordinates.shortForm()));

            PomDependencies.navigateTo(declared);

            return;
        }

        Ref<MavenDomDependency> inserted = Ref.create();

        WriteCommandAction
            .writeCommandAction(project, pomPsiFile)
            .withName(MavenPrimeBundle.message("mavenprime.repository.add.command"))
            .run(() -> inserted.set(addDependency(model, coordinates)));

        MavenPrimeNotifications.info(
            project,
            MavenPrimeBundle.message(
                "mavenprime.repository.add.done", coordinates.shortForm(), mavenProject.getDisplayName()));

        PomDependencies.navigateTo(inserted.get());
    }

    private static PsiFile fileOf(MavenDomProjectModel model)
    {
        PsiElement element = (model == null) ? null : model.getXmlElement();

        return (element == null) ? null : element.getContainingFile();
    }

    static MavenDomDependency addDependency(
        MavenDomProjectModel model, ArtifactCoordinates coordinates)
    {
        MavenDomDependency dependency = model.getDependencies().addDependency();

        dependency.getGroupId().setStringValue(coordinates.groupId());
        dependency.getArtifactId().setStringValue(coordinates.artifactId());
        dependency.getVersion().setStringValue(coordinates.version());

        if (!MavenConstants.TYPE_JAR.equals(coordinates.extension()))
        {
            dependency.getType().setStringValue(coordinates.extension());
        }

        if (StringUtils.isNotBlank(coordinates.classifier()))
        {
            dependency.getClassifier().setStringValue(coordinates.classifier());
        }

        return dependency;
    }
}
