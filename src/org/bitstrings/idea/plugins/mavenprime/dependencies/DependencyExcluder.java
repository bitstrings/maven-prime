package org.bitstrings.idea.plugins.mavenprime.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomExclusion;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

public final class DependencyExcluder
{
    private DependencyExcluder()
    {
    }

    public static boolean isExcludable(MavenArtifactNode node)
    {
        return DependencyAnalysis.pathToRoot(node).size() > 1;
    }

    public static void exclude(Project project, MavenProject mavenProject, MavenArtifactNode node)
    {
        if (!isExcludable(node))
        {
            MavenPrimeNotifications.warning(
                project, MavenPrimeBundle.message("mavenprime.dependencies.exclude.direct"));

            return;
        }

        MavenDomDependency declaration = PomDependencies.declarationOf(project, mavenProject, node);

        if (declaration == null)
        {
            return;
        }

        VirtualFile declaringFile = PomDependencies.fileOf(declaration);

        if (!mavenProject.getFile().equals(declaringFile))
        {
            MavenPrimeNotifications.warning(
                project,
                MavenPrimeBundle.message(
                    "mavenprime.dependencies.exclude.inherited",
                    (declaringFile == null) ? StringUtils.EMPTY : declaringFile.getPresentableUrl()));

            return;
        }

        MavenArtifact excluded = node.getArtifact();

        PsiFile pomPsiFile = declaration.getXmlElement().getContainingFile();

        WriteCommandAction
            .writeCommandAction(project, pomPsiFile)
            .withName(MavenPrimeBundle.message("mavenprime.dependencies.exclude.command"))
            .run(() -> addExclusion(declaration, excluded));
    }

    static void addExclusion(MavenDomDependency declaration, MavenArtifact excluded)
    {
        MavenId excludedId = excluded.getMavenId();

        for (MavenDomExclusion existing : declaration.getExclusions().getExclusions())
        {
            if (excludedId.equals(
                existing.getGroupId().getStringValue(), existing.getArtifactId().getStringValue()))
            {
                return;
            }
        }

        MavenDomExclusion exclusion = declaration.getExclusions().addExclusion();

        exclusion.getGroupId().setStringValue(excluded.getGroupId());
        exclusion.getArtifactId().setStringValue(excluded.getArtifactId());
    }
}
