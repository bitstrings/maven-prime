package org.bitstrings.idea.plugins.mavenprime.dependencies;

import static org.bitstrings.idea.plugins.mavenprime.dependencies.DependencyAnalysisTest.artifact;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;

public class DependencyRowTextPlatformTest
    extends BasePlatformTestCase
{
    public void testVersionAttributes_omittedNode_isGrayedRatherThanRed()
    {
        assertEquals(
            SimpleTextAttributes.GRAYED_ATTRIBUTES,
            DependencyRowText.versionAttributes(ResolutionReason.DUPLICATE, true, true));
    }

    public void testVersionAttributes_conflictedAndUsed_isError()
    {
        assertEquals(
            SimpleTextAttributes.ERROR_ATTRIBUTES,
            DependencyRowText.versionAttributes(ResolutionReason.INCLUDED, true, true));
    }

    public void testVersionAttributes_plainNode_isRegular()
    {
        assertEquals(
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            DependencyRowText.versionAttributes(ResolutionReason.INCLUDED, false, true));
    }

    public void testAppendName_groupIdHidden_writesTheArtifactIdAlone()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendName(
            artifact("com.lib", "shared", "2.0"),
            false,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            row::append);

        assertEquals("shared", row.getCharSequence(false).toString());
    }

    public void testAppendName_groupIdShown_prefixesTheGroupId()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendName(
            artifact("com.lib", "shared", "2.0"),
            true,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            row::append);

        assertEquals("com.lib:shared", row.getCharSequence(false).toString());
    }

    public void testAppendVersion_withoutATransition_writesTheVersionAlone()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendVersion(
            node(MavenArtifactState.ADDED, null),
            ResolutionReason.INCLUDED,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            row::append);

        assertEquals(":2.0", row.getCharSequence(false).toString());
    }

    public void testAppendVersion_managedNode_writesTheDeclaredVersionArrowedToTheEffectiveOne()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendVersion(
            node(MavenArtifactState.ADDED, "1.0"),
            ResolutionReason.MANAGED,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            row::append);

        assertEquals(":1.0" + DependencyRowText.ARROW + "2.0", row.getCharSequence(false).toString());
    }

    public void testAppendScope_compileScope_writesNothingBecauseItIsMavenTheDefault()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendScope(row, artifact("com.lib", "shared", "2.0"), true);

        assertEquals(0, row.getFragmentCount());
    }

    public void testAppendScope_testScope_writesTheScope()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendScope(row, scoped("test"), true);

        assertEquals(" test", row.getCharSequence(false).toString());
    }

    public void testVersionAttributes_conflictedWithColorOff_fallsBackToRegularSoTheIconCarriesIt()
    {
        assertEquals(
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            DependencyRowText.versionAttributes(ResolutionReason.INCLUDED, true, false));
    }

    public void testAppendScope_colorOff_stillWritesTheScopeText()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendScope(row, scoped("test"), false);

        assertEquals(" test", row.getCharSequence(false).toString());
    }

    public void testAppendSize_subtreeLargerThanTheArtifact_writesBothNumbers()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        MavenArtifactNode root = sizedNode("/repo/root.jar");

        root.setDependencies(List.of(sizedNode("/repo/child.jar")));

        DependencyRowText.appendSize(
            row,
            new ArtifactSizes(
                Map.of(
                    Path.of("/repo/root.jar"), Long.valueOf(1024L),
                    Path.of("/repo/child.jar"), Long.valueOf(2048L))),
            root);

        assertTrue(
            "a subtree bigger than the artifact must show the total in parentheses",
            row.getCharSequence(false).toString().contains("("));
    }

    public void testAppendSize_leafArtifact_writesOneNumberOnly()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendSize(
            row,
            new ArtifactSizes(Map.of(Path.of("/repo/root.jar"), Long.valueOf(1024L))),
            sizedNode("/repo/root.jar"));

        assertFalse(
            "an artifact whose subtree adds nothing must not repeat the same number",
            row.getCharSequence(false).toString().contains("("));
    }

    public void testAppendSize_anyArtifact_separatesTheSizeFromTheCoordinate()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendSize(
            row,
            new ArtifactSizes(Map.of(Path.of("/repo/root.jar"), Long.valueOf(1024L))),
            sizedNode("/repo/root.jar"));

        assertTrue(
            "the size must carry its own separator, never butt against the coordinate",
            row.getCharSequence(false).toString().startsWith(DependencyRowText.SEPARATOR));
    }

    private static MavenArtifactNode sizedNode(String path)
    {
        MavenArtifact artifact =
            new MavenArtifact(
                "com.lib",
                Path.of(path).getFileName().toString(),
                "1.0",
                "1.0",
                "jar",
                null,
                "compile",
                false,
                "jar",
                new File(path),
                null,
                false,
                false);

        return new MavenArtifactNode(null, artifact, MavenArtifactState.ADDED, null, "compile", null, null);
    }

    public void testAppendMarker_duplicate_namesTheStateInPlainWordsRatherThanASymbol()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendMarker(row, ResolutionReason.DUPLICATE, true);

        assertEquals(" duplicate", row.getCharSequence(false).toString());
    }

    public void testAppendMarker_included_writesNothing()
    {
        SimpleColoredComponent row = new SimpleColoredComponent();

        DependencyRowText.appendMarker(row, ResolutionReason.INCLUDED, true);

        assertEquals(0, row.getFragmentCount());
    }

    private static MavenArtifactNode node(MavenArtifactState state, String premanagedVersion)
    {
        return new MavenArtifactNode(
            null, artifact("com.lib", "shared", "2.0"), state, null, "compile", premanagedVersion, null);
    }

    private static MavenArtifact scoped(String scope)
    {
        return new MavenArtifact(
            "com.lib", "shared", "2.0", "2.0", "jar", null, scope, false, "jar", null, null, false, false);
    }
}
