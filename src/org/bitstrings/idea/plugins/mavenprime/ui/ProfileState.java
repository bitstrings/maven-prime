package org.bitstrings.idea.plugins.mavenprime.ui;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;

import com.intellij.util.ui.ThreeStateCheckBox;

public enum ProfileState
{
    NOT_SET("notSet"),
    ENABLED("enabled"),
    DISABLED("disabled");

    private final String bundleKey;

    ProfileState(String bundleKey)
    {
        this.bundleKey = bundleKey;
    }

    public static ProfileState of(Boolean enabled)
    {
        if (enabled == null)
        {
            return NOT_SET;
        }

        return enabled.booleanValue() ? ENABLED : DISABLED;
    }

    public Boolean toEnabled()
    {
        switch (this)
        {
            case ENABLED:
                return Boolean.TRUE;

            case DISABLED:
                return Boolean.FALSE;

            default:
                return null;
        }
    }

    public ThreeStateCheckBox.State getCheckboxState()
    {
        return switch (this)
        {
            case NOT_SET -> ThreeStateCheckBox.State.DONT_CARE;
            case ENABLED -> ThreeStateCheckBox.State.SELECTED;
            case DISABLED -> ThreeStateCheckBox.State.NOT_SELECTED;
        };
    }

    public String getTitle()
    {
        return MavenPrimeBundle.message("mavenprime.profileState." + bundleKey);
    }

    @Override
    public String toString()
    {
        return getTitle();
    }
}
