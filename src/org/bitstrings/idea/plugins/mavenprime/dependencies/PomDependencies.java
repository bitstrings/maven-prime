package org.bitstrings.idea.plugins.mavenprime.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.jetbrains.idea.maven.dom.DependencyConflictId;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.MavenPropertyResolver;
import org.jetbrains.idea.maven.dom.model.MavenDomDependencies;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProfile;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.xml.DomElement;

public final class PomDependencies
{
    private PomDependencies()
    {
    }

    public static MavenDomProjectModel modelOf(Project project, MavenProject mavenProject)
    {
        return MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile());
    }

    public static MavenDomDependency declarationOf(
        Project project, MavenProject mavenProject, MavenArtifactNode node)
    {
        List<MavenArtifactNode> path = DependencyAnalysis.pathToRoot(node);

        if (path.isEmpty())
        {
            return null;
        }

        MavenDomProjectModel model = modelOf(project, mavenProject);

        if (model == null)
        {
            return null;
        }

        MavenArtifact direct = path.get(0).getArtifact();

        MavenDomDependency declaration = findDeclaration(model, direct);

        if (declaration == null)
        {
            MavenPrimeNotifications.warning(
                project,
                MavenPrimeBundle.message(
                    "mavenprime.dependencies.notDeclared", direct.getDisplayStringSimple()));
        }

        return declaration;
    }

    public static VirtualFile fileOf(MavenDomDependency declaration)
    {
        PsiElement element = declaration.getXmlElement();

        PsiFile file = (element == null) ? null : element.getContainingFile();

        return (file == null) ? null : file.getVirtualFile();
    }

    public static MavenDomDependency findDeclaration(MavenDomProjectModel model, MavenArtifact artifact)
    {
        DependencyConflictId wanted = DependencyConflictId.create(artifact);

        if (wanted == null)
        {
            return null;
        }

        MavenDomDependency literal =
            findFirst(model, dependency -> wanted.equals(DependencyConflictId.create(dependency)));

        return (literal != null)
            ? literal
            : findFirst(model, dependency -> wanted.equals(resolvedConflictId(model, dependency)));
    }

    public static MavenDomDependency findDeclaration(
        MavenDomProjectModel model, String groupId, String artifactId)
    {
        return findFirst(
            model,
            dependency ->
                groupId.equals(resolve(model, dependency.getGroupId().getStringValue()))
                    && artifactId.equals(resolve(model, dependency.getArtifactId().getStringValue())));
    }

    private static MavenDomDependency findFirst(
        MavenDomProjectModel model, Predicate<MavenDomDependency> matching)
    {
        MavenDomDependency declared = findIn(model, matching);

        if (declared != null)
        {
            return declared;
        }

        return ParentPomSearch.firstInAncestry(model, parent -> findIn(parent, matching));
    }

    private static MavenDomDependency findIn(
        MavenDomProjectModel model, Predicate<MavenDomDependency> matching)
    {
        for (MavenDomDependencies container : containersOf(model))
        {
            for (MavenDomDependency dependency : container.getDependencies())
            {
                if (matching.test(dependency))
                {
                    return dependency;
                }
            }
        }

        return null;
    }

    private static List<MavenDomDependencies> containersOf(MavenDomProjectModel model)
    {
        List<MavenDomDependencies> containers = declaredContainersOf(model);

        containers.add(model.getDependencyManagement().getDependencies());

        for (MavenDomProfile profile : model.getProfiles().getProfiles())
        {
            containers.add(profile.getDependencyManagement().getDependencies());
        }

        return containers;
    }

    private static List<MavenDomDependencies> declaredContainersOf(MavenDomProjectModel model)
    {
        List<MavenDomDependencies> containers = new ArrayList<>();

        containers.add(model.getDependencies());

        for (MavenDomProfile profile : model.getProfiles().getProfiles())
        {
            containers.add(profile.getDependencies());
        }

        return containers;
    }

    public static MavenDomDependency findManagedDeclaration(
        MavenDomProjectModel model, String groupId, String artifactId)
    {
        Ref<MavenDomDependency> found = Ref.create();

        found.set(matchIn(model.getDependencyManagement().getDependencies(), model, groupId, artifactId));

        if (!found.isNull())
        {
            return found.get();
        }

        return ParentPomSearch.firstInAncestry(
            model,
            parent -> matchIn(parent.getDependencyManagement().getDependencies(), parent, groupId, artifactId));
    }

    private static MavenDomDependency matchIn(
        MavenDomDependencies container, MavenDomProjectModel model, String groupId, String artifactId)
    {
        for (MavenDomDependency dependency : container.getDependencies())
        {
            if (
                groupId.equals(resolve(model, dependency.getGroupId().getStringValue()))
                    && artifactId.equals(resolve(model, dependency.getArtifactId().getStringValue()))
            )
            {
                return dependency;
            }
        }

        return null;
    }

    public static MavenDomDependency findDeclaredDependency(
        MavenDomProjectModel model, String groupId, String artifactId)
    {
        for (MavenDomDependencies container : declaredContainersOf(model))
        {
            for (MavenDomDependency dependency : container.getDependencies())
            {
                if (
                    groupId.equals(resolve(model, dependency.getGroupId().getStringValue()))
                        && artifactId.equals(resolve(model, dependency.getArtifactId().getStringValue()))
                )
                {
                    return dependency;
                }
            }
        }

        return null;
    }

    private static DependencyConflictId resolvedConflictId(
        MavenDomProjectModel model, MavenDomDependency dependency)
    {
        return DependencyConflictId.create(
            resolve(model, dependency.getGroupId().getStringValue()),
            resolve(model, dependency.getArtifactId().getStringValue()),
            resolve(model, dependency.getType().getStringValue()),
            resolve(model, dependency.getClassifier().getStringValue()));
    }

    static String resolve(MavenDomProjectModel model, String text)
    {
        return StringUtils.isBlank(text) ? text : MavenPropertyResolver.resolve(text, model);
    }

    public static boolean navigateTo(DomElement declaration)
    {
        if (declaration == null)
        {
            return false;
        }

        PsiElement element = declaration.getXmlElement();

        if (!(element instanceof Navigatable))
        {
            return false;
        }

        Navigatable navigatable = (Navigatable) element;

        if (!navigatable.canNavigate())
        {
            return false;
        }

        navigatable.navigate(true);

        return true;
    }
}
