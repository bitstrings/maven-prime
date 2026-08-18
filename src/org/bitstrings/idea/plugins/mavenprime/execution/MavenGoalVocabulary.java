package org.bitstrings.idea.plugins.mavenprime.execution;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.MavenUtil;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;

public final class MavenGoalVocabulary
{
    public static final String CLEAN_PHASE = "clean";

    private MavenGoalVocabulary()
    {
    }

    public static List<String> phases()
    {
        return MavenConstants.PHASES;
    }

    public static boolean hasPhase(List<String> goals)
    {
        return goals.stream().anyMatch(MavenConstants.PHASES::contains);
    }

    public static List<String> allVariants(Project project)
    {
        Set<String> variants = new LinkedHashSet<>(MavenConstants.PHASES);

        for (LookupElement variant : MavenUtil.getPhaseVariants(MavenProjectsManager.getInstance(project)))
        {
            variants.add(variant.getLookupString());
        }

        return List.copyOf(variants);
    }
}
