package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class RepositoryCredentialsTest
{
    private static final String PASSWORD = "s3cr3t-that-must-never-be-logged";

    private static final RepositoryCredentials USABLE =
        new RepositoryCredentials("deployer", PASSWORD, RepositoryCredentials.Source.SETTINGS_XML);

    @Test
    public void toString_credentialsCarryingAPassword_neverRendersThePassword()
    {
        assertFalse(USABLE.toString().contains(PASSWORD));
    }

    @Test
    public void toString_credentialsCarryingAPassword_stillNamesTheServerAccount()
    {
        assertTrue(USABLE.toString().contains("deployer"));
    }

    @Test
    public void toString_credentialsCarryingAPassword_namesTheSourceThatSuppliedThem()
    {
        assertTrue(USABLE.toString().contains(RepositoryCredentials.Source.SETTINGS_XML.name()));
    }

    @Test
    public void isUsable_blankUsername_isNotUsable()
    {
        assertFalse(
            new RepositoryCredentials(StringUtils.EMPTY, PASSWORD, RepositoryCredentials.Source.SETTINGS_XML)
                .isUsable());
    }

    @Test
    public void isUsable_missingPassword_isNotUsable()
    {
        assertFalse(
            new RepositoryCredentials("deployer", null, RepositoryCredentials.Source.SETTINGS_XML).isUsable());
    }

    @Test
    public void isUsable_usernameAndPasswordPresent_isUsable()
    {
        assertTrue(USABLE.isUsable());
    }
}
