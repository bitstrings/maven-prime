package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.swing.Icon;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;

public final class ViewToggleAction
    extends ToggleAction
    implements DumbAware
{
    private final BooleanSupplier shown;

    private final Consumer<Boolean> onToggled;

    public ViewToggleAction(
        String text, String description, Icon icon, BooleanSupplier shown, Consumer<Boolean> onToggled)
    {
        super(text, description, icon);

        this.shown = shown;
        this.onToggled = onToggled;
    }

    @Override
    public boolean isSelected(AnActionEvent event)
    {
        return shown.getAsBoolean();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean selected)
    {
        onToggled.accept(Boolean.valueOf(selected));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.EDT;
    }
}
