package org.bitstrings.idea.plugins.mavenprime.ui;

import static com.intellij.ui.dsl.builder.UtilsKt.DEFAULT_COMMENT_WIDTH;

import java.awt.FontMetrics;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

public final class HintLabel
{
    private HintLabel()
    {
    }

    public static JComponent forKey(String bundleKey, Object... params)
    {
        JBLabel hint =
            new JBLabel(StringUtils.EMPTY, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);

        String text = MavenPrimeBundle.message(bundleKey, params);
        FontMetrics metrics = hint.getFontMetrics(hint.getFont());
        int commentWidth = metrics.charWidth('0') * DEFAULT_COMMENT_WIDTH;

        hint.setText(metrics.stringWidth(text) > commentWidth ? bounded(commentWidth, text) : text);

        return hint;
    }

    public static JComponent under(JCheckBox anchor, String bundleKey, Object... params)
    {
        JComponent hint = forKey(bundleKey, params);

        hint.setBorder(
            new EmptyBorder(
                0,
                UIUtil.getCheckBoxTextHorizontalOffset(anchor),
                JBUI.scale(UIUtil.DEFAULT_VGAP),
                0));

        return hint;
    }

    public static String bounded(int width, String text)
    {
        return "<html><div width=" + width + ">" + StringUtil.escapeXmlEntities(text) + "</div></html>";
    }
}
