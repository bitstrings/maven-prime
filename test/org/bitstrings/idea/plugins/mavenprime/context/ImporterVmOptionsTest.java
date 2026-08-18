package org.bitstrings.idea.plugins.mavenprime.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.intellij.util.execution.ParametersListUtil;

public class ImporterVmOptionsTest
{
    private static final String USER_OPTIONS = "-Xmx4g -Dhttps.proxyHost=proxy.corp";

    @Test
    public void merge_noExistingOptions_writesOnlyTheManagedRegion()
    {
        String merged = ImporterVmOptions.merge("", List.of(BuildContextProperty.of("env", "ci", true)));

        assertEquals(
            ImporterVmOptions.BEGIN + " -Denv=ci " + ImporterVmOptions.END, merged);
    }

    @Test
    public void merge_userOptionsPresent_keepsThemVerbatim()
    {
        String merged =
            ImporterVmOptions.merge(USER_OPTIONS, List.of(BuildContextProperty.of("env", "ci", true)));

        assertTrue(merged, merged.startsWith(USER_OPTIONS + ' '));
    }

    @Test
    public void merge_appliedTwice_leavesASingleRegion()
    {
        List<BuildContextProperty> properties = List.of(BuildContextProperty.of("env", "ci", true));

        String once = ImporterVmOptions.merge(USER_OPTIONS, properties);

        assertEquals(once, ImporterVmOptions.merge(once, properties));
    }

    @Test
    public void merge_propertyExcludedFromImport_isNotWritten()
    {
        String merged =
            ImporterVmOptions.merge(
                "",
                List.of(
                    BuildContextProperty.of("env", "ci", true),
                    BuildContextProperty.of("skipTests", "true", false)));

        assertEquals(ImporterVmOptions.BEGIN + " -Denv=ci " + ImporterVmOptions.END, merged);
    }

    @Test
    public void merge_valueContainingASpace_survivesReparsing()
    {
        String merged =
            ImporterVmOptions.merge("", List.of(BuildContextProperty.of("banner", "hello world", true)));

        assertTrue(merged, ParametersListUtil.parse(merged).contains("-Dbanner=hello world"));
    }

    @Test
    public void merge_valueContainingADoubleQuote_survivesReparsing()
    {
        String merged =
            ImporterVmOptions.merge("", List.of(BuildContextProperty.of("banner", "say \"hi\"", true)));

        assertTrue(merged, ParametersListUtil.parse(merged).contains("-Dbanner=say \"hi\""));
    }

    @Test
    public void merge_valuelessProperty_rendersABareDefinition()
    {
        String merged = ImporterVmOptions.merge("", List.of(BuildContextProperty.of("quickstart", "", true)));

        assertEquals(ImporterVmOptions.BEGIN + " -Dquickstart " + ImporterVmOptions.END, merged);
    }

    @Test
    public void merge_lastImportPropertyRemoved_dropsTheRegionAndKeepsUserOptions()
    {
        String existing = ImporterVmOptions.merge(USER_OPTIONS, List.of(BuildContextProperty.of("env", "ci", true)));

        assertEquals(USER_OPTIONS, ImporterVmOptions.merge(existing, List.of()));
    }

    @Test
    public void merge_blankKey_isSkipped()
    {
        assertEquals("", ImporterVmOptions.merge("", List.of(BuildContextProperty.of("  ", "value", true))));
    }

    @Test
    public void merge_aKeyDeclaredTwice_definesTheValueTheContextReportsForIt()
    {
        List<BuildContextProperty> properties =
            List.of(
                BuildContextProperty.of("revision", "1.0.0", true),
                BuildContextProperty.of("revision", "2.0.0", true));

        String merged = ImporterVmOptions.merge("", properties);

        assertEquals(
            "the importer JVM keeps the last -D, so no reader of the same rows may report an earlier one",
            ContextLayers.valueOf(properties, "revision"),
            lastDefinitionOf(merged, "revision"));
    }

    @Test
    public void strip_noManagedRegion_returnsTheInputTrimmed()
    {
        assertEquals(USER_OPTIONS, ImporterVmOptions.strip("  " + USER_OPTIONS + "  "));
    }

    @Test
    public void strip_userOptionsOnBothSidesOfTheRegion_keepsBoth()
    {
        String existing = "-Xmx4g " + ImporterVmOptions.BEGIN + " -Denv=ci " + ImporterVmOptions.END + " -Xms1g";

        assertEquals("-Xmx4g -Xms1g", ImporterVmOptions.strip(existing));
    }

    private static String lastDefinitionOf(String merged, String key)
    {
        String prefix = "-D" + key + '=';
        String value = null;

        for (String argument : ParametersListUtil.parse(merged))
        {
            if (argument.startsWith(prefix))
            {
                value = argument.substring(prefix.length());
            }
        }

        return value;
    }
}
