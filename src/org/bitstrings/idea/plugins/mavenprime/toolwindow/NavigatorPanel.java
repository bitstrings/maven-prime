package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeExecutor;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.bitstrings.idea.plugins.mavenprime.util.UiState;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.util.ui.tree.TreeUtil;

public final class NavigatorPanel
    extends JPanel
    implements Disposable
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getInstance(NavigatorPanel.class);

    private static final String STATE_PREFIX = "mavenPrime.navigator.";

    private static final String TREE_STATE_KEY = "tree";

    private static final String TREE_STATE_ELEMENT = "navigator";

    private final transient Project project;

    private final transient UiState state;

    private final transient StructureTreeModel<SimpleTreeStructure> structureModel;

    private final SimpleTree tree = new SimpleTree();

    public NavigatorPanel(Project project)
    {
        super(new BorderLayout());

        this.project = project;
        this.state = new UiState(project, STATE_PREFIX);
        this.structureModel = new StructureTreeModel<>(new NavigatorStructure(project), this);

        tree.setModel(new AsyncTreeModel(structureModel, this));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        TreeSpeedSearch.installOn(tree);

        new DoubleClickListener()
        {
            @Override
            protected boolean onDoubleClick(MouseEvent event)
            {
                return run(ExecutionMode.RUN);
            }
        }.installOn(tree);

        add(
            ToolbarDecorator
                .createDecorator(tree)
                .disableAddAction()
                .disableRemoveAction()
                .disableUpDownActions()
                .addExtraAction(executeAction(ExecutionMode.RUN))
                .addExtraAction(executeAction(ExecutionMode.DEBUG))
                .createPanel(),
            BorderLayout.CENTER);

        restoreState();
    }

    @Override
    public void dispose()
    {
        saveState();
    }

    public void refresh()
    {
        rebuildThenApply(TreeState.createOn(tree));
    }

    private void rebuildThenApply(TreeState treeState)
    {
        structureModel
            .invalidateAsync()
            .thenRun(
                () ->
                    ApplicationManager
                        .getApplication()
                        .invokeLater(() -> treeState.applyTo(tree), project.getDisposed()));
    }

    private void saveState()
    {
        Element element = new Element(TREE_STATE_ELEMENT);

        TreeState.createOn(tree).writeExternal(element);

        state.setText(TREE_STATE_KEY, JDOMUtil.write(element), StringUtils.EMPTY);
    }

    private void restoreState()
    {
        String stored = state.getText(TREE_STATE_KEY, StringUtils.EMPTY);

        if (StringUtils.isBlank(stored))
        {
            return;
        }

        try
        {
            rebuildThenApply(TreeState.createFrom(JDOMUtil.load(stored)));
        }
        catch (IOException | JDOMException unreadable)
        {
            LOG.debug("Maven Prime could not restore the navigator tree", unreadable);
        }
    }

    private RunnableNode getSelectedNode()
    {
        return TreeUtil.getLastUserObject(RunnableNode.class, tree.getSelectionPath());
    }

    private boolean run(ExecutionMode mode)
    {
        RunnableNode selected = getSelectedNode();

        if (selected == null)
        {
            return false;
        }

        MavenProject module = selected.getModule();

        MavenPrimeRequest request =
            MavenPrimeRequest.in(module.getDirectory(), List.of(selected.getGoalLine()));

        request.name =
            MavenPrimeBundle.message(
                "mavenprime.navigator.runName", module.getDisplayName(), selected.getGoalLine());

        MavenPrimeExecutor.getInstance(project).execute(request, mode);

        return true;
    }

    private ExecuteGoalAction executeAction(ExecutionMode mode)
    {
        return new ExecuteGoalAction(mode, () -> getSelectedNode() != null, this::run);
    }

    private static final class NavigatorStructure
        extends SimpleTreeStructure
    {
        private final RootNode root;

        NavigatorStructure(Project project)
        {
            this.root = new RootNode(project);
        }

        @Override
        public SimpleNode getRootElement()
        {
            return root;
        }
    }

    private static final class RootNode
        extends SimpleNode
    {
        RootNode(Project project)
        {
            super(project);
        }

        @Override
        public SimpleNode[] getChildren()
        {
            List<MavenProject> modules = new ArrayList<>(MavenProjects.all(getProject()));

            modules.sort(Comparator.comparing(MavenProject::getDisplayName, String.CASE_INSENSITIVE_ORDER));

            List<SimpleNode> children = new ArrayList<>(modules.size());

            for (MavenProject module : modules)
            {
                children.add(new ModuleNode(this, module));
            }

            return children.toArray(new SimpleNode[0]);
        }
    }

    private static final class ModuleNode
        extends SimpleNode
    {
        private final MavenProject module;

        ModuleNode(SimpleNode parent, MavenProject module)
        {
            super(parent);

            this.module = module;
        }

        @Override
        public String getName()
        {
            return module.getDisplayName();
        }

        @Override
        protected void update(PresentationData presentation)
        {
            presentation.addText(module.getDisplayName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            presentation.addText(
                StringUtils.SPACE + module.getMavenId().getGroupId(),
                SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            presentation.setIcon(AllIcons.Nodes.Module);
        }

        @Override
        public SimpleNode[] getChildren()
        {
            return new SimpleNode[] { new LifecycleNode(this, module) };
        }
    }

    private static final class LifecycleNode
        extends SimpleNode
    {
        private final MavenProject module;

        LifecycleNode(SimpleNode parent, MavenProject module)
        {
            super(parent);

            this.module = module;
        }

        @Override
        public String getName()
        {
            return MavenPrimeBundle.message("mavenprime.navigator.lifecycle");
        }

        @Override
        protected void update(PresentationData presentation)
        {
            presentation.addText(getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            presentation.setIcon(AllIcons.Nodes.ConfigFolder);
        }

        @Override
        public SimpleNode[] getChildren()
        {
            List<SimpleNode> phases = new ArrayList<>(MavenConstants.PHASES.size());

            for (String phase : MavenConstants.PHASES)
            {
                phases.add(new RunnableNode(this, module, phase));
            }

            return phases.toArray(new SimpleNode[0]);
        }
    }

    private static final class RunnableNode
        extends SimpleNode
    {
        private final MavenProject module;

        private final String goalLine;

        RunnableNode(SimpleNode parent, MavenProject module, String goalLine)
        {
            super(parent);

            this.module = module;
            this.goalLine = goalLine;
        }

        MavenProject getModule()
        {
            return module;
        }

        String getGoalLine()
        {
            return goalLine;
        }

        @Override
        public String getName()
        {
            return goalLine;
        }

        @Override
        protected void update(PresentationData presentation)
        {
            presentation.addText(goalLine, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            presentation.setIcon(AllIcons.Actions.Execute);
        }

        @Override
        public SimpleNode[] getChildren()
        {
            return NO_CHILDREN;
        }

        @Override
        public boolean isAlwaysLeaf()
        {
            return true;
        }
    }
}
