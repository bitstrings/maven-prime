package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.List;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class PomRunTargetsPlatformTest
    extends BasePlatformTestCase
{
    private static final String ANTRUN = "org.apache.maven.plugins:maven-antrun-plugin";

    public void testGoalsOf_anExecutionWithTwoGoals_buildsBothAgainstItsExecutionId()
    {
        XmlTag execution = firstTag(plugin("<id>early</id><goals><goal>run</goal><goal>help</goal></goals>"),
            "execution");

        assertEquals(
            List.of(ANTRUN + ":run@early", ANTRUN + ":help@early"), PomRunTargets.goalsOf(execution));
    }

    public void testGoalsOf_anExecutionWithoutAnId_usesMavenDefaultExecutionId()
    {
        XmlTag execution = firstTag(plugin("<goals><goal>run</goal></goals>"), "execution");

        assertEquals(List.of(ANTRUN + ":run@default"), PomRunTargets.goalsOf(execution));
    }

    public void testGoalsOf_aGoalElement_buildsOnlyThatGoal()
    {
        XmlTag goal = firstTag(plugin("<id>early</id><goals><goal>run</goal><goal>help</goal></goals>"), "goal");

        assertEquals(List.of(ANTRUN + ":run@early"), PomRunTargets.goalsOf(goal));
    }

    public void testGoalsOf_aPluginWithExecutions_buildsEveryExecutionGoal()
    {
        XmlTag plugin =
            firstTag(
                plugin("<id>one</id><goals><goal>run</goal></goals></execution><execution>"
                    + "<id>two</id><goals><goal>help</goal></goals>"),
                "plugin");

        assertEquals(List.of(ANTRUN + ":run@one", ANTRUN + ":help@two"), PomRunTargets.goalsOf(plugin));
    }

    public void testGoalsOf_aPluginWithAnExplicitGroupId_keepsThatGroupId()
    {
        XmlTag execution =
            firstTag(
                pom(
                    "<build><plugins><plugin><groupId>com.example</groupId>"
                        + "<artifactId>custom-plugin</artifactId><executions><execution>"
                        + "<goals><goal>run</goal></goals>"
                        + "</execution></executions></plugin></plugins></build>"),
                "execution");

        assertEquals(List.of("com.example:custom-plugin:run@default"), PomRunTargets.goalsOf(execution));
    }

    public void testGoalsOf_aPluginInsidePluginManagement_isNotRunnable()
    {
        XmlTag execution =
            firstTag(
                pom(
                    "<build><pluginManagement><plugins><plugin>"
                        + "<artifactId>maven-antrun-plugin</artifactId><executions><execution>"
                        + "<goals><goal>run</goal></goals>"
                        + "</execution></executions></plugin></plugins></pluginManagement></build>"),
                "execution");

        assertNull(PomRunTargets.goalsOf(execution));
    }

    public void testGoalsOf_aPluginInsideReporting_isNotRunnable()
    {
        XmlTag execution =
            firstTag(
                pom(
                    "<reporting><plugins><plugin>"
                        + "<artifactId>maven-antrun-plugin</artifactId><executions><execution>"
                        + "<goals><goal>run</goal></goals>"
                        + "</execution></executions></plugin></plugins></reporting>"),
                "execution");

        assertNull(PomRunTargets.goalsOf(execution));
    }

    public void testGoalsOf_anExecutionWithoutGoals_isNotRunnable()
    {
        XmlTag execution = firstTag(plugin("<id>early</id><phase>compile</phase>"), "execution");

        assertNull(PomRunTargets.goalsOf(execution));
    }

    public void testGoalsOf_theProjectRoot_asksForAGoalChoice()
    {
        XmlTag project = firstTag(pom("<modelVersion>4.0.0</modelVersion>"), "project");

        assertEquals(List.of(), PomRunTargets.goalsOf(project));
    }

    public void testGoalsOf_aDependencyElement_isNotRunnable()
    {
        XmlTag dependency =
            firstTag(
                pom("<dependencies><dependency><artifactId>guava</artifactId></dependency></dependencies>"),
                "dependency");

        assertNull(PomRunTargets.goalsOf(dependency));
    }

    public void testOpeningTagOf_theOpeningTagName_resolvesTheTag()
    {
        XmlTag execution = firstTag(plugin("<goals><goal>run</goal></goals>"), "execution");

        assertSame(execution, PomRunTargets.openingTagOf(nameToken(execution, true)));
    }

    public void testOpeningTagOf_theClosingTagName_resolvesNothing()
    {
        XmlTag execution = firstTag(plugin("<goals><goal>run</goal></goals>"), "execution");

        assertNull(PomRunTargets.openingTagOf(nameToken(execution, false)));
    }

    private static String plugin(String executionBody)
    {
        return pom(
            "<build><plugins><plugin><artifactId>maven-antrun-plugin</artifactId>"
                + "<executions><execution>" + executionBody
                + "</execution></executions></plugin></plugins></build>");
    }

    private static String pom(String body)
    {
        return "<project>" + body + "</project>";
    }

    private XmlTag firstTag(String pom, String name)
    {
        PsiFile file = myFixture.configureByText("pom.xml", pom);

        for (XmlTag tag : PsiTreeUtil.findChildrenOfType(file, XmlTag.class))
        {
            if (name.equals(tag.getName()))
            {
                return tag;
            }
        }

        throw new IllegalStateException("no <" + name + "> in the test pom");
    }

    private static PsiElement nameToken(XmlTag tag, boolean opening)
    {
        PsiElement found = null;

        for (PsiElement child = tag.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if (PsiUtilCore.getElementType(child) == XmlTokenType.XML_NAME)
            {
                found = child;

                if (opening)
                {
                    return found;
                }
            }
        }

        return found;
    }
}
