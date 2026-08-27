package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BuildSelectorPlatformTest
    extends BasePlatformTestCase
{
    private static final long STARTED_MILLIS = 1_700_000_000_000L;

    private static final String GOALS = "clean install -DskipTests";

    public void testReload_severalBuilds_showsTheSelector()
    {
        BuildSelector<String> selector = new BuildSelector<>(entry -> { });

        selector.reload(List.of(build("first"), build("second")), build("second"));

        assertTrue(selector.getComponent().isVisible());
    }

    public void testReload_aSingleBuild_hidesTheSelectorBecauseThereIsNothingToSwitchTo()
    {
        BuildSelector<String> selector = new BuildSelector<>(entry -> { });

        selector.reload(List.of(build("only")), build("only"));

        assertFalse(selector.getComponent().isVisible());
    }

    public void testSetApplicable_falseWithSeveralBuilds_hidesTheSelector()
    {
        BuildSelector<String> selector = new BuildSelector<>(entry -> { });

        selector.reload(List.of(build("first"), build("second")), build("second"));

        selector.setApplicable(false);

        assertFalse(selector.getComponent().isVisible());
    }

    public void testSetApplicable_trueAgainWithSeveralBuilds_showsTheSelectorOnceMore()
    {
        BuildSelector<String> selector = new BuildSelector<>(entry -> { });

        selector.reload(List.of(build("first"), build("second")), build("second"));
        selector.setApplicable(false);

        selector.setApplicable(true);

        assertTrue(selector.getComponent().isVisible());
    }

    public void testSetSelectedItem_pickedFromTheDropDown_reportsThatBuild()
    {
        List<BuildEntry<String>> reported = new ArrayList<>();

        BuildSelector<String> selector = new BuildSelector<>(reported::add);

        selector.reload(List.of(build("first"), build("second")), build("second"));

        comboOf(selector).setSelectedItem(build("first"));

        assertEquals(List.of(build("first")), reported);
    }

    private static JComboBox<?> comboOf(BuildSelector<String> selector)
    {
        return (JComboBox<?>) selector.getComponent();
    }

    public void testReload_aBuildThatRecordedItsGoals_reachesThemFromTheSelector()
    {
        BuildSelector<String> selector = new BuildSelector<>(entry -> { });

        selector.reload(List.of(ran("first", GOALS)), ran("first", GOALS));

        assertEquals(
            "the goal line is as long as the user made it, so the selector carries it rather than "
                + "laying it out",
            MavenPrimeBundle.message("mavenprime.build.goals", GOALS),
            selector.getComponent().getToolTipText());
    }

    public void testReload_aBuildWhoseGoalsWereNeverRecorded_carriesNoTooltipAtAll()
    {
        BuildSelector<String> selector = new BuildSelector<>(entry -> { });

        selector.reload(List.of(build("adopted")), build("adopted"));

        assertNull(
            "an empty tooltip box under the cursor says the goals are unknown less clearly than none",
            selector.getComponent().getToolTipText());
    }

    private static BuildEntry<String> build(String name)
    {
        return ran(name, "");
    }

    private static BuildEntry<String> ran(String name, String goals)
    {
        return new BuildEntry<>(name, goals, STARTED_MILLIS, name);
    }
}
