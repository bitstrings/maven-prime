package org.bitstrings.idea.plugins.mavenprime.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitstrings.idea.plugins.mavenprime.distribution.DistributionSpec;
import org.junit.Test;

public class ContextLayersTest
{
    @Test
    public void properties_aTeamPropertyNobodyOverrode_isListedWithTheTeamValue()
    {
        List<BuildContextProperty> resolved =
            ContextLayers.properties(team("revision", "1.0.0"), List.of(), List.of());

        assertEquals(List.of(BuildContextProperty.of("revision", "1.0.0", false)), resolved);
    }

    @Test
    public void properties_aTeamPropertyOverriddenLocally_isListedWithTheLocalValue()
    {
        List<BuildContextProperty> resolved =
            ContextLayers.properties(
                team("revision", "1.0.0"),
                List.of(PropertyOverride.of("revision", "9.9.9", null)),
                List.of());

        assertEquals("the developer's value wins over the team's", "9.9.9", resolved.get(0).value);
    }

    @Test
    public void properties_aTeamPropertyIgnoredLocally_isNotListed()
    {
        List<BuildContextProperty> resolved =
            ContextLayers.properties(team("revision", "1.0.0"), List.of(), List.of("revision"));

        assertTrue(resolved.isEmpty());
    }

    @Test
    public void properties_aLocalOnlyProperty_isListedAfterTheTeamProperties()
    {
        List<BuildContextProperty> resolved =
            ContextLayers.properties(
                team("revision", "1.0.0"),
                List.of(PropertyOverride.of("mine", "yes", null)),
                List.of());

        assertEquals(List.of("revision", "mine"), keysOf(resolved));
    }

    @Test
    public void narrowProperties_aValueEqualToTheTeamValue_isNotStoredLocally()
    {
        assertTrue(
            "storing an unchanged team value would freeze it against the team's next edit",
            ContextLayers
                .narrowProperties(
                    team("revision", "1.0.0"), List.of(BuildContextProperty.of("revision", "1.0.0", false)))
                .isEmpty());
    }

    @Test
    public void narrowProperties_aValueDifferentFromTheTeamValue_isStoredLocally()
    {
        assertEquals(
            List.of(PropertyOverride.of("revision", "9.9.9", null)),
            ContextLayers.narrowProperties(
                team("revision", "1.0.0"), List.of(BuildContextProperty.of("revision", "9.9.9", false))));
    }

    @Test
    public void narrowProperties_onlyTheImportFlagTurnedOn_leavesTheTeamValueInherited()
    {
        assertEquals(
            "pinning the value would freeze it for a developer who only changed the import flag",
            List.of(PropertyOverride.of("revision", null, Boolean.TRUE)),
            ContextLayers.narrowProperties(
                team("revision", "1.0.0"), List.of(BuildContextProperty.of("revision", "1.0.0", true))));
    }

    @Test
    public void properties_theImportFlagOverriddenAndTheTeamValueChanged_takesTheNewTeamValue()
    {
        assertEquals(
            List.of(BuildContextProperty.of("revision", "2.0.0", true)),
            ContextLayers.properties(
                team("revision", "2.0.0"),
                List.of(PropertyOverride.of("revision", null, Boolean.TRUE)),
                List.of()));
    }

    @Test
    public void properties_narrowedFromASnapshotOmittingATeamProperty_stillResolveTheTeamValue()
    {
        Map<String, String> shared = team("revision", "1.0.0");

        List<PropertyOverride> overrides = ContextLayers.narrowProperties(shared, List.of());

        assertEquals(
            "a stale panel snapshot must never be read as a deletion",
            List.of(BuildContextProperty.of("revision", "1.0.0", false)),
            ContextLayers.properties(shared, overrides, List.of()));
    }

    @Test
    public void properties_narrowedAndResolvedAgain_yieldTheListTheDeveloperSaw()
    {
        Map<String, String> shared = team("revision", "1.0.0");

        List<BuildContextProperty> edited =
            List.of(
                BuildContextProperty.of("revision", "9.9.9", true),
                BuildContextProperty.of("mine", "yes", false));

        assertEquals(
            edited,
            ContextLayers.properties(shared, ContextLayers.narrowProperties(shared, edited), List.of()));
    }

    @Test
    public void profiles_aTeamProfileNobodyTouched_takesTheTeamValue()
    {
        assertEquals(
            Boolean.TRUE, ContextLayers.profiles(profileMap("release", Boolean.TRUE), List.of()).get("release"));
    }

    @Test
    public void profiles_aTeamProfileOverriddenLocally_takesTheLocalValue()
    {
        assertEquals(
            Boolean.FALSE,
            ContextLayers
                .profiles(
                    profileMap("release", Boolean.TRUE), List.of(ProfileOverride.of("release", Boolean.FALSE)))
                .get("release"));
    }

