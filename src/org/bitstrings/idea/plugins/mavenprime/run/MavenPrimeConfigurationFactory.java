package org.bitstrings.idea.plugins.mavenprime.run;

import org.apache.commons.lang3.StringUtils;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

public final class MavenPrimeConfigurationFactory
    extends ConfigurationFactory
{
    private static final String FACTORY_ID = "MavenPrime";

    public MavenPrimeConfigurationFactory(ConfigurationType type)
    {
        super(type);
    }

    @Override
    public String getId()
    {
        return FACTORY_ID;
    }

    @Override
    public RunConfiguration createTemplateConfiguration(Project project)
    {
        return new MavenPrimeRunConfiguration(project, this, StringUtils.EMPTY);
    }
}
