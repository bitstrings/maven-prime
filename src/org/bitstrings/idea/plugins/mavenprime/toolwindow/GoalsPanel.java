package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.RequestSummary;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalActions;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRegistry;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalRunner;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalScope;
import org.bitstrings.idea.plugins.mavenprime.goals.ScopedGoal;
import org.bitstrings.idea.plugins.mavenprime.ui.CommandAction;
import org.bitstrings.idea.plugins.mavenprime.ui.GoalEditorDialog;
import org.bitstrings.idea.plugins.mavenprime.ui.GoalPresentation;
import org.bitstrings.idea.plugins.mavenprime.ui.ProjectTrustBanner;
import org.bitstrings.idea.plugins.mavenprime.util.ProjectTrust;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;

public final class GoalsPanel
    extends JPanel
    implements UiDataProvider
{
    private static final long serialVersionUID = 1L;

    private static final int GLOBAL_COLUMN_WIDTH = 60;

    private static final int OPTIONS_COLUMN_WIDTH = 220;

    private static final double GOAL_COLUMN_RATIO = 1.2d;

    private final transient Project project;

    private final transient Supplier<MavenProject> moduleSupplier;

    private final transient ListTableModel<ScopedGoal> model;

    private final TableView<ScopedGoal> goalTable;

    private final EditorNotificationPanel untrusted = ProjectTrustBanner.create();

    public GoalsPanel(Project project, Supplier<MavenProject> moduleSupplier)
    {
        super(new BorderLayout());

        this.project = project;
        this.moduleSupplier = moduleSupplier;
        this.model = new GoalTableModel(project);
        this.goalTable = new TableView<>(model);

        configureTable();

        add(untrusted, BorderLayout.NORTH);
        add(
            ToolbarDecorator
                .createDecorator(goalTable)
                .setAddAction(button -> addGoal())
                .setEditAction(button -> editGoal())
                .setRemoveAction(button -> removeGoal())
                .setMoveUpAction(button -> moveGoal(-1))
                .setMoveDownAction(button -> moveGoal(1))
                .setMoveUpActionUpdater(event -> canMoveGoal(-1))
                .setMoveDownActionUpdater(event -> canMoveGoal(1))
                .addExtraAction(cloneAction())
                .addExtraAction(executeAction(ExecutionMode.RUN))
                .addExtraAction(executeAction(ExecutionMode.DEBUG))
                .createPanel(),
            BorderLayout.CENTER);

        refresh();
    }

    @SuppressWarnings("rawtypes")
    private static ColumnInfo[] columns()
    {
        return new ColumnInfo[]
        {
            new GoalColumn(),
            new OptionsColumn(),
            new GlobalColumn()
        };
    }

    private void configureTable()
    {
        goalTable
            .getColumnModel()
            .getColumn(0)
            .setPreferredWidth(JBUI.scale((int) (OPTIONS_COLUMN_WIDTH * GOAL_COLUMN_RATIO)));
        goalTable.getColumnModel().getColumn(1).setPreferredWidth(JBUI.scale(OPTIONS_COLUMN_WIDTH));
        goalTable.getColumnModel().getColumn(2).setMaxWidth(JBUI.scale(GLOBAL_COLUMN_WIDTH));

        new DoubleClickListener()
        {
            @Override
            protected boolean onDoubleClick(MouseEvent event)
            {
                return run(ExecutionMode.RUN);
            }
        }.installOn(goalTable);
    }

    public void refresh()
    {
        ProjectTrustBanner.update(untrusted, ProjectTrust.isTrusted(project));

        ScopedGoal previous = goalTable.getSelectedObject();

        String previousId = (previous == null) ? null : previous.definition().id;

        List<ScopedGoal> goals = new ArrayList<>(GoalRegistry.getInstance(project).all());

        model.setItems(goals);

        goals
            .stream()
            .filter(goal -> goal.definition().id.equals(previousId))
            .findFirst()
            .ifPresent(goal -> goalTable.setSelection(List.of(goal)));
    }

    @Override
    public void uiDataSnapshot(DataSink sink)
    {
        MavenProject module = moduleSupplier.get();

        if (module != null)
        {
            sink.set(CommonDataKeys.VIRTUAL_FILE, module.getFile());
        }
    }

    private boolean run(ExecutionMode mode)
    {
        ScopedGoal goal = goalTable.getSelectedObject();

        if ((goal == null) || (moduleSupplier.get() == null))
        {
            return false;
        }

        ActionManager actions = ActionManager.getInstance();

        AnAction registered = actions.getAction(GoalActions.actionIdOf(goal.definition().id, mode));

        if (registered == null)
        {
            return GoalRunner.run(project, goal, moduleSupplier.get(), mode);
        }

        actions.tryToExecute(registered, null, this, ActionPlaces.TOOLWINDOW_CONTENT, true);

        return true;
    }

    private void addGoal()
    {
        createGoal(new GoalDefinition(), GoalScope.PROJECT);
    }

    private void createGoal(GoalDefinition definition, GoalScope scope)
    {
        GoalEditorDialog dialog = new GoalEditorDialog(project, definition, scope);

        if (dialog.showAndGet())
        {
            String added = GoalRegistry.getInstance(project).add(dialog.getScope(), dialog.getDefinition());

            refresh();

            reveal(added);
        }
    }

    void reveal(String goalId)
    {
        model
            .getItems()
            .stream()
            .filter(goal -> goal.definition().id.equals(goalId))
            .findFirst()
            .ifPresent(
                goal ->
                {
                    goalTable.setSelection(List.of(goal));

                    TableUtil.scrollSelectionToVisible(goalTable);
                });
    }

    private void cloneGoal()
    {
        ScopedGoal selected = goalTable.getSelectedObject();

        if (selected == null)
        {
            return;
        }

        GoalDefinition copy = selected.definition().copy();

        copy.name =
            MavenPrimeBundle.message("mavenprime.goals.clone.name", selected.definition().getDisplayName());

        createGoal(copy, selected.scope());
    }

    private void editGoal()
    {
        ScopedGoal selected = goalTable.getSelectedObject();

        if (selected == null)
        {
            return;
        }

        GoalEditorDialog dialog = new GoalEditorDialog(project, selected.definition(), selected.scope());

        if (!dialog.showAndGet())
        {
            return;
        }

        GoalRegistry registry = GoalRegistry.getInstance(project);

        if (dialog.getScope() == selected.scope())
        {
            registry.replace(selected.scope(), dialog.getDefinition());
        }
        else
        {
            registry.remove(selected.scope(), selected.definition());
            registry.add(dialog.getScope(), dialog.getDefinition());
        }

        refresh();
    }

    private void removeGoal()
    {
        ScopedGoal selected = goalTable.getSelectedObject();

        if (selected != null)
        {
            GoalRegistry.getInstance(project).remove(selected.scope(), selected.definition());

            refresh();
        }
    }

    private void moveGoal(int delta)
    {
        ScopedGoal selected = goalTable.getSelectedObject();

        if ((selected != null)
            && GoalRegistry.getInstance(project).move(selected.scope(), selected.definition().id, delta))
        {
            refresh();
        }
    }

    private boolean canMoveGoal(int delta)
    {
        ScopedGoal selected = goalTable.getSelectedObject();

        return (selected != null)
            && GoalRegistry.getInstance(project).canMove(selected.scope(), selected.definition().id, delta);
    }

    private CommandAction cloneAction()
    {
        return new CommandAction(
            MavenPrimeBundle.message("mavenprime.goals.clone"),
            MavenPrimeBundle.message("mavenprime.goals.clone.description"),
            AllIcons.Actions.Copy,
            this::cloneGoal,
            () -> goalTable.getSelectedObject() != null);
    }

    private ExecuteGoalAction executeAction(ExecutionMode mode)
    {
        return new ExecuteGoalAction(
            mode,
            () -> (goalTable.getSelectedObject() != null) && (moduleSupplier.get() != null),
            this::run);
    }

    private static final class GoalTableModel
        extends ListTableModel<ScopedGoal>
    {
        private static final long serialVersionUID = 1L;

        private final transient Project project;

        GoalTableModel(Project project)
        {
            super(columns());

            this.project = project;
        }

        @Override
        public boolean canExchangeRows(int oldIndex, int newIndex)
        {
            ScopedGoal moved = getRowValue(oldIndex);
            ScopedGoal target = getRowValue(newIndex);

            return (moved != null) && (target != null) && (moved.scope() == target.scope());
        }

        @Override
        public void exchangeRows(int oldIndex, int newIndex)
        {
            ScopedGoal moved = getRowValue(oldIndex);

            if (
                (moved == null)
                    || !GoalRegistry
                        .getInstance(project)
                        .move(moved.scope(), moved.definition().id, newIndex - oldIndex)
            )
            {
                return;
            }

            List<ScopedGoal> reordered = new ArrayList<>(getItems());

            reordered.add(newIndex, reordered.remove(oldIndex));

            setItems(reordered);
        }
    }

    private static final class GoalColumn
        extends ColumnInfo<ScopedGoal, ScopedGoal>
    {
        private final GoalCellRenderer renderer = new GoalCellRenderer();

        GoalColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.goals.column.goal"));
        }

        @Override
        public ScopedGoal valueOf(ScopedGoal goal)
        {
            return goal;
        }

        @Override
        public TableCellRenderer getRenderer(ScopedGoal goal)
        {
            return renderer;
        }
    }

    private static final class GoalCellRenderer
        extends ColoredTableCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void customizeCellRenderer(
            JTable table, Object value, boolean selected, boolean hasFocus, int row, int column)
        {
            if (value instanceof ScopedGoal)
            {
                GoalPresentation.appendTo(this, (ScopedGoal) value);
            }
        }
    }

    private static final class OptionsColumn
        extends ColumnInfo<ScopedGoal, String>
    {
        OptionsColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.goals.column.options"));
        }

        @Override
        public String valueOf(ScopedGoal goal)
        {
            return RequestSummary.of(goal.definition().template);
        }
    }

    private static final class GlobalColumn
        extends ColumnInfo<ScopedGoal, Boolean>
    {
        GlobalColumn()
        {
            super(MavenPrimeBundle.message("mavenprime.goals.column.global"));
        }

        @Override
        public Boolean valueOf(ScopedGoal goal)
        {
            return Boolean.valueOf(goal.scope() == GoalScope.GLOBAL);
        }

        @Override
        public Class<?> getColumnClass()
        {
            return Boolean.class;
        }
    }
}
