package org.bitstrings.idea.plugins.mavenprime.ui;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;

public final class DetailsPanel
    extends JPanel
{
    private static final long serialVersionUID = 1L;

    private static final int SECTION_GAP = 8;

    private static final int ROW_INDENT = 12;

    public DetailsPanel()
    {
        setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));
        setBorder(JBUI.Borders.empty(6, 8));
    }

    public void clear()
    {
        removeAll();
    }

    public void title(String text)
    {
        add(line(null, text, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES, 0, 0));
    }

    public void section(String text)
    {
        add(line(null, text, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES, 0, SECTION_GAP));
    }

    public void row(Icon icon, String label, String value, SimpleTextAttributes valueAttributes)
    {
        SimpleColoredComponent component = new SimpleColoredComponent();

        component.setIcon(icon);

        if (StringUtils.isNotBlank(label))
        {
            component.append(label + ' ', SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        component.append(StringUtils.defaultString(value), valueAttributes);
        component.setBorder(JBUI.Borders.emptyLeft(ROW_INDENT));

        add(component);
    }

    public void note(Icon icon, String text, SimpleTextAttributes attributes)
    {
        add(line(icon, text, attributes, ROW_INDENT, 0));
    }

    public void done()
    {
        revalidate();
        repaint();
    }

    private Component line(
        Icon icon, String text, SimpleTextAttributes attributes, int indent, int topGap)
    {
        SimpleColoredComponent component = new SimpleColoredComponent();

        component.setIcon(icon);
        component.append(StringUtils.defaultString(text), attributes);
        component.setBorder(JBUI.Borders.empty(topGap, indent, 0, 0));

        return component;
    }
}
