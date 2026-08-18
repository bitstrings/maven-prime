package org.bitstrings.idea.plugins.mavenprime.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeNotifications;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.bitstrings.idea.plugins.mavenprime.util.ProjectTrust;
import org.bitstrings.idea.plugins.mavenprime.util.ReloadableCache;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;
import org.jetbrains.idea.maven.project.MavenProject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.intellij.ide.trustedProjects.TrustedProjectsListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.PathUtil;
import com.intellij.util.messages.Topic;

@Service(Service.Level.PROJECT)
public final class MavenPrimeConfigService
    implements Disposable
{
    public static final String FILE_NAME = ".mavenprime.json";

    public static final Topic<ConfigListener> TOPIC =
        Topic.create("Maven Prime project configuration", ConfigListener.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Project project;

    private final ReloadableCache<Snapshot> snapshot;

    public MavenPrimeConfigService(Project project)
    {
        this.project = project;
        this.snapshot = new ReloadableCache<>(this::load);

        project
            .getMessageBus()
            .connect(this)
            .subscribe(
                VirtualFileManager.VFS_CHANGES,
                new BulkFileListener()
                {
                    @Override
                    public void after(List<? extends VFileEvent> events)
                    {
                        invalidateIfConfigChanged(events);
                    }
                });

        ApplicationManager
            .getApplication()
            .getMessageBus()
            .connect(this)
            .subscribe(
                TrustedProjectsListener.TOPIC,
                new TrustedProjectsListener()
                {
                    @Override
                    public void onProjectTrusted(Project trusted)
                    {
                        reloadFor(trusted);
                    }

                    @Override
                    public void onProjectUntrusted(Project untrusted)
                    {
                        reloadFor(untrusted);
                    }
                });
    }

    public static MavenPrimeConfigService getInstance(Project project)
    {
        return project.getService(MavenPrimeConfigService.class);
    }

    @Override
    public void dispose()
    {
    }

    public void invalidate()
    {
        snapshot.invalidate();
    }

    public ContextConfig getDefaults()
    {
        return snapshot.get().config().defaults;
    }

    public List<GoalDefinition> getGoals()
    {
        List<GoalDefinition> definitions = snapshot.get().goals();

        List<GoalDefinition> copies = new ArrayList<>(definitions.size());

        for (GoalDefinition definition : definitions)
        {
            copies.add(definition.copy());
        }

        return copies;
    }

    public boolean hasGoal(String goalId)
    {
        return (goalId != null) && snapshot.get().goalIds().contains(goalId);
    }

    public void setGoals(List<GoalDefinition> goals)
    {
        if (goals.isEmpty() && (findConfigFile() == null))
        {
            return;
        }

        if (write(MavenPrimeConfig.of(snapshot.get().config().defaults, goals)))
        {
            invalidate();
        }
    }

    public VirtualFile findConfigFile()
    {
        VirtualFile directory = rootDirectory();

        return (directory == null) ? null : directory.findChild(FILE_NAME);
    }

    private void reloadFor(Project changed)
    {
        if (!project.equals(changed))
        {
            return;
        }

        invalidate();

        UiTopics.publish(project, TOPIC, ConfigListener::configChanged);
    }

    private void invalidateIfConfigChanged(List<? extends VFileEvent> events)
    {
        for (VFileEvent event : events)
        {
            if (FILE_NAME.equals(PathUtil.getFileName(event.getPath()))
                && event.getPath().equals(configFilePath()))
            {
                invalidate();

                UiTopics.publish(project, TOPIC, ConfigListener::configChanged);

                return;
            }
        }
    }

    private String configFilePath()
    {
        VirtualFile directory = rootDirectory();

        return (directory == null) ? null : (directory.getPath() + '/' + FILE_NAME);
    }

    private Snapshot load()
    {
        return snapshotOf(ProjectTrust.isTrusted(project));
    }

    Snapshot snapshotOf(boolean trusted)
    {
        if (!trusted)
        {
            return Snapshot.of(new MavenPrimeConfig(), false);
        }

        VirtualFile file = findConfigFile();

        if (file == null)
        {
            return Snapshot.of(new MavenPrimeConfig(), true);
        }

        try
        {
            MavenPrimeConfig parsed = GSON.fromJson(VfsUtil.loadText(file), MavenPrimeConfig.class);

            if (parsed == null)
            {
                return Snapshot.of(new MavenPrimeConfig(), true);
            }

            if (parsed.version > MavenPrimeConfig.CURRENT_VERSION)
            {
                MavenPrimeNotifications.warning(
                    project,
                    MavenPrimeBundle.message(
                        "mavenprime.config.newerVersion",
                        FILE_NAME,
                        Integer.valueOf(parsed.version),
                        Integer.valueOf(MavenPrimeConfig.CURRENT_VERSION)));

                return Snapshot.of(parsed, false);
            }

            return Snapshot.of(parsed, true);
        }
        catch (IOException | JsonParseException failure)
        {
            MavenPrimeNotifications.error(
                project, MavenPrimeBundle.message("mavenprime.config.readFailed", FILE_NAME, failure.getMessage()));

            return Snapshot.of(new MavenPrimeConfig(), false);
        }
    }

    private boolean write(MavenPrimeConfig config)
    {
        if (!ProjectTrust.isTrusted(project))
        {
            MavenPrimeNotifications.error(
                project, MavenPrimeBundle.message("mavenprime.config.untrusted", FILE_NAME));

            return false;
        }

        if (!snapshot.get().writable())
        {
            MavenPrimeNotifications.error(
                project, MavenPrimeBundle.message("mavenprime.config.notWritable", FILE_NAME));

            return false;
        }

        VirtualFile directory = rootDirectory();

        if (directory == null)
        {
            MavenPrimeNotifications.error(project, MavenPrimeBundle.message("mavenprime.config.noRoot", FILE_NAME));

            return false;
        }

        String text = GSON.toJson(config);

        try
        {
            WriteAction.run(() -> save(directory, text));

            return true;
        }
        catch (IOException failure)
        {
            MavenPrimeNotifications.error(
                project, MavenPrimeBundle.message("mavenprime.config.writeFailed", FILE_NAME, failure.getMessage()));

            return false;
        }
    }

    private void save(VirtualFile directory, String text)
        throws IOException
    {
        VirtualFile file = directory.findChild(FILE_NAME);

        VfsUtil.saveText((file == null) ? directory.createChildData(this, FILE_NAME) : file, text);
    }

    private VirtualFile rootDirectory()
    {
        List<MavenProject> roots = MavenProjects.roots(project);

        if (!roots.isEmpty())
        {
            return roots.get(0).getDirectoryFile();
        }

        String basePath = project.getBasePath();

        return StringUtils.isBlank(basePath) ? null : LocalFileSystem.getInstance().findFileByPath(basePath);
    }

    public interface ConfigListener
    {
        void configChanged();
    }

    record Snapshot(
        MavenPrimeConfig config, List<GoalDefinition> goals, Set<String> goalIds, boolean writable)
    {
        static Snapshot of(MavenPrimeConfig config, boolean writable)
        {
            List<GoalDefinition> goals = config.getGoalDefinitions();

            return new Snapshot(
                config,
                goals,
                goals.stream().map(goal -> goal.id).collect(Collectors.toUnmodifiableSet()),
                writable);
        }
    }
}
