package org.bitstrings.idea.plugins.mavenprime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MavenPathsTest
{
    @Rule
    public final TemporaryFolder home = new TemporaryFolder();

    @Test
    public void toPath_anAbsolutePath_isTakenAsItIs()
    {
        assertEquals(Path.of("/team/repo"), MavenPaths.toPath(userHome(), "/team/repo"));
    }

    @Test
    public void toPath_aRelativePath_isResolvedAgainstTheUserHome()
    {
        assertEquals(userHome().resolve("repo"), MavenPaths.toPath(userHome(), "repo"));
    }

    @Test
    public void toPath_theUserHomePlaceholder_isExpanded()
    {
        assertEquals(
            userHome().resolve("custom-repo"), MavenPaths.toPath(userHome(), "${user.home}/custom-repo"));
    }

    @Test
    public void toPath_aSystemPropertyPlaceholder_isExpanded()
    {
        assertEquals(
            Path.of(System.getProperty("java.io.tmpdir")).resolve("repo").normalize(),
            MavenPaths.toPath(userHome(), "${java.io.tmpdir}/repo"));
    }

    @Test
    public void toPath_anUnknownPlaceholder_expandsToNothingRatherThanLeakingTheExpression()
    {
        assertEquals(userHome().resolve("repo"), MavenPaths.toPath(userHome(), "${no.such.property}repo"));
    }

    @Test
    public void toPath_aBlankValue_reportsNoPath()
    {
        assertNull(MavenPaths.toPath(userHome(), StringUtils.SPACE));
    }

    @Test
    public void toPath_aNullValue_reportsNoPath()
    {
        assertNull(MavenPaths.toPath(userHome(), null));
    }

    @Test
    public void toPath_aPathThatNormalizesAway_isNormalized()
    {
        assertEquals(Path.of("/team/repo"), MavenPaths.toPath(userHome(), "/team/other/../repo"));
    }

    private Path userHome()
    {
        return home.getRoot().toPath();
    }
}
