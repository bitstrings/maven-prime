package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.function.Function;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;

public final class PomRunLineMarkerContributor
    extends RunLineMarkerContributor
{
    private static final Function<PsiElement, String> TOOLTIP = PomRunLineMarkerContributor::tooltipOf;

    @Override
    public Info getInfo(PsiElement element)
    {
        return (PomRunTargets.at(element) == null) ? null : runInfo();
    }

    static Info runInfo()
    {
        return new Info(AllIcons.RunConfigurations.TestState.Run, ExecutorAction.getActions(), TOOLTIP);
    }

    private static String tooltipOf(PsiElement element)
    {
        PomRunTarget target = PomRunTargets.at(element);

        return (target == null)
            ? null
            : MavenPrimeBundle.message("mavenprime.editor.runTooltip", target.label());
    }
}
