package org.bitstrings.idea.plugins.mavenprime.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperty;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;

import com.intellij.openapi.project.Project;

public final class SharedConfigTemplate
{
    private SharedConfigTemplate()
    {
    }

    public static MavenPrimeConfig of(Project project)
    {
        BuildContext context = BuildContext.getInstance(project);

        return MavenPrimeConfig.of(
            ContextConfig.of(
                context.getProfiles(),
                valuesOf(context.getProperties()),
                context.getEnvironment(),
                context.isForceClean()),
            GoalRegistry.getInstance(project).projectScoped());
    }

    private static Map<String, String> valuesOf(List<BuildContextProperty> properties)
    {
        Map<String, String> values = new LinkedHashMap<>();

        for (BuildContextProperty property : properties)
        {
            if (property.isDefined())
            {
                values.put(property.key.trim(), StringUtils.defaultString(property.value));
            }
        }

        return values;
    }
}
