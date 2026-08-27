package org.bitstrings.idea.plugins.mavenprime.repository;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.repository.ResolutionEvent.ArtifactDownloading;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ResolutionHistoryPlatformTest
    extends BasePlatformTestCase
{
    private static final String COORDINATES = "commons-io:commons-io:jar:2.16.1";

    public void testGetDownloads_recordedByTheNewestBuild_areWhatThePanelReads()
    {
        ResolutionLog log = ResolutionLog.getInstance(getProject());

        log.startBuild(buildNamed("only"), "").accept(download());

        assertEquals(1, log.getDownloads().size());
    }

    public void testStartBuild_aNamedBuild_labelsItsHistoryEntryWithThatName()
    {
        ResolutionLog log = ResolutionLog.getInstance(getProject());

        log.startBuild(buildNamed("only"), "");

        assertEquals(buildNamed("only"), log.getSelectedBuild().name());
    }

    public void testGetBuilds_severalBuildsRecorded_offersEveryOneOfThem()
    {
        ResolutionLog log = ResolutionLog.getInstance(getProject());

        log.startBuild(buildNamed("first"), "");
        log.startBuild(buildNamed("second"), "");

        assertTrue(
            "every recorded build has to be reachable from the selector",
            namesOf(log.getBuilds()).containsAll(List.of(buildNamed("first"), buildNamed("second"))));
    }

    public void testGetDownloads_recordedByAnEarlierBuild_comeBackWhenThatBuildIsSelected()
    {
        ResolutionLog log = ResolutionLog.getInstance(getProject());

        log.startBuild(buildNamed("earlier"), "").accept(download());

        BuildEntry<ResolutionData> earlier = log.getSelectedBuild();

        log.startBuild(buildNamed("newer"), "");

        log.select(earlier);

        assertEquals(1, log.getDownloads().size());
    }

    public void testGetDownloads_aLaterBuildThatDownloadedNothing_readsItsOwnEmptyData()
    {
        ResolutionLog log = ResolutionLog.getInstance(getProject());

        log.startBuild(buildNamed("earlier"), "").accept(download());
        log.startBuild(buildNamed("newer"), "");

        assertEquals(
            "each build owns its downloads, so the newest one does not inherit them",
            0,
            log.getDownloads().size());
    }

    private static List<String> namesOf(List<BuildEntry<ResolutionData>> builds)
    {
        List<String> names = new ArrayList<>(builds.size());

        for (BuildEntry<ResolutionData> build : builds)
        {
            names.add(build.name());
        }

        return names;
    }

    private String buildNamed(String suffix)
    {
        return getName() + '-' + suffix;
    }

    private static ArtifactDownloading download()
    {
        return new ArtifactDownloading(COORDINATES, "central", "https://repo.maven.apache.org/maven2");
    }
}
