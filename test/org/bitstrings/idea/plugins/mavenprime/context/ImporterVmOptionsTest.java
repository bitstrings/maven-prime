package org.bitstrings.idea.plugins.mavenprime.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.intellij.util.execution.ParametersListUtil;

public class ImporterVmOptionsTest
{
    private static final String USER_OPTIONS = "-Xmx4g -Dhttps.proxyHost=proxy.corp";

    private static final String NOTHING = "";

    @Test
    public void rendered_aPropertyMarkedForImport_isDefinedOnItsOwn()
    {
        assertEquals("-Denv=ci", ImporterVmOptions.rendered(List.of(BuildContextProperty.of("env", "ci", true))));
    }

    @Test
    public void rendered_aPropertyExcludedFromImport_isNotWritten()
    {
        assertEquals(
            "-Denv=ci",
            ImporterVmOptions.rendered(
                List.of(
                    BuildContextProperty.of("env", "ci", true),
                    BuildContextProperty.of("skipTests", "true", false))));
    }

    @Test
    public void rendered_aValuelessProperty_rendersABareDefinition()
    {
        assertEquals(
            "-Dquickstart",
            ImporterVmOptions.rendered(List.of(BuildContextProperty.of("quickstart", "", true))));
    }

    @Test
    public void rendered_aBlankKey_isSkipped()
    {
        assertEquals(NOTHING, ImporterVmOptions.rendered(List.of(BuildContextProperty.of("  ", "value", true))));
    }

    @Test
    public void rendered_aValueContainingASpace_survivesReparsing()
    {
        String region = ImporterVmOptions.rendered(List.of(BuildContextProperty.of("banner", "hello world", true)));

        assertTrue(region, ParametersListUtil.parse(region).contains("-Dbanner=hello world"));
    }

    @Test
    public void rendered_aValueContainingADoubleQuote_survivesReparsing()
    {
        String region = ImporterVmOptions.rendered(List.of(BuildContextProperty.of("banner", "say \"hi\"", true)));

        assertTrue(region, ParametersListUtil.parse(region).contains("-Dbanner=say \"hi\""));
    }

    @Test
    public void rendered_aKeyDeclaredTwice_definesTheValueTheContextReportsForIt()
    {
        List<BuildContextProperty> properties =
            List.of(
                BuildContextProperty.of("revision", "1.0.0", true),
                BuildContextProperty.of("revision", "2.0.0", true));

        assertEquals(
            "the importer JVM keeps the last -D, so no reader of the same rows may report an earlier one",
            ContextLayers.valueOf(properties, "revision"),
            lastDefinitionOf(ImporterVmOptions.rendered(properties), "revision"));
    }

    @Test
    public void merge_nothingWrittenBeforeAndNothingToWrite_leavesTheFieldByteForByte()
    {
        String untidy = "  -Xmx4g   -Xms1g  ";

        assertEquals(untidy, ImporterVmOptions.merge(untidy, NOTHING, NOTHING));
    }

    @Test
    public void merge_userOptionsPresent_keepsThemAndAppendsTheRegion()
    {
        assertEquals(
            USER_OPTIONS + " -Denv=ci", ImporterVmOptions.merge(USER_OPTIONS, NOTHING, "-Denv=ci"));
    }

    @Test
    public void merge_appliedTwice_leavesASingleRegion()
    {
        String once = ImporterVmOptions.merge(USER_OPTIONS, NOTHING, "-Denv=ci");

        assertEquals(once, ImporterVmOptions.merge(once, "-Denv=ci", "-Denv=ci"));
    }

    @Test
    public void merge_nothingLeftToWrite_removesWhatWeWroteAndKeepsTheRest()
    {
        String existing = ImporterVmOptions.merge(USER_OPTIONS, NOTHING, "-Denv=ci");

        assertEquals(USER_OPTIONS, ImporterVmOptions.merge(existing, "-Denv=ci", NOTHING));
    }

    @Test
    public void merge_theUserEditedWhatWeWrote_removesNothingAtAll()
    {
        String edited = USER_OPTIONS + " -Denv=staging";

        assertEquals(
            "a field that no longer matches what we wrote is the user's, and removing from it is guesswork",
            edited + " -Denv=ci",
            ImporterVmOptions.merge(edited, "-Denv=ci", "-Denv=ci"));
    }

    @Test
    public void merge_theUserAddedOptionsAroundOurs_keepsEveryOneOfThem()
    {
        String existing = "-Xmx4g -Denv=ci -Xms1g";

        assertEquals(
            "-Xmx4g -Xms1g -Denv=qa", ImporterVmOptions.merge(existing, "-Denv=ci", "-Denv=qa"));
    }

    @Test
    public void merge_ourRegionAlreadyGone_addsTheNewOneWithoutTouchingTheRest()
    {
        assertEquals(
            USER_OPTIONS + " -Denv=ci", ImporterVmOptions.merge(USER_OPTIONS, "-Denv=gone", "-Denv=ci"));
    }

    @Test
    public void stripLegacy_aFieldWithoutMarkers_isReturnedUntouched()
    {
        String untidy = "  -Xmx4g   -Xms1g  ";

        assertEquals(untidy, ImporterVmOptions.stripLegacy(untidy));
    }

    @Test
    public void stripLegacy_aFencedRegion_removesTheMarkersAndTheirContent()
    {
        String existing =
            "-Xmx4g " + ImporterVmOptions.LEGACY_BEGIN + " -Denv=ci " + ImporterVmOptions.LEGACY_END + " -Xms1g";

        assertEquals("-Xmx4g -Xms1g", ImporterVmOptions.stripLegacy(existing));
    }

    @Test
    public void stripLegacy_anOpeningMarkerWithNoClose_removesOnlyTheMarker()
    {
        String existing = "-Xmx4g " + ImporterVmOptions.LEGACY_BEGIN + " -Denv=ci -Xms1g";

        assertEquals(
            "everything after a dangling marker was the user's, and cleaning up is no licence to delete it",
            "-Xmx4g -Denv=ci -Xms1g",
            ImporterVmOptions.stripLegacy(existing));
    }

    private static String lastDefinitionOf(String rendered, String key)
    {
        String prefix = "-D" + key + '=';
        String value = null;

        for (String argument : ParametersListUtil.parse(rendered))
        {
            if (argument.startsWith(prefix))
            {
                value = argument.substring(prefix.length());
            }
        }

        return value;
    }
}
