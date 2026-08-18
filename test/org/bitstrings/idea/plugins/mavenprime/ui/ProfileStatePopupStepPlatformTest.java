package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ProfileStatePopupStepPlatformTest
    extends BasePlatformTestCase
{
    public void testGetValues_theDropdown_offersEveryStateAProfileCanBeIn()
    {
        assertEquals(
            "a state missing from the list is a state the user can no longer get back to",
            List.of(ProfileState.values()),
            step(ProfileState.NOT_SET).getValues());
    }

    public void testGetIconFor_anyState_offersNoIconSoTheLabelsCannotShift()
    {
        assertNull(
            "an icon in the chooser competes with the checkbox on the row and reindents on hover",
            step(ProfileState.NOT_SET).getIconFor(ProfileState.ENABLED));
    }

    public void testGetTextFor_aStateInTheDropdown_readsAsItsLabelRatherThanItsEnumName()
    {
        assertEquals(
            "the enum name would put DISABLED in the list where every other surface says Disabled",
            MavenPrimeBundle.message("mavenprime.profileState.disabled"),
            step(ProfileState.NOT_SET).getTextFor(ProfileState.DISABLED));
    }

    public void testGetDefaultOptionIndex_theStateTheProfileIsAlreadyIn_opensOnThatEntry()
    {
        ProfileStatePopupStep step = step(ProfileState.DISABLED);

        assertSame(
            "a dropdown that opens elsewhere invites a change the user never meant to make",
            ProfileState.DISABLED,
            step.getValues().get(step.getDefaultOptionIndex()));
    }

    public void testOnChosen_aStatePickedFromTheDropdown_handsItToTheCaller()
    {
        AtomicReference<ProfileState> picked = new AtomicReference<>();

        new ProfileStatePopupStep(ProfileState.NOT_SET, picked::set).onChosen(ProfileState.DISABLED, true);

        assertEquals(
            "a pick the step swallows leaves the profile on whatever it was before",
            ProfileState.DISABLED,
            picked.get());
    }

    public void testOnChosen_aStatePickedFromTheDropdown_closesTheDropdown()
    {
        assertSame(
            "asking for another level leaves the popup open on a sub-list that has nothing in it",
            PopupStep.FINAL_CHOICE,
            step(ProfileState.NOT_SET).onChosen(ProfileState.DISABLED, true));
    }

    private static ProfileStatePopupStep step(ProfileState current)
    {
        return new ProfileStatePopupStep(current, state -> { });
    }
}
