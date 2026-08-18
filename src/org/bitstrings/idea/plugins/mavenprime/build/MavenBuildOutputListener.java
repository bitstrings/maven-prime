package org.bitstrings.idea.plugins.mavenprime.build;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.BuildFinished;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleResult;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.ModuleStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.MojoStarted;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Problem;
import org.bitstrings.idea.plugins.mavenprime.build.MavenLogEvent.Severity;

import com.intellij.build.events.MessageEvent;
import com.intellij.build.progress.BuildProgress;
import com.intellij.build.progress.BuildProgressDescriptor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;

public final class MavenBuildOutputListener
{
    private final Project project;

    private final BuildProgress<BuildProgressDescriptor> rootProgress;

    private final BuildProgressDescriptor descriptor;

    private final Path workingDirectory;

    private final Object lock = new Object();

    private final Map<String, ModuleNode> modules = new LinkedHashMap<>();

    private String activeModule;

    private boolean buildFailed;

    public MavenBuildOutputListener(
        Project project,
        BuildProgress<BuildProgressDescriptor> rootProgress,
        BuildProgressDescriptor descriptor,
        Path workingDirectory)
    {
        this.project = project;
        this.rootProgress = rootProgress;
        this.descriptor = descriptor;
        this.workingDirectory = workingDirectory;
    }

    public void start()
    {
        synchronized (lock)
        {
            rootProgress.start(descriptor);
        }
    }

    public void fail(String message)
    {
        synchronized (lock)
        {
            rootProgress.fail(System.currentTimeMillis(), message);
        }
    }

    public void finish(int exitCode)
    {
        synchronized (lock)
        {
            closeAll();

            if ((exitCode == 0) && !buildFailed)
            {
                rootProgress.finish();
            }
            else
            {
                rootProgress.fail(
                    System.currentTimeMillis(),
                    MavenPrimeBundle.message("mavenprime.build.failed", exitCode));
            }
        }
    }

    public void accept(MavenLogEvent event)
    {
        synchronized (lock)
        {
            switch (event)
            {
                case ModuleStarted moduleStarted -> startModule(moduleStarted);
                case MojoStarted mojoStarted -> startMojo(mojoStarted);
                case Problem problem -> reportProblem(problem);
                case ModuleResult moduleResult -> reportModuleResult(moduleResult);
                case BuildFinished buildFinished -> buildFailed = !buildFinished.success();
            }
        }
    }

    private void startModule(ModuleStarted event)
    {
        String key = event.groupId() + ':' + event.artifactId();

        close(modules.remove(key));

        modules.put(key, new ModuleNode(rootProgress.startChildProgress(event.artifactId())));

        activeModule = key;
    }

    private void startMojo(MojoStarted event)
    {
        ModuleNode node = modules.get(event.module());

        if (node == null)
        {
            return;
        }

        node.startMojo(mojoTitle(event));

        activeModule = event.module();
    }

    private static String mojoTitle(MojoStarted event)
    {
        return StringUtils.isBlank(event.executionId())
            ? event.coordinates()
            : (event.coordinates() + " (" + event.executionId() + ')');
    }

    private void reportProblem(Problem problem)
    {
        MessageEvent.Kind kind =
            (problem.severity() == Severity.ERROR) ? MessageEvent.Kind.ERROR : MessageEvent.Kind.WARNING;

        ModuleNode node = modules.get(activeModule);

        if ((problem.severity() == Severity.ERROR) && (node != null))
        {
            node.markFailed();
        }

        progressOf(node).message(problem.message(), problem.message(), kind, navigatableOf(problem));
    }

    private void reportModuleResult(ModuleResult result)
    {
        ModuleNode node = modules.remove(result.module());

        if (node != null)
        {
            node.setFailed(!result.success() && !result.skipped());

            close(node);
        }

        if (result.success())
        {
            return;
        }

        String message =
            MavenPrimeBundle.message(
                result.skipped() ? "mavenprime.build.moduleSkipped" : "mavenprime.build.moduleFailed",
                result.module());

        rootProgress.message(
            message,
            StringUtils.isBlank(result.cause()) ? message : (message + ": " + result.cause()),
            result.skipped() ? MessageEvent.Kind.WARNING : MessageEvent.Kind.ERROR,
            null);
    }

    private Navigatable navigatableOf(Problem problem)
    {
        if (!problem.hasPosition())
        {
            return null;
        }

        Path path = resolve(problem.path());

        if (path == null)
        {
            return null;
        }

        VirtualFile file = LocalFileSystem.getInstance().findFileByNioFile(path);

        return (file == null)
            ? null
            : new OpenFileDescriptor(project, file, problem.line() - 1, problem.column() - 1);
    }

    private Path resolve(String path)
    {
        try
        {
            Path resolved = Path.of(path);

            return (resolved.isAbsolute() || (workingDirectory == null))
                ? resolved
                : workingDirectory.resolve(resolved);
        }
        catch (InvalidPathException ignored)
        {
            return null;
        }
    }

    private BuildProgress<BuildProgressDescriptor> progressOf(ModuleNode node)
    {
        return (node == null) ? rootProgress : node.current();
    }

    private void closeAll()
    {
        List<ModuleNode> pending = new ArrayList<>(modules.values());

        modules.clear();
        activeModule = null;

        pending.forEach(MavenBuildOutputListener::close);
    }

    private static void close(ModuleNode node)
    {
        if (node != null)
        {
            node.close();
        }
    }

    private static final class ModuleNode
    {
        private final BuildProgress<BuildProgressDescriptor> progress;

        private BuildProgress<BuildProgressDescriptor> mojo;

        private boolean failed;

        private boolean mojoFailed;

        ModuleNode(BuildProgress<BuildProgressDescriptor> progress)
        {
            this.progress = progress;
        }

        void startMojo(String title)
        {
            closeMojo();

            mojo = progress.startChildProgress(title);
        }

        void markFailed()
        {
            failed = true;
            mojoFailed = true;
        }

        void setFailed(boolean moduleFailed)
        {
            failed = moduleFailed;
        }

        BuildProgress<BuildProgressDescriptor> current()
        {
            return (mojo == null) ? progress : mojo;
        }

        void close()
        {
            closeMojo();

            if (failed)
            {
                progress.fail();
            }
            else
            {
                progress.finish();
            }
        }

        private void closeMojo()
        {
            if (mojo == null)
            {
                return;
            }

            if (mojoFailed)
            {
                mojo.fail();
            }
            else
            {
                mojo.finish();
            }

            mojo = null;
            mojoFailed = false;
        }
    }
}
