package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.execution.CommandLineRenderer;

import com.intellij.util.execution.ParametersListUtil;

public final class ImporterVmOptions
{
    static final String LEGACY_BEGIN = "-Dmavenprime.managed.begin";

    static final String LEGACY_END = "-Dmavenprime.managed.end";

    private ImporterVmOptions()
    {
    }

    public static String rendered(List<BuildContextProperty> properties)
    {
        return ParametersListUtil.join(render(properties));
    }

    // Removal matches the tokens last written and nothing else, so an edited field is left as it is.
    public static String merge(String existing, String written, String region)
    {
        if (StringUtils.isEmpty(written) && StringUtils.isEmpty(region))
        {
            return StringUtils.defaultString(existing);
        }

        List<String> tokens = new ArrayList<>(ParametersListUtil.parse(StringUtils.defaultString(existing)));

        remove(tokens, ParametersListUtil.parse(StringUtils.defaultString(written)));

        tokens.addAll(ParametersListUtil.parse(StringUtils.defaultString(region)));

        return ParametersListUtil.join(tokens);
    }

    public static String stripLegacy(String existing)
    {
        String text = StringUtils.defaultString(existing);

        List<String> tokens = new ArrayList<>(ParametersListUtil.parse(text));

        int begin = tokens.indexOf(LEGACY_BEGIN);

        if (begin < 0)
        {
            return text;
        }

        int end = tokens.indexOf(LEGACY_END);

        tokens.subList(begin, ((end < begin) ? begin : end) + 1).clear();

        return ParametersListUtil.join(tokens);
    }

    private static void remove(List<String> tokens, List<String> written)
    {
        int at = lastIndexOf(tokens, written);

        if (at >= 0)
        {
            tokens.subList(at, at + written.size()).clear();
        }
    }

    // merge appends, so only the trailing match is ours. Taking the first strands our token in the
    // user's field for good, and ImporterVmOptionsTest pins which one goes.
    private static int lastIndexOf(List<String> tokens, List<String> written)
    {
        if (written.isEmpty())
        {
            return -1;
        }

        for (int start = tokens.size() - written.size(); start >= 0; start--)
        {
            if (tokens.subList(start, start + written.size()).equals(written))
            {
                return start;
            }
        }

        return -1;
    }

    private static List<String> render(List<BuildContextProperty> properties)
    {
        List<String> definitions = new ArrayList<>(properties.size());

        for (BuildContextProperty property : properties)
        {
            if (property.isDefined() && property.appliesToImport)
            {
                definitions.add(define(property));
            }
        }

        return definitions;
    }

    private static String define(BuildContextProperty property)
    {
        return CommandLineRenderer.renderProperty(property.key.trim(), property.value);
    }
}
