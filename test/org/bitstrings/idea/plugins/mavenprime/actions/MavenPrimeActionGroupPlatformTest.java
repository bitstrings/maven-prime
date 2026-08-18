package org.bitstrings.idea.plugins.mavenprime.actions;

import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenPrimeActionGroupPlatformTest
    extends BasePlatformTestCase
{
    public void testUpdate_aProjectMavenHasRuledOut_hidesTheWholeMenu()
    {
        assertNotNull(
            "the gate needs a decided Maven state, or this exercises the still-loading branch instead",
            MavenProjectsManager.getInstance(getProject()));

        AnActionEvent event = eventWith(SimpleDataContext.getProjectContext(getProject()));

        new MavenPrimeActionGroup().update(event);

        assertFalse(
            "every action under this menu needs a Maven model, so a non-Maven project must not see it",
            event.getPresentation().isVisible());
    }

    public void testUpdate_noProjectToReadAMavenModelFrom_hidesTheWholeMenu()
    {
        AnActionEvent event = eventWith(DataContext.EMPTY_CONTEXT);

        new MavenPrimeActionGroup().update(event);

        assertFalse(event.getPresentation().isVisible());
    }

    private static AnActionEvent eventWith(DataContext context)
    {
        return AnActionEvent.createEvent(
            context, new Presentation(), "MavenPrimeTest", ActionUiKind.NONE, null);
    }
}
