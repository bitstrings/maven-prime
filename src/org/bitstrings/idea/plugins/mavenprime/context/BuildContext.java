package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.config.ContextConfig;
import org.bitstrings.idea.plugins.mavenprime.config.MavenPrimeConfigService;
import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenFlag;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenGoalVocabulary;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.bitstrings.idea.plugins.mavenprime.util.MavenImports;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.bitstrings.idea.plugins.mavenprime.util.UiTopics;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.project.MavenImportingSettings;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;

@Service(Service.Level.PROJECT)
@State(name = "MavenPrimeBuildContext", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class BuildContext
    implements PersistentStateComponent<BuildContextProperties>, Disposable
{
    private static final boolean PUSH = true;

    private static final boolean ABSORB_ONLY = false;

    public static final Topic<BuildContextListener> TOPIC =
        Topic.create("Maven Prime build context", BuildContextListener.class);

    public static final Topic<ModelListener> MODEL_TOPIC =
        Topic.create("Maven Prime build context model", ModelListener.class);

    private static final int REIMPORT_MERGE_MILLIS = 300;

    private static final int CHANGE_MERGE_MILLIS = 500;

    private final Project project;

    final MergingUpdateQueue reimportQueue;

    private final MergingUpdateQueue changeQueue;

    private final Object stateLock = new Object();

    private BuildContextProperties state = new BuildContextProperties();

    private SharedSnapshot lastShared;

    public BuildContext(Project project)
    {
        this.project = project;
        this.reimportQueue =
            new MergingUpdateQueue(
                "MavenPrimeBuildContext",
                REIMPORT_MERGE_MILLIS,
                true,
                MergingUpdateQueue.ANY_COMPONENT,
                this);
        this.changeQueue =
            new MergingUpdateQueue(
                "MavenPrimeBuildContextChange",
                CHANGE_MERGE_MILLIS,
                true,
                MergingUpdateQueue.ANY_COMPONENT,
                this);

        this.changeQueue.setPassThrough(ApplicationManager.getApplication().isUnitTestMode());

        project
            .getMessageBus()
            .connect(this)
            .subscribe(
                MavenPrimeConfigService.TOPIC,
                (MavenPrimeConfigService.ConfigListener) this::sharedContextChanged);
    }

    @Override
    public void dispose()
    {
    }

    public static BuildContext getInstance(Project project)
    {
        return project.getService(BuildContext.class);
    }

    @Override
    public BuildContextProperties getState()
    {
        synchronized (stateLock)
        {
            return state.copy();
        }
    }

    @Override
    public void loadState(BuildContextProperties loaded)
    {
        synchronized (stateLock)
        {
            state = loaded;

            if (state.environment == null)
            {
                state.environment = new EnvironmentOverride();
            }

            if (state.properties == null)
            {
                state.properties = new ArrayList<>();
            }

            if (state.ignoredProperties == null)
            {
                state.ignoredProperties = new ArrayList<>();
            }

            if (state.profiles == null)
            {
                state.profiles = new ArrayList<>();
            }

            if (state.pushedProfiles == null)
            {
                state.pushedProfiles = new ArrayList<>();
            }
        }
    }

    public void listen()
    {
        MavenImports.onModelAvailable(project, this, this::sync, this::absorb);

        sync();
    }

    public void sync()
    {
        removeLegacyImporterOptions();

        if (!MavenProjects.isMavenized(project))
        {
            return;
        }

        syncProfiles(UnaryOperator.identity(), PUSH);
    }

    // Reached only from a finished import: reconcile with what the IDE resolved, never write back.
    public void absorb()
    {
        removeLegacyImporterOptions();

        if (MavenProjects.isMavenized(project))
        {
            syncProfiles(UnaryOperator.identity(), ABSORB_ONLY);
        }

        contextChanged();
    }

    public BuildContextEnvironment getEnvironment()
    {
        BuildContextEnvironment shared = shared().getEnvironment();

        synchronized (stateLock)
        {
            return ContextLayers.environment(shared, state.environment);
        }
    }

    public void setEnvironment(BuildContextEnvironment environment)
    {
        BuildContextEnvironment shared = shared().getEnvironment();

        boolean resolutionChanged;

        synchronized (stateLock)
        {
            resolutionChanged =
                !ContextLayers.environment(shared, state.environment).resolvesLike(environment);

            state.environment = ContextLayers.narrowEnvironment(shared, environment);
        }

        if (resolutionChanged)
        {
            modelChanged();
        }

        contextChanged();
    }

    public BuildContextEnvironment getSharedEnvironment()
    {
        return shared().getEnvironment();
    }

    public EnvironmentOverride getEnvironmentOverride()
    {
        synchronized (stateLock)
        {
            return state.environment.copy();
        }
    }

    public boolean hasSharedContext()
    {
        ContextConfig shared = shared();

        return !shared.getProfiles().isEmpty()
            || !shared.getProperties().isEmpty()
            || (shared.forceClean != null)
            || shared.getEnvironment().isDeclared();
    }

    public DistributionSpec getDistribution()
    {
        return getEnvironment().getDistribution();
    }

    public boolean isForceClean()
    {
        Boolean shared = shared().forceClean;

        synchronized (stateLock)
        {
            return ContextLayers.forceClean(shared, state.forceClean);
        }
    }

    public void setForceClean(boolean forceClean)
    {
        Boolean shared = shared().forceClean;

        synchronized (stateLock)
        {
            state.forceClean = ContextLayers.narrowForceClean(shared, forceClean);
        }

        contextChanged();
    }

    public Set<String> getAvailableProfiles()
    {
        return MavenProjects.allProfiles(project);
    }

    public Map<String, Boolean> getProfiles()
    {
        Map<String, Boolean> shared = shared().getProfiles();
        Map<String, Boolean> explicit = explicitProfiles();
        Set<String> available = getAvailableProfiles();

        synchronized (stateLock)
        {
            return ContextLayers.profiles(shared, absorbed(explicit, available));
        }
    }

    public Map<String, ContextOrigin> profileOrigins()
    {
        Map<String, Boolean> shared = ContextLayers.declaredProfiles(shared().getProfiles());
        Map<String, Boolean> explicit = explicitProfiles();
        Set<String> available = getAvailableProfiles();

        synchronized (stateLock)
        {
            return ContextLayers.originsOf(
                shared.keySet(),
                ContextLayers
                    .asMap(ContextLayers.narrowProfiles(shared, absorbed(explicit, available)))
                    .keySet());
        }
    }

    public void setProfile(String name, Boolean enabled)
    {
        syncProfiles(overrides -> ContextLayers.withOverride(overrides, name, enabled), PUSH);

        reimport();

        modelChanged();

        contextChanged();
    }

    public void clearOverrides()
    {
        Map<String, Boolean> explicit = explicitProfiles();

        synchronized (stateLock)
        {
            state.environment = new EnvironmentOverride();
            state.properties.clear();
            state.ignoredProperties.clear();
            state.profiles.clear();
            state.pushedProfiles = ContextLayers.asOverrides(explicit);
            state.forceClean = null;
        }

        syncProfiles(UnaryOperator.identity(), PUSH);

        reimport();

        modelChanged();

        contextChanged();
    }

    public List<BuildContextProperty> getProperties()
    {
        Map<String, String> shared = shared().getProperties();

        synchronized (stateLock)
        {
            return ContextLayers.properties(shared, state.properties, state.ignoredProperties);
        }
    }

    public Map<String, ContextOrigin> propertyOrigins()
    {
        Map<String, String> shared = ContextLayers.declaredProperties(shared().getProperties());

        synchronized (stateLock)
        {
            return ContextLayers.originsOf(
                shared.keySet(), ContextLayers.overriddenKeys(state.properties));
        }
    }

    public String getProperty(String key)
    {
        return ContextLayers.valueOf(getProperties(), key);
    }

    public void setProperties(List<BuildContextProperty> properties)
    {
        Map<String, String> shared = shared().getProperties();

        synchronized (stateLock)
        {
            state.properties = ContextLayers.narrowProperties(shared, properties);
            state.ignoredProperties.removeAll(ContextLayers.keysOf(properties));
        }

        modelChanged();

        contextChanged();
    }

    public void removeProperty(String key)
    {
        String name = StringUtils.trimToEmpty(key);

        boolean declared = ContextLayers.declaredProperties(shared().getProperties()).containsKey(name);

        synchronized (stateLock)
        {
            state.properties.removeIf(override -> override.key.trim().equals(name));

            if (declared && !state.ignoredProperties.contains(name))
            {
                state.ignoredProperties.add(name);
            }
        }

        modelChanged();

        contextChanged();
    }

    public void applyTo(MavenPrimeRequest request)
    {
        getProfiles().forEach(request.profiles::putIfAbsent);

        for (BuildContextProperty property : getProperties())
        {
            if (property.isDefined())
            {
                request.properties.putIfAbsent(property.key.trim(), property.value);
            }
        }

        BuildContextEnvironment environment = getEnvironment();

        request.settingsFile = inherit(request.settingsFile, environment.settingsFile);
        request.jreName = inherit(request.jreName, environment.jreName);
        request.vmOptions = inherit(request.vmOptions, environment.vmOptions);

        inheritIdeToggles(request);
    }

    private void inheritIdeToggles(MavenPrimeRequest request)
    {
        if (!request.selectsTests() && MavenRunner.getInstance(project).getSettings().isSkipTests())
        {
            request.flags.add(MavenFlag.SKIP_TESTS);
        }

        if (MavenProjectsManager.getInstance(project).getGeneralSettings().isWorkOffline())
        {
            request.flags.add(MavenFlag.OFFLINE);
        }
    }

    public MavenPrimeRequest effective(MavenPrimeRequest request)
    {
        MavenPrimeRequest effective = request.copy();

        applyTo(effective);
        prependForcedClean(effective);

        return effective;
    }

    private void prependForcedClean(MavenPrimeRequest request)
    {
        if (
            !isForceClean()
                || request.goals.contains(MavenGoalVocabulary.CLEAN_PHASE)
                || !MavenGoalVocabulary.hasPhase(request.goals)
        )
        {
            return;
        }

        request.goals.add(0, MavenGoalVocabulary.CLEAN_PHASE);
    }

    private static String inherit(String requested, String fallback)
    {
        return StringUtils.trimToNull(StringUtils.isNotBlank(requested) ? requested : fallback);
    }

    private ContextConfig shared()
    {
        if (!MavenPrimeSettings.getInstance(project).sharedContext)
        {
            return new ContextConfig();
        }

        ContextConfig defaults = MavenPrimeConfigService.getInstance(project).getDefaults();

        return (defaults == null) ? new ContextConfig() : defaults;
    }

    public void sharedContextChanged()
    {
        SharedSnapshot current = sharedSnapshot();

        synchronized (stateLock)
        {
            if (current.equals(lastShared))
            {
                return;
            }

            lastShared = current;
        }

        sync();

        reimport();

        modelChanged();

        contextChanged();
    }

    private SharedSnapshot sharedSnapshot()
    {
        ContextConfig shared = shared();

        return new SharedSnapshot(
            ContextLayers.declaredProfiles(shared.getProfiles()),
            ContextLayers.declaredProperties(shared.getProperties()),
            shared.getEnvironment(),
            shared.forceClean);
    }

    private void syncProfiles(UnaryOperator<List<ProfileOverride>> edit, boolean push)
    {
        Map<String, Boolean> shared = shared().getProfiles();
        Map<String, Boolean> explicit = ContextLayers.declaredProfiles(explicitProfiles());
        Set<String> available = getAvailableProfiles();

        Map<String, Boolean> effective;

        synchronized (stateLock)
        {
            state.profiles =
                ContextLayers.narrowProfiles(shared, edit.apply(absorbed(explicit, available)));

            effective = ContextLayers.profiles(shared, state.profiles);

            state.pushedProfiles = ContextLayers.asOverrides(effective);
        }

        if (push && !effective.equals(explicit))
        {
            pushProfiles(effective);
        }
    }

    private List<ProfileOverride> absorbed(Map<String, Boolean> explicit, Set<String> available)
    {
        return ContextLayers.absorb(
            state.profiles, ContextLayers.asMap(state.pushedProfiles), explicit, available);
    }

    private Map<String, Boolean> explicitProfiles()
    {
        MavenExplicitProfiles explicit = MavenProjectsManager.getInstance(project).getExplicitProfiles();

        Map<String, Boolean> profiles = new LinkedHashMap<>();

        for (String name : explicit.getEnabledProfiles())
        {
            profiles.put(name, Boolean.TRUE);
        }

        for (String name : explicit.getDisabledProfiles())
        {
            profiles.put(name, Boolean.FALSE);
        }

        return profiles;
    }

    private void pushProfiles(Map<String, Boolean> profiles)
    {
        Set<String> enabled = new LinkedHashSet<>();
        Set<String> disabled = new LinkedHashSet<>();

        profiles.forEach((profile, on) -> (on.booleanValue() ? enabled : disabled).add(profile));

        MavenProjectsManager
            .getInstance(project)
            .setExplicitProfiles(new MavenExplicitProfiles(enabled, disabled));
    }

    // The push re-resolves what the IDE already knows; only a forced update finds a module a
    // profile just added.
    private void reimport()
    {
        if (!MavenPrimeSettings.getInstance(project).autoRefresh)
        {
            return;
        }

        reimportQueue.queue(Update.create(this, this::forceReimport));
    }

    public int applyToImporter()
    {
        MavenImportingSettings importing = MavenProjectsManager.getInstance(project).getImportingSettings();
        MavenPrimeSettings settings = MavenPrimeSettings.getInstance(project);

        String existing = importing.getVmOptionsForImporter();
        String region = ImporterVmOptions.rendered(getProperties());
        String merged = ImporterVmOptions.merge(existing, settings.importerVmOptions, region);

        settings.importerVmOptions = region;

        if (!merged.equals(existing))
        {
            importing.setVmOptionsForImporter(merged);
        }

        return ParametersListUtil.parse(region).size();
    }

    // The region used to be fenced by two -D markers that reached the importer JVM as real properties.
    private void removeLegacyImporterOptions()
    {
        MavenImportingSettings importing = MavenProjectsManager.getInstance(project).getImportingSettings();

        String existing = importing.getVmOptionsForImporter();
        String cleaned = ImporterVmOptions.stripLegacy(existing);

        if (!cleaned.equals(existing))
        {
            importing.setVmOptionsForImporter(cleaned);
        }
    }

    private void contextChanged()
    {
        changeQueue.queue(
            Update.create(TOPIC, () -> UiTopics.publish(project, TOPIC, BuildContextListener::buildContextChanged)));
    }

    private void modelChanged()
    {
        changeQueue.queue(
            Update.create(
                MODEL_TOPIC,
                () -> UiTopics.publish(project, MODEL_TOPIC, ModelListener::buildModelChanged)));
    }

    private record SharedSnapshot(
        Map<String, Boolean> profiles,
        Map<String, String> properties,
        BuildContextEnvironment environment,
        Boolean forceClean)
    {
    }

    public interface BuildContextListener
    {
        void buildContextChanged();
    }

    public interface ModelListener
    {
        void buildModelChanged();
    }

    public void forceReimport()
    {
        MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
    }
}
