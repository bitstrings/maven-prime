package org.bitstrings.idea.plugins.mavenprime.config;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion;

public final class MavenPrimeSchemaProviderFactory
    implements JsonSchemaProviderFactory
{
    private static final String SCHEMA_RESOURCE = "/schemas/mavenprime.schema.json";

    @Override
    public List<JsonSchemaFileProvider> getProviders(Project project)
    {
        return List.of(new MavenPrimeSchemaProvider(project));
    }

    private static final class MavenPrimeSchemaProvider
        implements JsonSchemaFileProvider
    {
        private final Project project;

        MavenPrimeSchemaProvider(Project project)
        {
            this.project = project;
        }

        @Override
        public boolean isAvailable(VirtualFile file)
        {
            return MavenPrimeConfigService.FILE_NAME.equals(file.getName())
                && file.equals(MavenPrimeConfigService.getInstance(project).findConfigFile());
        }

        @Override
        public String getName()
        {
            return MavenPrimeBundle.message("mavenprime.config.schemaName");
        }

        @Override
        public VirtualFile getSchemaFile()
        {
            return JsonSchemaProviderFactory.getResourceFile(
                MavenPrimeSchemaProviderFactory.class, SCHEMA_RESOURCE);
        }

        @Override
        public SchemaType getSchemaType()
        {
            return SchemaType.embeddedSchema;
        }

        @Override
        public JsonSchemaVersion getSchemaVersion()
        {
            return JsonSchemaVersion.SCHEMA_7;
        }
    }
}
