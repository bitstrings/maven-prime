package org.bitstrings.idea.plugins.mavenprime.goals;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@Service(Service.Level.APP)
@State(name = "MavenPrimeGlobalGoals", storages = @Storage("mavenPrimeGoals.xml"))
public final class ApplicationGoalStore
    extends GoalStore
{
    public static ApplicationGoalStore getInstance()
    {
        return ApplicationManager.getApplication().getService(ApplicationGoalStore.class);
    }

    @Override
    public void noStateLoaded()
    {
        seedDefaults(defaults());
    }

    @Override
    public void loadState(GoalList loaded)
    {
        super.loadState(loaded);

        mergeDefaults(defaults());
    }

    public static List<GoalDefinition> defaults()
    {
        return List.of(
            GoalDefinition.of("install"),
            GoalDefinition.of("package"),
            GoalDefinition.of("clean install"),
            cleanInstallSkippingTests(),
            GoalDefinition.of("clean package"),
            GoalDefinition.of("verify"),
            GoalDefinition.of("test"),
            GoalDefinition.of("test-compile"),
            GoalDefinition.of("clean"),
            GoalDefinition.of("dependency:tree"),
            GoalDefinition.of("dependency:analyze"),
            dependencyResolveUpdatingSnapshots(),
            GoalDefinition.of("help:effective-pom"));
    }

    private static GoalDefinition cleanInstallSkippingTests()
    {
        return carrying("clean-install-skiptests", "clean install (skip tests)", "clean install",
            MavenFlag.SKIP_TESTS);
    }

    private static GoalDefinition dependencyResolveUpdatingSnapshots()
    {
        return carrying("dependency-resolve-updatesnapshots", "dependency:resolve (update)", "dependency:resolve",
            MavenFlag.UPDATE_SNAPSHOTS);
    }

    private static GoalDefinition carrying(String key, String name, String goalLine, MavenFlag flag)
    {
        GoalDefinition definition = GoalDefinition.of(name, goalLine);

        definition.id = key;
        definition.template.flags.add(flag);

        return definition;
    }

    public void restoreDefaults()
    {
        seedDefaults(defaults());
    }
}
