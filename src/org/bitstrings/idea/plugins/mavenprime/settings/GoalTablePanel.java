package org.bitstrings.idea.plugins.mavenprime.settings;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.JPanel;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.goals.ApplicationGoalStore;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalScope;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;
import org.bitstrings.idea.plugins.mavenprime.ui.GoalEditorDialog;
import org.bitstrings.idea.plugins.mavenprime.ui.ScopedGoalRenderer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;

public final class GoalTablePanel
    extends JPanel
{
    private static final long serialVersionUID = 1L;

    private final transient Project project;

    private final transient GoalScope scope;

    private final transient BiConsumer<GoalScope, GoalDefinition> relocate;

    private final transient CollectionListModel<ScopedGoal> model = new CollectionListModel<>();

    final JBList<ScopedGoal> goalList = new JBList<>(model);

    public GoalTablePanel(Project project, GoalScope scope, BiConsumer<GoalScope, GoalDefinition> relocate)
    {
        super(new BorderLayout());

        this.project = project;
        this.scope = scope;
        this.relocate = relocate;

        goalList.setCellRenderer(new ScopedGoalRenderer());

        ToolbarDecorator decorator =
            ToolbarDecorator
                .createDecorator(goalList)
                .setAddAction(button -> addGoal())
                .setEditAction(button -> editGoal())
                .setRemoveAction(button -> removeGoal());

        if (scope == GoalScope.GLOBAL)
        {
            decorator.addExtraAction(new RestoreDefaultsAction());
        }

        add(decorator.createPanel(), BorderLayout.CENTER);
    }

    public void setGoals(List<GoalDefinition> goals)
    {
        List<ScopedGoal> scoped = new ArrayList<>(goals.size());

        for (GoalDefinition definition : goals)
        {
            scoped.add(new ScopedGoal(scope, definition.copy()));
        }

        model.replaceAll(scoped);
    }

    public List<GoalDefinition> getGoals()
    {
        List<GoalDefinition> goals = new ArrayList<>(model.getSize());

        for (ScopedGoal scoped : model.getItems())
        {
            goals.add(scoped.definition());
        }

        return goals;
    }

    public void append(GoalDefinition definition)
    {
        model.add(new ScopedGoal(scope, definition));

        int index = model.getSize() - 1;

        goalList.setSelectedIndex(index);
        goalList.ensureIndexIsVisible(index);
    }

    private void addGoal()
    {
        GoalEditorDialog dialog = new GoalEditorDialog(project, new GoalDefinition(), scope);

        if (!dialog.showAndGet())
        {
            return;
        }

        store(dialog.getScope(), dialog.getDefinition());
    }

    private void editGoal()
    {
        ScopedGoal selected = goalList.getSelectedValue();

        if (selected == null)
        {
            return;
        }

        int index = goalList.getSelectedIndex();

        GoalEditorDialog dialog = new GoalEditorDialog(project, selected.definition(), scope);

        if (!dialog.showAndGet())
        {
            return;
        }

        if (dialog.getScope() == scope)
        {
            model.setElementAt(new ScopedGoal(scope, dialog.getDefinition()), index);

            return;
        }

        model.remove(index);

        relocate.accept(dialog.getScope(), dialog.getDefinition());
    }

    private void store(GoalScope target, GoalDefinition definition)
    {
        if (target == scope)
        {
            append(definition);
        }
        else
        {
            relocate.accept(target, definition);
        }
    }

    private void removeGoal()
    {
        ScopedGoal selected = goalList.getSelectedValue();

        if (selected != null)
        {
            model.remove(selected);
        }
    }

    private final class RestoreDefaultsAction
        extends DumbAwareAction
    {
        RestoreDefaultsAction()
        {
            super(
                MavenPrimeBundle.message("mavenprime.action.restoreGoalDefaults"),
                MavenPrimeBundle.message("mavenprime.action.restoreGoalDefaults.description"),
                AllIcons.General.Reset);
        }

        @Override
        public void actionPerformed(AnActionEvent event)
        {
            setGoals(ApplicationGoalStore.defaults());
        }

        @Override
        public ActionUpdateThread getActionUpdateThread()
        {
            return ActionUpdateThread.BGT;
        }
    }
}
