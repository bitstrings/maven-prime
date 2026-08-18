package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.intellij.codeInsight.hints.declarative.CollapseState;
import com.intellij.codeInsight.hints.declarative.CollapsiblePresentationTreeBuilder;
import com.intellij.codeInsight.hints.declarative.InlayActionData;
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder;

import kotlin.Unit;

public final class InlaySegments
{
    static final int MAX_SEGMENT_LENGTH = 30;

    static final String ELLIPSIS = "…";

    private InlaySegments()
    {
    }

    public static void render(
        PresentationTreeBuilder builder, String prefix, String value, InlayActionData action)
    {
        if ((prefix.length() + value.length()) <= MAX_SEGMENT_LENGTH)
        {
            builder.text(prefix + value, action);

            return;
        }

        builder.collapsibleList(
            CollapseState.Collapsed,
            expanded -> expand(expanded, prefix, value, action),
            collapsed -> collapse(collapsed, prefix + head(value, prefix.length())));
    }

    private static Unit expand(
        CollapsiblePresentationTreeBuilder builder, String prefix, String value, InlayActionData action)
    {
        if (StringUtils.isNotEmpty(prefix))
        {
            builder.toggleButton(toggle -> text(toggle, prefix));
        }

        for (String segment : of(value))
        {
            builder.text(segment, action);
        }

        return Unit.INSTANCE;
    }

    private static Unit collapse(CollapsiblePresentationTreeBuilder builder, String placeholder)
    {
        builder.toggleButton(toggle -> text(toggle, placeholder));

        return Unit.INSTANCE;
    }

    private static Unit text(PresentationTreeBuilder builder, String text)
    {
        builder.text(text, null);

        return Unit.INSTANCE;
    }

    static List<String> of(String value)
    {
        if (value.length() <= MAX_SEGMENT_LENGTH)
        {
            return List.of(value);
        }

        List<String> segments = new ArrayList<>();

        for (int start = 0; start < value.length(); start += MAX_SEGMENT_LENGTH)
        {
            segments.add(value.substring(start, Math.min(start + MAX_SEGMENT_LENGTH, value.length())));
        }

        return segments;
    }

    static String head(String value, int prefixLength)
    {
        int budget = MAX_SEGMENT_LENGTH - prefixLength - ELLIPSIS.length();

        return (budget <= 0)
            ? ELLIPSIS
            : (value.substring(0, Math.min(budget, value.length())) + ELLIPSIS);
    }
}
