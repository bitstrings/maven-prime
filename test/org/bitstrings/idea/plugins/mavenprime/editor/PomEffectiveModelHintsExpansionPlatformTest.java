package org.bitstrings.idea.plugins.mavenprime.editor;

import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.InlayProviderPassInfo;
import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayHintsPass;
import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayRenderer;
import com.intellij.codeInsight.hints.declarative.impl.InlayPresentationEntry;
import com.intellij.codeInsight.hints.declarative.impl.InlayPresentationList;
import com.intellij.codeInsight.hints.declarative.impl.TextInlayPresentationEntry;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class PomEffectiveModelHintsExpansionPlatformTest
    extends BasePlatformTestCase
{
    private static final String PROVIDER_ID = "mavenprime.effectiveModel";

    private static final String POM_WITH_A_LONG_VALUE =
        "<project><groupId>${group}</groupId></project>";

    private static final Map<String, Boolean> EVERY_OPTION_ON =
        Map.of(
            PomEffectiveModelHints.PROPERTY_VALUES_OPTION,
            Boolean.TRUE,
            PomEffectiveModelHints.OTHER_VALUES_OPTION,
            Boolean.TRUE,
            PomEffectiveModelHints.MANAGED_VERSIONS_OPTION,
            Boolean.TRUE);

    public void testPresentation_aValueTooLongForTheLine_startsCappedWithinOneSegment()
    {
        String drawn = drawn(presentationOf(POM_WITH_A_LONG_VALUE));

        assertTrue(drawn, drawn.endsWith(InlaySegments.ELLIPSIS));
        assertEquals(
            "a placeholder over the limit is cut again by the platform, showing two ellipses",
            InlaySegments.MAX_SEGMENT_LENGTH,
            drawn.length());
    }

    public void testPresentation_aClickOnTheCappedValue_expandsItToTheWholeValue()
    {
        InlayPresentationList presentation = presentationOf(POM_WITH_A_LONG_VALUE);

        click(presentation, presentation.getEntries()[0]);

        assertEquals(
            "a cap the reader cannot open is the truncation it replaced",
            " = " + ProvenanceFixture.LONG_VALUE,
            drawn(presentation));
    }

    public void testPresentation_aClickOnTheExpandedValuesPrefix_capsItAgain()
    {
        InlayPresentationList presentation = presentationOf(POM_WITH_A_LONG_VALUE);

        click(presentation, presentation.getEntries()[0]);
        click(presentation, presentation.getEntries()[0]);

        assertTrue(drawn(presentation), drawn(presentation).endsWith(InlaySegments.ELLIPSIS));
    }

    public void testPresentation_theExpandedValue_keepsTheLinkToItsDeclaration()
    {
        InlayPresentationList presentation = presentationOf(POM_WITH_A_LONG_VALUE);

        click(presentation, presentation.getEntries()[0]);

        assertNotNull(
            "expanding must not cost the click-through that answers where the value came from",
            presentation.getEntries()[1].getClickArea());
    }

    private void click(InlayPresentationList presentation, InlayPresentationEntry entry)
    {
        Editor editor = myFixture.getEditor();

        entry.handleClick(
            new EditorMouseEvent(
                editor,
                new MouseEvent(
                    editor.getContentComponent(), MouseEvent.MOUSE_CLICKED, 0L, 0, 0, 0, 1, false),
                EditorMouseEventArea.EDITING_AREA),
            presentation,
            false);
    }

    private static String drawn(InlayPresentationList presentation)
    {
        return Arrays
            .stream(presentation.getEntries())
            .map(entry -> ((TextInlayPresentationEntry) entry).getText())
            .collect(Collectors.joining());
    }

    private InlayPresentationList presentationOf(String pom)
    {
        PsiFile file = myFixture.configureByText("pom.xml", pom);

        Editor editor = myFixture.getEditor();

        ActionUtil
            .underModalProgress(
                getProject(),
                "",
                () ->
                {
                    DeclarativeInlayHintsPass pass =
                        new DeclarativeInlayHintsPass(
                            file,
                            editor,
                            List.of(
                                new InlayProviderPassInfo(
                                    new FixtureProvider(), PROVIDER_ID, EVERY_OPTION_ON)),
                            false,
                            false);

                    pass.doCollectInformation(new EmptyProgressIndicator());

                    return pass;
                })
            .applyInformationToEditor();

        List<Inlay<?>> inlays =
            editor.getInlayModel().getInlineElementsInRange(0, editor.getDocument().getTextLength());

        assertEquals("the hint has to exist before its behaviour can be read", 1, inlays.size());

        return ((DeclarativeInlayRenderer) inlays.get(0).getRenderer()).getPresentationList();
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
