package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.function.BooleanSupplier;

import javax.swing.Icon;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

public final class CommandAction
    extends DumbAwareAction
{
    private static final BooleanSupplier ALWAYS = () -> true;

    private final Runnable command;

    private final BooleanSupplier enabled;

    public CommandAction(String text, String description, Icon icon, Runnable command)
    {
        this(text, description, icon, command, ALWAYS);
    }

    public CommandAction(
        String text, String description, Icon icon, Runnable command, BooleanSupplier enabled)
    {
        super(text, description, icon);

        this.command = command;
        this.enabled = enabled;
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        command.run();
    }

    @Override
    public void update(AnActionEvent event)
    {
        event.getPresentation().setEnabled(enabled.getAsBoolean());
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.EDT;
    }
}
