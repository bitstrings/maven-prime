package org.bitstrings.idea.plugins.mavenprime.context;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.settings.MavenPrimeSettings;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BuildContextImporterPlatformTest
    extends BasePlatformTestCase
{
    private static final String USER_OPTIONS = "-Xmx4g";

    private static final String NOTHING = "";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        importerOptions(NOTHING);

        MavenPrimeSettings.getInstance(getProject()).importerVmOptions = NOTHING;
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            RunAll.runAll(
                () -> importerOptions(NOTHING),
                () -> context().loadState(new BuildContextProperties()));
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testSetProperties_aPropertyMarkedForImport_doesNotTouchTheImporterOnItsOwn()
    {
        importerOptions(USER_OPTIONS);

        context().setProperties(List.of(BuildContextProperty.of("env", "ci", true)));

        assertEquals(
            "editing a property must not rewrite a shared IDE setting behind the user",
            USER_OPTIONS,
            importerOptions());
    }

    public void testApplyToImporter_askedFor_writesThePropertiesMarkedForImport()
    {
        importerOptions(USER_OPTIONS);

        context().setProperties(List.of(BuildContextProperty.of("env", "ci", true)));
        context().applyToImporter();

        assertEquals(USER_OPTIONS + " -Denv=ci", importerOptions());
    }

    public void testApplyToImporter_appliedTwice_leavesOneDefinition()
    {
        context().setProperties(List.of(BuildContextProperty.of("env", "ci", true)));
        context().applyToImporter();
        context().applyToImporter();

        assertEquals("-Denv=ci", importerOptions());
    }

    public void testApplyToImporter_nothingLeftMarked_takesBackOnlyWhatItWrote()
    {
        importerOptions(USER_OPTIONS);

        context().setProperties(List.of(BuildContextProperty.of("env", "ci", true)));
        context().applyToImporter();

        context().setProperties(List.of(BuildContextProperty.of("env", "ci", false)));
        context().applyToImporter();

        assertEquals(USER_OPTIONS, importerOptions());
    }

    public void testApplyToImporter_theUserEditedTheField_removesNothingOfTheirs()
    {
        context().setProperties(List.of(BuildContextProperty.of("env", "ci", true)));
        context().applyToImporter();

        importerOptions("-Denv=staging");

        context().setProperties(List.of(BuildContextProperty.of("env", "qa", true)));
        context().applyToImporter();

        assertEquals(
            "what we wrote is gone from the field, so there is nothing of ours left to reclaim",
            "-Denv=staging -Denv=qa",
            importerOptions());
    }

    public void testSync_aRegionLeftByAnOlderVersion_isTakenBackOut()
    {
        importerOptions(
            USER_OPTIONS + ' ' + ImporterVmOptions.LEGACY_BEGIN + " -Denv=ci "
                + ImporterVmOptions.LEGACY_END);

        context().sync();

        assertEquals(USER_OPTIONS, importerOptions());
    }

    private BuildContext context()
    {
        return BuildContext.getInstance(getProject());
    }

    private String importerOptions()
    {
        return MavenProjectsManager.getInstance(getProject()).getImportingSettings().getVmOptionsForImporter();
    }

    private void importerOptions(String options)
    {
        MavenProjectsManager.getInstance(getProject()).getImportingSettings().setVmOptionsForImporter(options);
    }
}
