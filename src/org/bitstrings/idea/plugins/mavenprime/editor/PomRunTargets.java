package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.repository.ArtifactCoordinates;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;

public final class PomRunTargets
{
    private static final String PROJECT_TAG = "project";

    private static final String PLUGIN_TAG = "plugin";

    private static final String EXECUTIONS_TAG = "executions";

    private static final String EXECUTION_TAG = "execution";

    private static final String GOALS_TAG = "goals";

    private static final String GOAL_TAG = "goal";

    private static final String GROUP_ID_TAG = "groupId";

    private static final String ARTIFACT_ID_TAG = "artifactId";

    private static final String ID_TAG = "id";

    private static final String PLUGIN_MANAGEMENT_TAG = "pluginManagement";

    private static final String REPORTING_TAG = "reporting";

    private static final String DEFAULT_EXECUTION_ID = "default";

    private PomRunTargets()
    {
    }

    public static PomRunTarget of(PsiElement element)
    {
        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);

        while (tag != null)
        {
            PomRunTarget target = targetFor(tag);

            if (target != null)
            {
                return target;
            }

            tag = tag.getParentTag();
        }

        return null;
    }

    public static PomRunTarget at(PsiElement leaf)
    {
        XmlTag tag = openingTagOf(leaf);

        return (tag == null) ? null : targetFor(tag);
    }

    static XmlTag openingTagOf(PsiElement leaf)
    {
        if (!(leaf instanceof XmlToken)
            || (((XmlToken) leaf).getTokenType() != XmlTokenType.XML_NAME)
            || (PsiUtilCore.getElementType(leaf.getPrevSibling()) != XmlTokenType.XML_START_TAG_START))
        {
            return null;
        }

        PsiElement parent = leaf.getParent();

        return (parent instanceof XmlTag) ? (XmlTag) parent : null;
    }

    private static PomRunTarget targetFor(XmlTag tag)
    {
        List<String> goals = goalsOf(tag);

        if (goals == null)
        {
            return null;
        }

        MavenProject module = PomModules.of(tag);

        return (module == null) ? null : new PomRunTarget(module, goals, labelOf(module, goals));
    }

    static List<String> goalsOf(XmlTag tag)
    {
        switch (tag.getName())
        {
            case PROJECT_TAG:
                return (tag.getParentTag() == null) ? List.of() : null;

            case GOAL_TAG:
                return singleGoal(tag);

            case EXECUTION_TAG:
                return executionGoals(tag);

            case PLUGIN_TAG:
                return pluginGoals(tag);

            default:
                return null;
        }
    }

    private static List<String> singleGoal(XmlTag goal)
    {
        XmlTag goals = parentNamed(goal, GOALS_TAG);
        XmlTag execution = parentNamed(goals, EXECUTION_TAG);
        String coordinates = pluginCoordinatesOf(execution);
        String name = goal.getValue().getTrimmedText();

        return ((coordinates == null) || StringUtils.isBlank(name))
            ? null
            : List.of(goalLine(coordinates, name, executionIdOf(execution)));
    }

    private static List<String> executionGoals(XmlTag execution)
    {
        String coordinates = pluginCoordinatesOf(execution);
        XmlTag goals = execution.findFirstSubTag(GOALS_TAG);

        if ((coordinates == null) || (goals == null))
        {
            return null;
        }

        String executionId = executionIdOf(execution);

        List<String> goalLines = new ArrayList<>();

        for (XmlTag goal : goals.findSubTags(GOAL_TAG))
        {
            String name = goal.getValue().getTrimmedText();

            if (StringUtils.isNotBlank(name))
            {
                goalLines.add(goalLine(coordinates, name, executionId));
            }
        }

        return goalLines.isEmpty() ? null : goalLines;
    }

    private static List<String> pluginGoals(XmlTag plugin)
    {
        XmlTag executions = plugin.findFirstSubTag(EXECUTIONS_TAG);

        if (executions == null)
        {
            return null;
        }

        List<String> goalLines = new ArrayList<>();

        for (XmlTag execution : executions.findSubTags(EXECUTION_TAG))
        {
            List<String> goals = executionGoals(execution);

            if (goals != null)
            {
                goalLines.addAll(goals);
            }
        }

        return goalLines.isEmpty() ? null : goalLines;
    }

    private static String pluginCoordinatesOf(XmlTag execution)
    {
        XmlTag plugin = parentNamed(parentNamed(execution, EXECUTIONS_TAG), PLUGIN_TAG);

        if ((plugin == null) || isDeclarationOnly(plugin))
        {
            return null;
        }

        String artifactId = childText(plugin, ARTIFACT_ID_TAG);

        return StringUtils.isBlank(artifactId)
            ? null
            : StringUtils.defaultIfBlank(
                childText(plugin, GROUP_ID_TAG), ArtifactCoordinates.DEFAULT_PLUGIN_GROUP_ID)
                + ':' + artifactId;
    }

    private static boolean isDeclarationOnly(XmlTag plugin)
    {
        for (XmlTag ancestor = plugin.getParentTag(); ancestor != null; ancestor = ancestor.getParentTag())
        {
            if (PLUGIN_MANAGEMENT_TAG.equals(ancestor.getName()) || REPORTING_TAG.equals(ancestor.getName()))
            {
                return true;
            }
        }

        return false;
    }

    private static String goalLine(String pluginCoordinates, String goal, String executionId)
    {
        return pluginCoordinates + ':' + goal + '@' + executionId;
    }

    private static String executionIdOf(XmlTag execution)
    {
        return StringUtils.defaultIfBlank(childText(execution, ID_TAG), DEFAULT_EXECUTION_ID);
    }

    private static String childText(XmlTag tag, String name)
    {
        XmlTag child = tag.findFirstSubTag(name);

        return (child == null) ? null : child.getValue().getTrimmedText();
    }

    private static XmlTag parentNamed(XmlTag tag, String name)
    {
        if (tag == null)
        {
            return null;
        }

        XmlTag parent = tag.getParentTag();

        return ((parent != null) && name.equals(parent.getName())) ? parent : null;
    }

    private static String labelOf(MavenProject module, List<String> goals)
    {
        return goals.isEmpty() ? module.getDisplayName() : String.join(StringUtils.SPACE, goals);
    }
}
