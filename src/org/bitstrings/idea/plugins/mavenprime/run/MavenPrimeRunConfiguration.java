package org.bitstrings.idea.plugins.mavenprime.run;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.jdom.Element;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializer;

public final class MavenPrimeRunConfiguration
    extends LocatableConfigurationBase<Element>
{
    private static final long serialVersionUID = 1L;

    private static final String REQUEST_ELEMENT = "MavenPrimeRequest";

    private transient MavenPrimeRequest request = new MavenPrimeRequest();

    MavenPrimeRunConfiguration(Project project, ConfigurationFactory factory, String name)
    {
        super(project, factory, name);
    }

    public MavenPrimeRequest getRequest()
    {
        return request;
    }

    public void setRequest(MavenPrimeRequest request)
    {
        this.request = request.copy();
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
    {
        return new MavenPrimeSettingsEditor(getProject());
    }

    @Override
    public RunProfileState getState(Executor executor, ExecutionEnvironment environment)
    {
        return new MavenPrimeRunProfileState(environment, this);
    }

    @Override
    public void checkConfiguration()
        throws RuntimeConfigurationException
    {
        if (!request.hasGoals())
        {
            throw new RuntimeConfigurationError(MavenPrimeBundle.message("mavenprime.runConfiguration.error.noGoals"));
        }

        if (!isExistingDirectory(request.workingDirectory))
        {
            throw new RuntimeConfigurationError(
                MavenPrimeBundle.message(
                    "mavenprime.runConfiguration.error.workingDirectory",
                    StringUtils.defaultString(request.workingDirectory)));
        }
    }

    private static boolean isExistingDirectory(String path)
    {
        if (StringUtils.isBlank(path))
        {
            return false;
        }

        try
        {
            return Files.isDirectory(Paths.get(path));
        }
        catch (InvalidPathException malformed)
        {
            return false;
        }
    }

    @Override
    public String suggestedName()
    {
        return request.getGoalLine();
    }

    @Override
    public void readExternal(Element element)
    {
        super.readExternal(element);

        Element requestElement = element.getChild(REQUEST_ELEMENT);

        if (requestElement != null)
        {
            request = new MavenPrimeRequest();

            XmlSerializer.deserializeInto(request, requestElement);
        }
    }

    @Override
    public void writeExternal(Element element)
    {
        super.writeExternal(element);

        Element requestElement = new Element(REQUEST_ELEMENT);

        XmlSerializer.serializeInto(request, requestElement);

        element.addContent(requestElement);
    }

    @Override
    public MavenPrimeRunConfiguration clone()
    {
        MavenPrimeRunConfiguration copy = (MavenPrimeRunConfiguration) super.clone();

        copy.request = request.copy();

        return copy;
    }
}
