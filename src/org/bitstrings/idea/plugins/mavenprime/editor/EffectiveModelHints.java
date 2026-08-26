package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.model.EffectiveModel;
import org.bitstrings.idea.plugins.mavenprime.model.ModelOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ModuleFacts;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PluginOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PropertyOrigin;

public final class EffectiveModelHints
{
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^{}]+)}");

    private static final String DEFAULT_TYPE = "jar";

    private static final String DEFAULT_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

    private final EffectiveModel model;

    private final ModuleFacts facts;

    public EffectiveModelHints(EffectiveModel model, ModuleFacts facts)
    {
        this.model = model;
        this.facts = facts;
    }

    public Hint propertyValue(String text)
    {
        String source = StringUtils.defaultString(text);

        Matcher placeholders = PLACEHOLDER.matcher(source);

        StringBuilder resolved = new StringBuilder();

        ModelOrigin origin = ModelOrigin.UNKNOWN;

        int resolvedCount = 0;
        int copiedUpTo = 0;

        while (placeholders.find())
        {
            String name = placeholders.group(1);

            PropertyOrigin declared = model.propertyOf(facts.moduleKey(), name);

            String value = StringUtils.defaultIfBlank(facts.expression(name), valueOf(declared));

            if (StringUtils.isBlank(value))
            {
                return null;
            }

            resolved.append(source, copiedUpTo, placeholders.start()).append(value);

            origin =
                ((resolvedCount == 0) && (declared != null)) ? declared.origin() : ModelOrigin.UNKNOWN;
            resolvedCount++;
            copiedUpTo = placeholders.end();
        }

        if (resolvedCount == 0)
        {
            return null;
        }

        resolved.append(source, copiedUpTo, source.length());

        return new Hint(resolved.toString(), (resolvedCount == 1) ? origin : ModelOrigin.UNKNOWN);
    }

    public Hint managedVersion(String groupId, String artifactId, String type, String classifier)
    {
        if (StringUtils.isAnyBlank(groupId, artifactId))
        {
            return null;
        }

        DependencyOrigin declared =
            model.dependencyOf(facts.moduleKey(), managementKey(groupId, artifactId, type, classifier));

        return hintOf(
            facts.dependencyVersion(groupId, artifactId),
            (declared == null) ? null : declared.value(),
            (declared == null) ? ModelOrigin.UNKNOWN : declared.origin());
    }

    public Hint managedPluginVersion(String groupId, String artifactId)
    {
        if (StringUtils.isBlank(artifactId))
        {
            return null;
        }

        PluginOrigin declared = model.pluginOf(facts.moduleKey(), pluginKey(groupId, artifactId));

        return hintOf(
            facts.pluginVersion(groupId, artifactId),
            (declared == null) ? null : declared.value(),
            (declared == null) ? ModelOrigin.UNKNOWN : declared.origin());
    }

    // The IDE re-resolves its model on every import; a recorded run only says what a past one saw, so
    // preferring the recording renders a value the reader can no longer reproduce from the poms.
    private static Hint hintOf(String live, String recorded, ModelOrigin origin)
    {
        String value = StringUtils.defaultIfBlank(live, recorded);

        return StringUtils.isBlank(value) ? null : new Hint(value, origin);
    }

    private static String valueOf(PropertyOrigin declared)
    {
        return (declared == null) ? null : declared.value();
    }

    static String managementKey(String groupId, String artifactId, String type, String classifier)
    {
        String key = groupId + ':' + artifactId + ':' + StringUtils.defaultIfBlank(type, DEFAULT_TYPE);

        return StringUtils.isBlank(classifier) ? key : (key + ':' + classifier);
    }

    static String pluginKey(String groupId, String artifactId)
    {
        return StringUtils.defaultIfBlank(groupId, DEFAULT_PLUGIN_GROUP_ID) + ':' + artifactId;
    }

    public record Hint(String text, ModelOrigin origin)
    {
    }
}
