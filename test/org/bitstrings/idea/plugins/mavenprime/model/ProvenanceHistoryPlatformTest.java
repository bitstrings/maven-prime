package org.bitstrings.idea.plugins.mavenprime.model;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.model.ProvenanceEvent.DependencyOrigin;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ProvenanceHistoryPlatformTest
    extends BasePlatformTestCase
{
    public void testGetBuilds_severalBuildsRecorded_offersEveryOneOfThem()
    {
        ProvenanceLog log = ProvenanceLog.getInstance(getProject());

        log.startBuild(buildNamed("first"));
        log.startBuild(buildNamed("second"));

        assertTrue(
            "every recorded build has to be reachable from the selector",
            namesOf(log.getBuilds()).containsAll(List.of(buildNamed("first"), buildNamed("second"))));
    }

    public void testStartBuild_aNamedBuild_labelsItsHistoryEntryWithThatName()
    {
        ProvenanceLog log = ProvenanceLog.getInstance(getProject());

        log.startBuild(buildNamed("only"));

        assertEquals(buildNamed("only"), log.getSelectedBuild().name());
    }

    public void testSelect_anEarlierBuild_readsThatBuildsDependenciesAgain()
    {
        ProvenanceLog log = ProvenanceLog.getInstance(getProject());

        log.startBuild(buildNamed("earlier")).accept(origin("core", "commons-io"));

        BuildEntry<ProvenanceData> earlier = log.getSelectedBuild();

        log.startBuild(buildNamed("newer"));

        log.select(earlier);

        assertEquals(1, log.getDependencies("core").size());
    }

    public void testSelect_theNewestBuild_readsItsOwnDataRatherThanThePreviousBuilds()
    {
        ProvenanceLog log = ProvenanceLog.getInstance(getProject());

        log.startBuild(buildNamed("earlier")).accept(origin("core", "commons-io"));
        log.startBuild(buildNamed("newer"));

        assertEquals(
            "starting a build selects it, so the panel shows that build and not the one before it",
            0,
            log.getDependencies("core").size());
    }

    private String buildNamed(String suffix)
    {
        return getName() + '-' + suffix;
    }

    private static List<String> namesOf(List<BuildEntry<ProvenanceData>> builds)
    {
        List<String> names = new ArrayList<>(builds.size());

        for (BuildEntry<ProvenanceData> build : builds)
        {
            names.add(build.name());
        }

        return names;
    }

    private static DependencyOrigin origin(String module, String name)
    {
        return new DependencyOrigin(module, name, "1.0", ModelOrigin.UNKNOWN);
    }
}