    @Test
    public void profiles_aLocalUnsetOfATeamProfile_removesItFromTheEffectiveSet()
    {
        assertFalse(
            "an unset override is how a developer opts out of a team profile",
            ContextLayers
                .profiles(profileMap("release", Boolean.TRUE), List.of(ProfileOverride.of("release", null)))
                .containsKey("release"));
    }

    @Test
    public void narrowProfiles_anOverrideEqualToTheTeamValue_isDropped()
    {
        assertTrue(
            ContextLayers
                .narrowProfiles(
                    profileMap("release", Boolean.TRUE), List.of(ProfileOverride.of("release", Boolean.TRUE)))
                .isEmpty());
    }

    @Test
    public void narrowProfiles_anOverrideDifferingFromTheTeamValue_isKept()
    {
        assertEquals(
            List.of(ProfileOverride.of("release", Boolean.FALSE)),
            ContextLayers.narrowProfiles(
                profileMap("release", Boolean.TRUE), List.of(ProfileOverride.of("release", Boolean.FALSE))));
    }

    @Test
    public void absorb_aProfileToggledOutsideMavenPrime_becomesALocalOverride()
    {
        assertEquals(
            Boolean.FALSE,
            ContextLayers
                .asMap(
                    ContextLayers.absorb(
                        List.of(),
                        profileMap("release", Boolean.TRUE),
                        profileMap("release", Boolean.FALSE),
                        Set.of("release")))
                .get("release"));
    }

    @Test
    public void absorb_aProfileNobodyChangedSinceWePushedIt_staysInherited()
    {
        assertTrue(
            "absorbing an untouched profile would freeze it against the team's next edit",
            ContextLayers
                .absorb(
                    List.of(),
                    profileMap("release", Boolean.TRUE),
                    profileMap("release", Boolean.TRUE),
                    Set.of("release"))
                .isEmpty());
    }

    @Test
    public void absorb_aProfileUnsetWhileStillDeclaredInTheReactor_becomesALocalUnset()
    {
        Map<String, Boolean> absorbed =
            ContextLayers.asMap(
                ContextLayers.absorb(
                    List.of(), profileMap("release", Boolean.TRUE), profileMap(), Set.of("release")));

        assertTrue(absorbed.containsKey("release"));
        assertNull(absorbed.get("release"));
    }

    @Test
    public void absorb_aProfileGoneFromTheExplicitSetAndFromTheReactor_isLeftAlone()
    {
        assertTrue(
            "before Maven reports its profiles, absence says nothing about what the developer wants",
            ContextLayers
                .absorb(List.of(), profileMap("release", Boolean.TRUE), profileMap(), Set.of())
                .isEmpty());
    }

    @Test
    public void absorb_aProfileGoneFromTheReactor_keepsAnExistingLocalOverride()
    {
        assertEquals(
            "a branch without the profile must not erase the opinion the developer recorded",
            Boolean.FALSE,
            ContextLayers
                .asMap(
                    ContextLayers.absorb(
                        List.of(ProfileOverride.of("release", Boolean.FALSE)),
                        profileMap("release", Boolean.TRUE),
                        profileMap(),
                        Set.of()))
                .get("release"));
    }

    @Test
    public void environment_aFieldTheDeveloperNeverSet_takesTheTeamValue()
    {
        BuildContextEnvironment shared = new BuildContextEnvironment();

        shared.vmOptions = "-Xmx2g";

        assertEquals("-Xmx2g", ContextLayers.environment(shared, new EnvironmentOverride()).vmOptions);
    }

    @Test
    public void environment_aFieldTheDeveloperSet_keepsTheLocalValue()
    {
        BuildContextEnvironment shared = new BuildContextEnvironment();
        EnvironmentOverride local = new EnvironmentOverride();

        shared.jreName = "corretto-21";
        local.jreName = "temurin-21";

        assertEquals(
            "a machine-local JDK name has to survive a team edit",
            "temurin-21",
            ContextLayers.environment(shared, local).jreName);
    }

    @Test
    public void environment_aTeamFieldTheDeveloperCleared_staysCleared()
    {
        BuildContextEnvironment shared = new BuildContextEnvironment();
        EnvironmentOverride local = new EnvironmentOverride();

        shared.settingsFile = "/team/settings.xml";
        local.settingsFile = "";

        assertEquals(
            "an explicitly emptied field is a decision, not the absence of one",
            "",
            ContextLayers.environment(shared, local).settingsFile);
    }

    @Test
    public void narrowEnvironment_aTeamFieldTheDeveloperCleared_isStoredAsAnEmptyOpinion()
    {
        BuildContextEnvironment shared = new BuildContextEnvironment();
        BuildContextEnvironment edited = new BuildContextEnvironment();

        shared.settingsFile = "/team/settings.xml";
        edited.settingsFile = "";

        assertEquals("", ContextLayers.narrowEnvironment(shared, edited).settingsFile);
    }

