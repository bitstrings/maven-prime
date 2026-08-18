package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RepositorySourceTest
{
    @Test
    public void isFromResolutionLog_theSourcesRecordedByABuild_areTrue()
    {
        assertTrue(RepositorySource.FAILURES.isFromResolutionLog());
        assertTrue(RepositorySource.DOWNLOADS.isFromResolutionLog());
    }

    @Test
    public void isFromResolutionLog_theSourcesReadFromTheRepositoryOrThePom_areFalse()
    {
        assertFalse(RepositorySource.SEARCH.isFromResolutionLog());
        assertFalse(RepositorySource.INDEX.isFromResolutionLog());
        assertFalse(RepositorySource.MODULE.isFromResolutionLog());
        assertFalse(RepositorySource.PLUGINS.isFromResolutionLog());
    }
}
