package org.bitstrings.idea.plugins.mavenprime.editor;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class PomRunLineMarkerPlatformTest
    extends BasePlatformTestCase
{
    public void testRunInfo_twoHighlightingPasses_produceEqualMarkers()
    {
        assertEquals(PomRunLineMarkerContributor.runInfo(), PomRunLineMarkerContributor.runInfo());
    }
}
