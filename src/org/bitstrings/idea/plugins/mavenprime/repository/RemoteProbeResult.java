package org.bitstrings.idea.plugins.mavenprime.repository;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

public record RemoteProbeResult(
    String repositoryId,
    String url,
    RemoteAvailability availability,
    int statusCode,
    String error,
    RepositoryCredentials.Source credentialSource)
{
    public String getMessage()
    {
        return MavenPrimeBundle.message(
            messageKey(), repositoryId, url, StringUtils.defaultString(error));
    }

    private String messageKey()
    {
        return (availability == RemoteAvailability.UNAUTHORIZED)
            ? ("mavenprime.repository.remote.unauthorized." + StringUtils.lowerCase(credentialSource.name()))
            : ("mavenprime.repository.remote." + StringUtils.lowerCase(availability.name()));
    }
}
