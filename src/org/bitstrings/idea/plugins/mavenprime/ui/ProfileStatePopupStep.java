package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.List;
import java.util.function.Consumer;

import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;

public final class ProfileStatePopupStep
    extends BaseListPopupStep<ProfileState>
{
    private final Consumer<ProfileState> chosen;

    public ProfileStatePopupStep(ProfileState current, Consumer<ProfileState> chosen)
    {
        super(null, List.of(ProfileState.values()));

        this.chosen = chosen;

        setDefaultOptionIndex(getValues().indexOf(current));
    }

    @Override
    public String getTextFor(ProfileState state)
    {
        return state.getTitle();
    }

    @Override
    public PopupStep<?> onChosen(ProfileState state, boolean finalChoice)
    {
        chosen.accept(state);

        return FINAL_CHOICE;
    }
}
