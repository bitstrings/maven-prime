package org.bitstrings.idea.plugins.mavenprime.config;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalIds;

public final class GoalConfig
{
    public String id;

    public String name;

    public RequestConfig request;

    public GoalConfig()
    {
    }

    public static GoalConfig of(GoalDefinition definition)
    {
        GoalConfig config = new GoalConfig();

        config.id = definition.id;
        config.name = definition.name;
        config.request = RequestConfig.of(definition.template);

        return config;
    }

    public GoalDefinition toDefinition()
    {
        GoalDefinition definition = new GoalDefinition();

        definition.name = StringUtils.trimToNull(name);
        definition.template = (request == null) ? definition.template : request.toRequest();
        definition.id =
            StringUtils.isNotBlank(id) ? id.trim() : GoalIds.slug(definition.getGoalLine());

        return definition;
    }
}
