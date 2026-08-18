package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.List;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

public final class RegisteredActions
{
    private RegisteredActions()
    {
    }

    public static void addAll(DefaultActionGroup group, List<String> actionIds)
    {
        for (String actionId : actionIds)
        {
            add(group, actionId);
        }
    }

    public static void add(DefaultActionGroup group, String actionId)
    {
        AnAction action = ActionManager.getInstance().getAction(actionId);

        if (action != null)
        {
            group.add(action);
        }
    }
}
