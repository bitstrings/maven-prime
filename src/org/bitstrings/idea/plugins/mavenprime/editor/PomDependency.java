package org.bitstrings.idea.plugins.mavenprime.editor;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;

public record PomDependency(MavenProject module, String groupId, String artifactId)
{
    private static final String DEPENDENCY_TAG = "dependency";

    private static final String GROUP_ID_TAG = "groupId";

    private static final String ARTIFACT_ID_TAG = "artifactId";

    public static PomDependency at(PsiElement element)
    {
        XmlTag tag = tagOf(element);

        if (tag == null)
        {
            return null;
        }

        String groupId = childText(tag, GROUP_ID_TAG);
        String artifactId = childText(tag, ARTIFACT_ID_TAG);

        if (StringUtils.isBlank(groupId) || StringUtils.isBlank(artifactId))
        {
            return null;
        }

        MavenProject module = PomModules.of(tag);

        return (module == null) ? null : new PomDependency(module, groupId, artifactId);
    }

    public String key()
    {
        return groupId + ':' + artifactId;
    }

    public String moduleKey()
    {
        MavenId id = module.getMavenId();

        return id.getGroupId() + ':' + id.getArtifactId();
    }

    public boolean matches(String managementKey)
    {
        return (managementKey != null)
            && (managementKey.equals(key()) || managementKey.startsWith(key() + ':'));
    }

    private static XmlTag tagOf(PsiElement element)
    {
        for (XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
            tag != null;
            tag = tag.getParentTag())
        {
            if (DEPENDENCY_TAG.equals(tag.getName()))
            {
                return tag;
            }
        }

        return null;
    }

    private static String childText(XmlTag tag, String name)
    {
        XmlTag child = tag.findFirstSubTag(name);

        return (child == null) ? null : child.getValue().getTrimmedText();
    }
}
