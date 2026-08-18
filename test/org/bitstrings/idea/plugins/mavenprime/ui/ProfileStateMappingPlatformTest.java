package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.Arrays;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.ThreeStateCheckBox;

public class ProfileStateMappingPlatformTest
    extends BasePlatformTestCase
{
    public void testGetCheckboxState_aProfileForcedOn_readsAsATick()
    {
        assertSame(
            "the state Maven will actually apply has to be the one the checkbox shows",
            ThreeStateCheckBox.State.SELECTED,
            ProfileState.ENABLED.getCheckboxState());
    }

    public void testGetCheckboxState_aProfileForcedOff_readsAsAnEmptyBoxRatherThanADash()
    {
        assertSame(
            "a dash is the platform's indeterminate mark, so it would read as a default rather than off",
            ThreeStateCheckBox.State.NOT_SELECTED,
            ProfileState.DISABLED.getCheckboxState());
    }

    public void testGetCheckboxState_aProfileNobodyHasTouched_readsAsTheIndeterminateDash()
    {
        assertSame(
            "an empty box on an untouched profile claims a decision the user never made",
            ThreeStateCheckBox.State.DONT_CARE,
            ProfileState.NOT_SET.getCheckboxState());
    }

    public void testGetCheckboxState_theThreeStates_eachGetTheirOwnGlyph()
    {
        assertEquals(
            "two states sharing a glyph make every surface lie about what the build will do",
            ProfileState.values().length,
            Arrays
                .stream(ProfileState.values())
                .map(ProfileState::getCheckboxState)
                .distinct()
                .count());
    }
}
