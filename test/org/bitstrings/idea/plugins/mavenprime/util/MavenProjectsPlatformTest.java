package org.bitstrings.idea.plugins.mavenprime.util;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenProjectsPlatformTest
    extends BasePlatformTestCase
{
    public void testForFile_nullFile_returnsNoProject()
    {
        assertNull(MavenProjects.forFile(getProject(), null));
    }
}
