package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BuildSelectorPlatformTest
    extends BasePlatformTestCase
{
    private static final long STARTED_MILLIS = 1_700_000_000_000L;

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

    private static BuildEntry<String> build(String name)
    {
        return new BuildEntry<>(name, STARTED_MILLIS, name);
    }
}
