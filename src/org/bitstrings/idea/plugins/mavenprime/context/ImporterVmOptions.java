package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.execution.CommandLineRenderer;

import com.intellij.util.execution.ParametersListUtil;

public final class ImporterVmOptions
{
    static final String BEGIN = "-Dmavenprime.managed.begin";

    static final String END = "-Dmavenprime.managed.end";

    private ImporterVmOptions()
    {
    }

    public static String merge(String existing, List<BuildContextProperty> properties)
    {
        String preserved = strip(existing);

        List<String> region = render(properties);

        if (region.isEmpty())
        {
            return preserved;
        }

        region.add(0, BEGIN);
        region.add(END);

        String encoded = ParametersListUtil.join(region);

        return preserved.isEmpty() ? encoded : (preserved + ' ' + encoded);
    }

    public static String strip(String existing)
    {
        String text = StringUtils.defaultString(existing);

        int begin = text.indexOf(BEGIN);

        if (begin < 0)
        {
            return text.trim();
        }

        int end = text.indexOf(END, begin);

        if (end < 0)
        {
            return text.substring(0, begin).trim();
        }

        return (text.substring(0, begin).trim() + ' ' + text.substring(end + END.length()).trim()).trim();
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
