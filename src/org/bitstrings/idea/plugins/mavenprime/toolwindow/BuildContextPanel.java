package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.config.MavenPrimeConfigService;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextEnvironment;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperty;
import org.bitstrings.idea.plugins.mavenprime.context.ContextOrigin;
import org.bitstrings.idea.plugins.mavenprime.context.EnvironmentOverride;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.ui.BuildEnvironmentDialog;
import org.bitstrings.idea.plugins.mavenprime.ui.BuildPropertyDialog;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileEntries;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileEntry;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileRows;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileState;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileStateChooser;
import org.bitstrings.idea.plugins.mavenprime.ui.ProjectTrustBanner;
import org.bitstrings.idea.plugins.mavenprime.util.ProjectTrust;

import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.NamedColorUtil;
import com.intellij.util.ui.tree.TreeUtil;

public final class BuildContextPanel
    extends JPanel
{
    private static final long serialVersionUID = 1L;

    private static final String LOCATION_SEPARATOR = " - ";

    private static final String VALUE_SEPARATOR = " = ";

    private static final SimpleTextAttributes IMPORT_ATTRIBUTES =
        new SimpleTextAttributes(
            SimpleTextAttributes.STYLE_ITALIC,
            JBColor.namedColor(
                "MavenPrime.Context.importForeground", NamedColorUtil.getInactiveTextColor()));

    private final transient Project project;

    private final transient DefaultMutableTreeNode root = new DefaultMutableTreeNode();

    private final transient DefaultMutableTreeNode environmentNode =
        new DefaultMutableTreeNode(ContextSection.ENVIRONMENT);

    private final transient DefaultMutableTreeNode profilesNode =
        new DefaultMutableTreeNode(ContextSection.PROFILES);

    private final transient DefaultMutableTreeNode propertiesNode =
        new DefaultMutableTreeNode(ContextSection.PROPERTIES);

    final transient DefaultTreeModel model;

    final Tree tree;

    private final transient ProfileStateChooser chooser;

    private final EditorNotificationPanel untrusted = ProjectTrustBanner.create();

    private final transient Map<String, ContextOrigin> origins = new HashMap<>();

    private transient BuildContextEnvironment environment = new BuildContextEnvironment();

    public BuildContextPanel(Project project)
    {
        super(new BorderLayout());

        this.project = project;
        this.model = new DefaultTreeModel(root);
        this.tree = new Tree(model);
        this.chooser = new ProfileStateChooser(tree, this::applyProfileState);

        root.add(environmentNode);
        root.add(profilesNode);
        root.add(propertiesNode);

        configureTree();

        add(untrusted, BorderLayout.NORTH);
        add(
            ToolbarDecorator
                .createDecorator(tree)
                .setAddAction(button -> addProperty())
                .setEditAction(button -> edit())
                .setRemoveAction(button -> removeProperty())
                .setEditActionUpdater(event -> isEditable())
                .setRemoveActionUpdater(event -> selectedProperty() != null)
                .disableUpDownActions()
                .createPanel(),
            BorderLayout.CENTER);

        refresh();

        TreeUtil.expandAll(tree);
    }

    private void configureTree()
    {
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ContextRenderer());

        chooser.install();

        new DoubleClickListener()
        {
            @Override
            protected boolean onDoubleClick(MouseEvent event)
            {
                return !chooser.isOverCheckbox(event) && edit();
            }
        }.installOn(tree);
    }

    boolean applyProfileState(TreePath path, ProfileState state)
    {
        ProfileEntry entry = ProfileEntries.at(path);

        if (entry == null)
        {
            return false;
        }

        saveProfile(entry, state);

        model.nodeChanged(nodeOf(path));

        return true;
    }

    public void refresh()
    {
        ProjectTrustBanner.update(untrusted, ProjectTrust.isTrusted(project));

        TreeState state = TreeState.createOn(tree, root);

        BuildContext context = BuildContext.getInstance(project);

        BuildContextEnvironment shared = context.getSharedEnvironment();

        EnvironmentOverride override = context.getEnvironmentOverride();

        environment = context.getEnvironment();
        origins.clear();

        environmentNode.removeAllChildren();

        for (EnvironmentField field : EnvironmentField.values())
        {
            origins.put(
                originKey(ContextSection.ENVIRONMENT, field.name()), originOf(field, shared, override));

            environmentNode.add(new DefaultMutableTreeNode(field));
        }

        profilesNode.removeAllChildren();

        Map<String, ContextOrigin> profileOrigins = context.profileOrigins();

        for (ProfileEntry entry
            : ProfileEntries.merge(context.getAvailableProfiles(), context.getProfiles()))
        {
            origins.put(
                originKey(ContextSection.PROFILES, entry.getName()),
                profileOrigins.getOrDefault(entry.getName(), ContextOrigin.LOCAL));

            profilesNode.add(new DefaultMutableTreeNode(entry));
        }

        propertiesNode.removeAllChildren();

        Map<String, ContextOrigin> propertyOrigins = context.propertyOrigins();

        for (BuildContextProperty property : context.getProperties())
        {
            origins.put(
                originKey(ContextSection.PROPERTIES, property.key),
                propertyOrigins.getOrDefault(property.key.trim(), ContextOrigin.LOCAL));

            propertiesNode.add(new DefaultMutableTreeNode(property));
        }

        model.reload();

        state.applyTo(tree, root);
    }

    private static ContextOrigin originOf(
        EnvironmentField field, BuildContextEnvironment shared, EnvironmentOverride override)
    {
        if (!field.isDeclaredIn(shared))
        {
            return ContextOrigin.LOCAL;
        }

        return field.isOverriddenIn(override) ? ContextOrigin.OVERRIDDEN : ContextOrigin.SHARED;
    }

    private static String originKey(ContextSection section, String name)
    {
        return section.name() + ':' + StringUtils.trimToEmpty(name);
    }

    private boolean isEditable()
    {
        return (selectedProperty() != null) || (selectedEnvironmentField() != null);
    }

    private boolean edit()
    {
        return (selectedEnvironmentField() != null) ? editEnvironment() : editProperty();
    }

    public boolean editEnvironment()
    {
        BuildEnvironmentDialog dialog = new BuildEnvironmentDialog(project, environment);

        if (!dialog.showAndGet())
        {
            return false;
        }

        environment = dialog.getEnvironment();

        BuildContext.getInstance(project).setEnvironment(environment);

        model.nodeStructureChanged(environmentNode);

        return true;
    }

    private void addProperty()
    {
        BuildPropertyDialog dialog = BuildPropertyDialog.forBuildContext(project, new BuildContextProperty());

        if (dialog.showAndGet())
        {
            DefaultMutableTreeNode added = new DefaultMutableTreeNode(propertyFrom(dialog));

            propertiesNode.add(added);

            model.nodeStructureChanged(propertiesNode);

            TreeUtil.selectPath(tree, new TreePath(added.getPath()));

            saveProperties();
        }
    }

    private boolean editProperty()
    {
        BuildContextProperty selected = selectedProperty();

        if (selected == null)
        {
            return false;
        }

        BuildPropertyDialog dialog = BuildPropertyDialog.forBuildContext(project, selected);

        return dialog.showAndGet() && applyPropertyEdit(selected.key, propertyFrom(dialog));
    }

    boolean applyPropertyEdit(String key, BuildContextProperty edited)
    {
        DefaultMutableTreeNode node = propertyNodeFor(key);

        if (node == null)
        {
            MavenPrimeNotifications.warning(
                project, MavenPrimeBundle.message("mavenprime.context.property.gone", key));

            return false;
        }

        node.setUserObject(edited);

        model.nodeChanged(node);

        saveProperties();

        return true;
    }

    private DefaultMutableTreeNode propertyNodeFor(String key)
    {
        for (int child = 0; child < propertiesNode.getChildCount(); child++)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) propertiesNode.getChildAt(child);

            if (((BuildContextProperty) node.getUserObject()).key.trim().equals(StringUtils.trimToEmpty(key)))
            {
                return node;
            }
        }

        return null;
    }

    private static BuildContextProperty propertyFrom(BuildPropertyDialog dialog)
    {
        return BuildContextProperty.of(dialog.getKey(), dialog.getValue(), dialog.appliesToImport());
    }

    private void removeProperty()
    {
        DefaultMutableTreeNode node = selectedNode();

        if ((node == null) || !(node.getUserObject() instanceof BuildContextProperty))
        {
            return;
        }

        String key = ((BuildContextProperty) node.getUserObject()).key;

        propertiesNode.remove(node);

        model.nodeStructureChanged(propertiesNode);

        BuildContext.getInstance(project).removeProperty(key);
    }

    private void saveProperties()
    {
        List<BuildContextProperty> properties = new ArrayList<>(propertiesNode.getChildCount());

        for (int index = 0; index < propertiesNode.getChildCount(); index++)
        {
            properties.add(
                (BuildContextProperty) ((DefaultMutableTreeNode) propertiesNode.getChildAt(index)).getUserObject());
        }

        BuildContext.getInstance(project).setProperties(properties);
    }

    private void saveProfile(ProfileEntry entry, ProfileState state)
    {
        entry.setState(state);

        BuildContext.getInstance(project).setProfile(entry.getName(), state.toEnabled());
    }

    private BuildContextProperty selectedProperty()
    {
        DefaultMutableTreeNode node = selectedNode();

        Object row = (node == null) ? null : node.getUserObject();

        return (row instanceof BuildContextProperty) ? (BuildContextProperty) row : null;
    }

    private EnvironmentField selectedEnvironmentField()
    {
        DefaultMutableTreeNode node = selectedNode();

        Object row = (node == null) ? null : node.getUserObject();

        return (row instanceof EnvironmentField) ? (EnvironmentField) row : null;
    }

    private DefaultMutableTreeNode selectedNode()
    {
        return nodeOf(tree.getSelectionPath());
    }

    private static DefaultMutableTreeNode nodeOf(TreePath path)
    {
        return (path == null) ? null : (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    enum ContextSection
    {
        ENVIRONMENT("environment"),
        PROFILES("profiles"),
        PROPERTIES("properties");

        private final String bundleKey;

        ContextSection(String bundleKey)
        {
            this.bundleKey = bundleKey;
        }

        String getTitle()
        {
            return MavenPrimeBundle.message("mavenprime.context." + bundleKey);
        }
    }

    enum EnvironmentField
    {
        DISTRIBUTION("distribution"),
        SETTINGS_FILE("settingsFile"),
        JRE("jre"),
        VM_OPTIONS("vmOptions");

        private final String bundleKey;

        EnvironmentField(String bundleKey)
        {
            this.bundleKey = bundleKey;
        }

        String getTitle()
        {
            return MavenPrimeBundle.message("mavenprime.context.environment." + bundleKey);
        }

        boolean isDeclaredIn(BuildContextEnvironment shared)
        {
            return (this == DISTRIBUTION)
                ? !shared.rawDistribution().isContext()
                : StringUtils.isNotBlank(valueOf(shared));
        }

        boolean isOverriddenIn(EnvironmentOverride override)
        {
            return switch (this)
            {
                case DISTRIBUTION -> override.distribution != null;
                case SETTINGS_FILE -> override.settingsFile != null;
                case JRE -> override.jreName != null;
                case VM_OPTIONS -> override.vmOptions != null;
            };
        }

        String valueOf(BuildContextEnvironment environment)
        {
            return switch (this)
            {
                case DISTRIBUTION -> describe(environment.getDistribution());
                case SETTINGS_FILE -> environment.settingsFile;
                case JRE -> environment.jreName;
                case VM_OPTIONS -> environment.vmOptions;
            };
        }

        private static String describe(DistributionSpec spec)
        {
            return spec.kind.requiresPath()
                ? (spec.kind.getTitle() + LOCATION_SEPARATOR + spec.path)
                : spec.kind.getTitle();
        }
    }

    private final class ContextRenderer
        extends CheckboxTreeBase.CheckboxTreeCellRendererBase
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void customizeRenderer(
            JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

            if (userObject instanceof ContextSection)
            {
                getTextRenderer().append(
                    ((ContextSection) userObject).getTitle(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            }
            else if (userObject instanceof EnvironmentField)
            {
                EnvironmentField field = (EnvironmentField) userObject;

                appendEntry(field.getTitle(), field.valueOf(environment), true);

                appendOrigin(ContextSection.ENVIRONMENT, field.name());
            }
            else if (userObject instanceof ProfileEntry)
            {
                appendOrigin(ContextSection.PROFILES, ProfileRows.render(this, value).getName());
            }
            else if (userObject instanceof BuildContextProperty)
            {
                BuildContextProperty property = (BuildContextProperty) userObject;

                appendEntry(property.key, property.value, false);

                if (property.appliesToImport)
                {
                    getTextRenderer().append(
                        ' ' + MavenPrimeBundle.message("mavenprime.context.marker.import"), IMPORT_ATTRIBUTES);
                }

                appendOrigin(ContextSection.PROPERTIES, property.key);
            }
        }

        private void appendEntry(String name, String value, boolean alwaysSeparated)
        {
            getTextRenderer().append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);

            boolean valued = StringUtils.isNotBlank(value);

            if (valued || alwaysSeparated)
            {
                getTextRenderer().append(VALUE_SEPARATOR, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }

            if (valued)
            {
                getTextRenderer().append(value, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }

        private void appendOrigin(ContextSection section, String name)
        {
            ContextOrigin origin = origins.get(originKey(section, name));

            if ((origin == null) || (origin == ContextOrigin.LOCAL))
            {
                return;
            }

            getTextRenderer().append(
                LOCATION_SEPARATOR
                    + MavenPrimeBundle.message(
                        (origin == ContextOrigin.SHARED)
                            ? "mavenprime.context.origin.shared"
                            : "mavenprime.context.origin.overridden",
                        MavenPrimeConfigService.FILE_NAME),
                SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }
}
