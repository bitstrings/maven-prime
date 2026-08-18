package org.bitstrings.idea.plugins.mavenprime.ui;

public final class ProfileEntry
{
    private final String name;

    private ProfileState state;

    public ProfileEntry(String name, ProfileState state)
    {
        this.name = name;
        this.state = state;
    }

    public String getName()
    {
        return name;
    }

    public ProfileState getState()
    {
        return state;
    }

    public void setState(ProfileState state)
    {
        this.state = state;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
