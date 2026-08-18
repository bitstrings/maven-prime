package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.swing.Icon;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;

public final class CommandToggleAction
    extends ToggleAction
    implements DumbAware
{
    private final BooleanSupplier getter;

    private final Consumer<Boolean> setter;

    public CommandToggleAction(
        String text, String description, Icon icon, BooleanSupplier getter, Consumer<Boolean> setter)
    {
        super(text, description, icon);

        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public boolean isSelected(AnActionEvent event)
    {
        return getter.getAsBoolean();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean selected)
    {
        setter.accept(Boolean.valueOf(selected));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.EDT;
    }
}
