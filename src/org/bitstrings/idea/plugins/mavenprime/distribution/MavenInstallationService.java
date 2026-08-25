package org.bitstrings.idea.plugins.mavenprime.distribution;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenDistribution;
import org.jetbrains.idea.maven.server.MavenDistributionsCache;
import org.jetbrains.idea.maven.utils.MavenUtil;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.SystemProperties;

@Service(Service.Level.PROJECT)
public final class MavenInstallationService
{
    private final Project project;

    private final Map<DistributionSpec, MavenInstallation> cache = new ConcurrentHashMap<>();

    private final Map<String, MavenInstallation> ideCache = new ConcurrentHashMap<>();

    private volatile List<Path> daemonHomes;

    public MavenInstallationService(Project project)
    {
        this.project = project;
    }

    public static MavenInstallationService getInstance(Project project)
    {
        return project.getService(MavenInstallationService.class);
    }

    public MavenInstallation resolve(DistributionSpec spec, String workingDirectory)
    {
        DistributionSpec effective =
            spec.isContext() ? BuildContext.getInstance(project).getDistribution() : spec;

        if (!effective.kind.requiresPath())
        {
            return resolveIdeCached(workingDirectory);
        }

        MavenInstallation cached = cache.get(effective);

        if (cached != null)
        {
            return cached;
        }

        MavenInstallation resolved =
            (effective.kind == DistributionKind.MVND_HOME)
                ? resolveDaemonHome(effective.path)
                : resolveMavenHome(effective.path);

        if (resolved.isValid())
        {
            cache.put(effective.copy(), resolved);
        }

        return resolved;
    }

    public Map<DistributionSpec, MavenInstallation> detectDistributions(
        Collection<DistributionSpec> additional, String workingDirectory)
    {
        Map<DistributionSpec, MavenInstallation> detected = new LinkedHashMap<>();

        DistributionSpec ide = DistributionSpec.ide();

        detected.put(ide, resolve(ide, workingDirectory));

        for (Path daemonHome : detectDaemonHomes())
        {
            DistributionSpec spec = DistributionSpec.daemonHome(daemonHome.toString());

            detected.put(spec, resolve(spec, workingDirectory));
        }

        for (DistributionSpec spec : additional)
        {
            detected.computeIfAbsent(spec.copy(), candidate -> resolve(candidate, workingDirectory));
        }

        return detected;
    }

    public void detectDistributions(
        Collection<DistributionSpec> additional,
        String workingDirectory,
        Consumer<Map<DistributionSpec, MavenInstallation>> onDetected)
    {
        ApplicationManager
            .getApplication()
            .executeOnPooledThread(
                () ->
                {
                    Map<DistributionSpec, MavenInstallation> detected =
                        detectDistributions(additional, workingDirectory);

                    ApplicationManager
                        .getApplication()
                        .invokeLater(
                            () -> onDetected.accept(detected),
                            ModalityState.any(),
                            project.getDisposed());
                });
    }

    public List<Path> detectDaemonHomes()
    {
        List<Path> known = daemonHomes;

        if (known == null)
        {
            known = MvndHomeDetector.detect(Paths.get(SystemProperties.getUserHome()));
            daemonHomes = known;
        }

        return known;
    }

    private MavenInstallation resolveIdeCached(String workingDirectory)
    {
        String key = StringUtils.defaultString(workingDirectory) + '\u0000' + ideMavenHome();

        MavenInstallation cached = ideCache.get(key);

        if (cached != null)
        {
            return cached;
        }

        MavenInstallation resolved = resolveIde(workingDirectory);

        if (resolved.isValid())
        {
            ideCache.put(key, resolved);
        }

        return resolved;
    }

    private String ideMavenHome()
    {
        return StringUtils.defaultString(
            MavenProjectsManager.getInstance(project).getGeneralSettings().getMavenHomeType().getTitle());
    }

    public void invalidateDaemonHomes()
    {
        daemonHomes = null;
    }

    public void invalidate()
    {
        cache.clear();
        ideCache.clear();

        invalidateDaemonHomes();

        MavenDistributionsCache.getInstance(project).cleanCaches();
    }

    private MavenInstallation resolveIde(String workingDirectory)
    {
        MavenDistributionsCache distributions = MavenDistributionsCache.getInstance(project);

        MavenDistribution distribution =
            StringUtils.isBlank(workingDirectory)
                ? distributions.getSettingsDistribution()
                : distributions.getMavenDistribution(workingDirectory);

        Path home = distribution.getMavenHome();

        if (MvndLayout.isDaemonHome(home))
        {
            return resolveDaemonHome(home.toString());
        }

        return MavenInstallation.ide(home, MavenVersion.parse(distribution.getVersion()));
    }

    private MavenInstallation resolveMavenHome(String path)
    {
        if (StringUtils.isBlank(path))
        {
            return MavenInstallation.invalid(
                DistributionKind.MAVEN_HOME, MavenPrimeBundle.message("mavenprime.distribution.error.noPath"));
        }

        Path home = Paths.get(path).toAbsolutePath().normalize();

        MavenVersion version = MavenVersion.parse(MavenUtil.getMavenVersion(home.toString()));

        if (!version.isKnown())
        {
            return MavenInstallation.invalid(
                DistributionKind.MAVEN_HOME, MavenPrimeBundle.message("mavenprime.distribution.error.notMaven", home));
        }

        return MavenInstallation.maven(home, version);
    }

    private MavenInstallation resolveDaemonHome(String path)
    {
        if (StringUtils.isBlank(path))
        {
            return MavenInstallation.invalid(
                DistributionKind.MVND_HOME, MavenPrimeBundle.message("mavenprime.distribution.error.noPath"));
        }

        Path home = Paths.get(path).toAbsolutePath().normalize();

        Path executable = MvndLayout.findExecutable(home);

        if (executable == null)
        {
            return MavenInstallation.invalid(
                DistributionKind.MVND_HOME, MavenPrimeBundle.message("mavenprime.distribution.error.notDaemon", home));
        }

        if (!MvndLayout.isDaemonDistribution(home))
        {
            return MavenInstallation.invalid(
                DistributionKind.MVND_HOME,
                MavenPrimeBundle.message("mavenprime.distribution.error.noEmbeddedMavenHome", home));
        }

        return MavenInstallation.daemon(
            home,
            executable,
            MavenVersion.parse(MavenUtil.getMavenVersion(MvndLayout.embeddedMavenHome(home).toString())),
            MvndLayout.daemonVersion(home));
    }
}
