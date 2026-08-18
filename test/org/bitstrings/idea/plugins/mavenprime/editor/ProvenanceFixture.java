package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.Map;

import org.bitstrings.idea.plugins.mavenprime.model.EffectiveModel;
import org.bitstrings.idea.plugins.mavenprime.model.ModelOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceData;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PluginOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.PropertyOrigin;
import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceLog;

import com.intellij.openapi.project.Project;

final class ProvenanceFixture
{
    static final String MODULE = "org.example:core";

    static final String REVISION = "1.4.0-SNAPSHOT";

    static final String LONG_VALUE = "com.expretio.appia5.connector.core";

    static final String LANG3_VERSION = "3.14.0";

    static final String LANG3_KEY = "org.apache.commons:commons-lang3:jar";

    static final String COMPILER_PLUGIN_KEY = "org.apache.maven.plugins:maven-compiler-plugin";

    static final String COMPILER_PLUGIN_VERSION = "3.13.0";

    static final String SUREFIRE_PLUGIN_KEY = "org.apache.maven.plugins:maven-surefire-plugin";

    static final String SUREFIRE_PLUGIN_VERSION = "3.2.5";

    static final ModelOrigin PARENT = new ModelOrigin("org.example:parent", "/repo/parent/pom.xml", 27);

    private ProvenanceFixture()
    {
    }

    static EffectiveModelHints hintsFor(Project project)
    {
        return hintsFor(project, Map.of(), Map.of());
    }

    static EffectiveModelHints hintsFor(
        Project project, Map<String, String> properties, Map<String, String> dependencyVersions)
    {
        return hintsFor(
            project, properties, dependencyVersions, Map.of(COMPILER_PLUGIN_KEY, COMPILER_PLUGIN_VERSION));
    }

    static EffectiveModelHints hintsFor(
        Project project,
        Map<String, String> properties,
        Map<String, String> dependencyVersions,
        Map<String, String> pluginVersions)
    {
        ProvenanceData data = ProvenanceLog.getInstance(project).startBuild("hints");

        data.accept(new PropertyOrigin(MODULE, "revision", REVISION, PARENT));
        data.accept(new PropertyOrigin(MODULE, "stamp", "42", ModelOrigin.UNKNOWN));
        data.accept(new PropertyOrigin(MODULE, "group", LONG_VALUE, PARENT));
        data.accept(new DependencyOrigin(MODULE, LANG3_KEY, LANG3_VERSION, PARENT));
        data.accept(new PluginOrigin(MODULE, SUREFIRE_PLUGIN_KEY, SUREFIRE_PLUGIN_VERSION, PARENT));

        return new EffectiveModelHints(
            EffectiveModel.getInstance(project),
            new FakeModuleFacts(properties, dependencyVersions, pluginVersions));
    }
}
