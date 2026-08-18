package org.bitstrings.idea.plugins.mavenprime.repository;

import org.apache.commons.lang3.StringUtils;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class RepositoryCredentialStore
{
    private static final String SUBSYSTEM = "Maven Prime Repository";

    private final Project project;

    public RepositoryCredentialStore(Project project)
    {
        this.project = project;
    }

    public static RepositoryCredentialStore getInstance(Project project)
    {
        return project.getService(RepositoryCredentialStore.class);
    }

    public RepositoryCredentials forServer(String serverId)
    {
        if (StringUtils.isBlank(serverId))
        {
            return RepositoryCredentials.none();
        }

        RepositoryCredentials declared = MavenServers.of(project).get(serverId);

        if ((declared != null) && declared.isUsable())
        {
            return declared;
        }

        Credentials stored = PasswordSafe.getInstance().get(attributesFor(serverId));

        if ((stored != null)
            && StringUtils.isNotBlank(stored.getUserName())
            && StringUtils.isNotBlank(stored.getPasswordAsString()))
        {
            return new RepositoryCredentials(
                stored.getUserName(),
                stored.getPasswordAsString(),
                RepositoryCredentials.Source.PASSWORD_SAFE);
        }

        return (declared == null) ? RepositoryCredentials.none() : declared;
    }

    public void store(String serverId, String username, String password)
    {
        PasswordSafe.getInstance().set(attributesFor(serverId), new Credentials(username, password));
    }

    private static CredentialAttributes attributesFor(String serverId)
    {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(SUBSYSTEM, serverId));
    }
}
