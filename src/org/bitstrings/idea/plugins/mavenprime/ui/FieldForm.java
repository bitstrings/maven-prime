package org.bitstrings.idea.plugins.mavenprime.ui;

import java.awt.GridBagConstraints;

import javax.swing.JComponent;
import javax.swing.JTextField;

import com.intellij.util.ui.FormBuilder;

public final class FieldForm
    extends FormBuilder
{
    private FieldForm()
    {
    }

    public static FormBuilder create()
    {
        return new FieldForm();
    }

    @Override
    protected int getFill(JComponent component)
    {
        return (component instanceof JTextField)
            ? GridBagConstraints.HORIZONTAL
            : super.getFill(component);
    }
}
