package org.bitstrings.idea.plugins.mavenprime.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.jdom.Element;
import org.junit.Test;

import com.intellij.util.xmlb.XmlSerializer;

public class BuildContextPropertiesSerializationTest
{
    @Test
    public void serialize_environmentWithADaemonHome_survivesARoundTrip()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.environment.distribution = DistributionSpec.daemonHome("/opt/mvnd");
        state.environment.settingsFile = "/team/settings.xml";
        state.environment.jreName = "#JAVA_HOME";
        state.environment.vmOptions = "-Xmx2g";

        assertEquals(state.environment, roundTrip(state).environment);
    }

    @Test
    public void serialize_aTeamFieldClearedByTheDeveloper_staysAnEmptyOpinion()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.environment.settingsFile = "";

        assertEquals(
            "an empty opinion read back as null would let the team's settings file return",
            "",
            roundTrip(state).environment.settingsFile);
    }

    @Test
    public void serialize_propertyExcludedFromTheImport_survivesARoundTrip()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.properties.add(PropertyOverride.of("skipTests", "true", Boolean.FALSE));

        assertEquals(state.properties, roundTrip(state).properties);
    }

    @Test
    public void serialize_anImportFlagOverrideWithNoValueOpinion_keepsTheValueInherited()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.properties.add(PropertyOverride.of("revision", null, Boolean.FALSE));

        assertNull(
            "a value read back as empty would pin the team's value for this developer",
            roundTrip(state).properties.get(0).value);
    }

    @Test
    public void serialize_aProfileOverride_survivesARoundTrip()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.profiles.add(ProfileOverride.of("release", Boolean.FALSE));

        assertEquals(state.profiles, roundTrip(state).profiles);
    }

    @Test
    public void serialize_anOverrideThatUnsetsATeamProfile_keepsItsUnsetState()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.profiles.add(ProfileOverride.of("release", null));

        assertNull(
            "an unset override read back as enabled would silently re-enable a team profile",
            roundTrip(state).profiles.get(0).enabled);
    }

    @Test
    public void serialize_aSuppressedTeamProperty_survivesARoundTrip()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.ignoredProperties.add("revision");

        assertEquals(List.of("revision"), roundTrip(state).ignoredProperties);
    }

    @Test
    public void serialize_theProfilesWeLastPushed_surviveARoundTrip()
    {
        BuildContextProperties state = new BuildContextProperties();

        state.pushedProfiles.add(ProfileOverride.of("release", Boolean.TRUE));

        assertEquals(state.pushedProfiles, roundTrip(state).pushedProfiles);
    }

    @Test
    public void serialize_forceCleanWithNoLocalOpinion_staysUnset()
    {
        assertNull(
            "a false read back for an absent opinion would stop the team value from applying",
            roundTrip(new BuildContextProperties()).forceClean);
    }

    @Test
    public void serialize_aDistributionLeftToTheTeam_staysInherited()
    {
        assertTrue(
            "an inherited environment read back as a concrete one would pin the IDE Maven",
            roundTrip(new BuildContextProperties()).environment.isEmpty());
    }

    private static BuildContextProperties roundTrip(BuildContextProperties state)
    {
        Element element = XmlSerializer.serialize(state);

        return XmlSerializer.deserialize(element, BuildContextProperties.class);
    }
}
