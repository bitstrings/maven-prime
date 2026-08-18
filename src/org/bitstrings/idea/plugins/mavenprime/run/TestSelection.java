package org.bitstrings.idea.plugins.mavenprime.run;

import com.intellij.psi.PsiElement;

public record TestSelection(String filter, String label, PsiElement element)
{
}
