package org.bitstrings.idea.plugins.mavenprime.run;

import org.apache.commons.lang3.StringUtils;

import com.intellij.codeInsight.TestFrameworks;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

public final class TestSelections
{
    private TestSelections()
    {
    }

    public static TestSelection of(PsiElement location)
    {
        if (location == null)
        {
            return null;
        }

        PsiMethod method = PsiTreeUtil.getParentOfType(location, PsiMethod.class, false);

        PsiClass owner =
            (method == null) ? PsiTreeUtil.getParentOfType(location, PsiClass.class, false)
                : method.getContainingClass();

        if ((owner == null) || StringUtils.isBlank(owner.getName())
            || !TestFrameworks.getInstance().isTestClass(owner))
        {
            return null;
        }

        if ((method != null) && TestFrameworks.getInstance().isTestMethod(method))
        {
            return new TestSelection(
                owner.getName() + '#' + method.getName(), owner.getName() + '.' + method.getName(), method);
        }

        return new TestSelection(owner.getName(), owner.getName(), owner);
    }
}
