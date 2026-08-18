package org.bitstrings.idea.plugins.mavenprime.editor;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

abstract class PomDependencyIntention
    extends PsiElementBaseIntentionAction
{
    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement element)
    {
        PomDependency dependency = PomDependency.at(element);

        return (dependency != null) && isAvailableFor(project, dependency);
    }

    @Override
    public void invoke(Project project, Editor editor, PsiElement element)
    {
        PomDependency dependency = PomDependency.at(element);

        if (dependency != null)
        {
            invokeFor(project, editor, dependency);
        }
    }

    @Override
    public IntentionPreviewInfo generatePreview(Project project, Editor editor, PsiFile file)
    {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public boolean startInWriteAction()
    {
        return false;
    }

    @Override
    public String getFamilyName()
    {
        return MavenPrimeBundle.message("mavenprime.name");
    }

    protected boolean isAvailableFor(Project project, PomDependency dependency)
    {
        return true;
    }

    protected abstract void invokeFor(Project project, Editor editor, PomDependency dependency);
}
