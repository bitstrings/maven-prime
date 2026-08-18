package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.Map;

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase;

public class PomEffectiveModelHintsRenderingPlatformTest
    extends DeclarativeInlayHintsProviderTestCase
{
    private static final Map<String, Boolean> EVERY_OPTION_ON =
        Map.of(
            PomEffectiveModelHints.PROPERTY_VALUES_OPTION,
            Boolean.TRUE,
            PomEffectiveModelHints.OTHER_VALUES_OPTION,
            Boolean.TRUE,
            PomEffectiveModelHints.MANAGED_VERSIONS_OPTION,
            Boolean.TRUE);

    private static final String DECLARATION = "[/repo/parent/pom.xml|27:mavenprime.modelOrigin]";

    public void testProvider_aValueLongerThanOneInlaySegment_drawsItCappedToOneSegment()
    {
        drawn(
            "<project><groupId>${group}/*<#  = com.expretio.appia5.connec… #>*/"
                + "</groupId></project>");
    }

    public void testProvider_aValueThatFitsInOneSegment_drawsItAsASinglePiece()
    {
        drawn(
            "<project><version>${revision}/*<# " + DECLARATION + " = 1.4.0-SNAPSHOT #>*/"
                + "</version></project>");
    }

    public void testProvider_aPluginWithoutAVersion_drawsTheVersionItInherits()
    {
        drawn(
            "<project><build><plugins><plugin><artifactId>maven-compiler-plugin</artifactId>"
                + "/*<#  3.13.0 #>*/</plugin></plugins></build></project>");
    }

    private void drawn(String expected)
    {
        doTestProvider("pom.xml", expected, new FixtureProvider(), EVERY_OPTION_ON, null, false);
    }

    private final class FixtureProvider
        implements InlayHintsProvider
    {
        @Override
        public InlayHintsCollector createCollector(PsiFile file, Editor editor)
        {
            return new PomEffectiveModelHints.Collector(ProvenanceFixture.hintsFor(getProject()));
        }
    }
}
