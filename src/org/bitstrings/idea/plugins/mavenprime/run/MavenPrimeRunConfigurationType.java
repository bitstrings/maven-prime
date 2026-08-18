package org.bitstrings.idea.plugins.mavenprime.run;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeIcons;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;

public final class MavenPrimeRunConfigurationType
    extends ConfigurationTypeBase
{
    public static final String ID = "MavenPrimeRunConfiguration";

    public MavenPrimeRunConfigurationType()
    {
        super(
            ID,
            MavenPrimeBundle.message("mavenprime.runConfiguration.name"),
            MavenPrimeBundle.message("mavenprime.runConfiguration.description"),
            MavenPrimeIcons.DAEMON);

        addFactory(new MavenPrimeConfigurationFactory(this));
    }

    public static MavenPrimeRunConfigurationType getInstance()
    {
        return ConfigurationTypeUtil.findConfigurationType(MavenPrimeRunConfigurationType.class);
    }

    public ConfigurationFactory getFactory()
    {
        return getConfigurationFactories()[0];
    }
}