    @Test
    public void environment_aDistributionTheDeveloperNeverSet_takesTheTeamDistribution()
    {
        BuildContextEnvironment shared = new BuildContextEnvironment();

        shared.distribution = DistributionSpec.daemonHome("/opt/mvnd");

        assertEquals(
            DistributionSpec.daemonHome("/opt/mvnd"),
            ContextLayers.environment(shared, new EnvironmentOverride()).getDistribution());
    }

    @Test
    public void narrowEnvironment_aValueEqualToTheTeamValue_isNotStoredLocally()
    {
        BuildContextEnvironment shared = new BuildContextEnvironment();
        BuildContextEnvironment effective = new BuildContextEnvironment();

        shared.vmOptions = "-Xmx2g";
        effective.vmOptions = "-Xmx2g";

        assertNull(
            "storing an unchanged team value would freeze it against the team's next edit",
            ContextLayers.narrowEnvironment(shared, effective).vmOptions);
    }

    @Test
    public void narrowEnvironment_aDistributionEqualToTheTeamDistribution_isStoredAsInherit()
    {
        BuildContextEnvironment shared = new BuildContextEnvironment();
        BuildContextEnvironment effective = new BuildContextEnvironment();

        shared.distribution = DistributionSpec.daemonHome("/opt/mvnd");
        effective.distribution = DistributionSpec.daemonHome("/opt/mvnd");

        assertNull(ContextLayers.narrowEnvironment(shared, effective).distribution);
    }

    @Test
    public void narrowEnvironment_aDistributionDifferingFromTheTeamDistribution_isStoredLocally()
    {
        BuildContextEnvironment shared = new BuildContextEnvironment();
        BuildContextEnvironment effective = new BuildContextEnvironment();

        shared.distribution = DistributionSpec.daemonHome("/opt/mvnd");
        effective.distribution = DistributionSpec.ide();

        assertEquals(DistributionSpec.ide(), ContextLayers.narrowEnvironment(shared, effective).distribution);
    }

    @Test
    public void forceClean_noLocalOpinion_takesTheTeamValue()
    {
        assertTrue(ContextLayers.forceClean(Boolean.TRUE, null));
    }

    @Test
    public void forceClean_aLocalOpinion_winsOverTheTeamValue()
    {
        assertFalse(ContextLayers.forceClean(Boolean.TRUE, Boolean.FALSE));
    }

    @Test
    public void narrowForceClean_aValueEqualToTheTeamValue_isNotStoredLocally()
    {
        assertNull(ContextLayers.narrowForceClean(Boolean.TRUE, true));
    }

    @Test
    public void narrowForceClean_aValueDifferingFromTheTeamValue_isStoredLocally()
    {
        assertEquals(Boolean.FALSE, ContextLayers.narrowForceClean(Boolean.TRUE, false));
    }

    @Test
    public void valueOf_aKeyTheTableCarriesTwice_readsTheOccurrenceMavenWouldKeep()
    {
        assertEquals(
            "-Drevision is applied left to right, so the last row is the one that takes effect",
            "2.0.0",
            ContextLayers.valueOf(
                List.of(
                    BuildContextProperty.of("revision", "1.0.0", true),
                    BuildContextProperty.of("revision", "2.0.0", true)),
                "revision"));
    }

    @Test
    public void valueOf_aKeyNoRowDeclares_readsAsAbsentRatherThanEmpty()
    {
        assertNull(
            ContextLayers.valueOf(List.of(BuildContextProperty.of("revision", "1.0.0", true)), "other"));
    }

    @Test
    public void originsOf_aSharedKeyNobodyOverrode_isReportedAsShared()
    {
        assertEquals(
            Map.of("revision", ContextOrigin.SHARED),
            ContextLayers.originsOf(Set.of("revision"), Set.of()));
    }

    @Test
    public void originsOf_aSharedKeyCarryingALocalOverride_isReportedAsOverridden()
    {
        assertEquals(
            Map.of("revision", ContextOrigin.OVERRIDDEN),
            ContextLayers.originsOf(Set.of("revision"), Set.of("revision")));
    }

    @Test
    public void originsOf_anOverrideOnAKeyTheTeamNeverDeclared_reportsNoOrigin()
    {
        assertTrue(
            "a key outside the shared layer has no team origin to report",
            ContextLayers.originsOf(Set.of(), Set.of("mine")).isEmpty());
    }

    private static Map<String, String> team(String key, String value)
    {
        Map<String, String> properties = new LinkedHashMap<>();

        properties.put(key, value);

        return properties;
    }

    private static Map<String, Boolean> profileMap(String name, Boolean enabled)
    {
        Map<String, Boolean> profiles = new LinkedHashMap<>();

        profiles.put(name, enabled);

        return profiles;
    }

    private static Map<String, Boolean> profileMap()
    {
        return new LinkedHashMap<>();
    }

    private static List<String> keysOf(List<BuildContextProperty> properties)
    {
        return properties.stream().map(property -> property.key).toList();
    }
}
