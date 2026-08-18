package org.bitstrings.idea.plugins.mavenprime.run;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class TestedBuildDirectoriesPlatformTest
    extends BasePlatformTestCase
{
    private static final String MODULE_DIRECTORY = "/repo/service";

    public void testTestedBuildDirectories_aDirectoryTheIdeHasNoMavenModuleFor_fallsBackToMavensDefault()
    {
        assertEquals(
            "watching nothing leaves the tree empty with no report ever read, whatever the build did",
            List.of(Paths.get(MODULE_DIRECTORY, "target")),
            directoriesFor(MODULE_DIRECTORY));
    }

    private List<Path> directoriesFor(String workingDirectory)
    {
        return MavenPrimeRunProfileState.testedBuildDirectories(
            getProject(), MavenPrimeRequest.in(workingDirectory, List.of("test")));
    }
}
