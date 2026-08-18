package org.bitstrings.idea.plugins.mavenprime.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.intellij.codeInsight.hints.declarative.CollapseState;
import com.intellij.codeInsight.hints.declarative.CollapsiblePresentationTreeBuilder;
import com.intellij.codeInsight.hints.declarative.HintFormat;
import com.intellij.codeInsight.hints.declarative.InlayActionData;
import com.intellij.codeInsight.hints.declarative.InlayActionHandler;
import com.intellij.codeInsight.hints.declarative.InlayPayload;
import com.intellij.codeInsight.hints.declarative.InlayPosition;
import com.intellij.codeInsight.hints.declarative.InlayTreeSink;
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class PomEffectiveModelHintsPlatformTest
    extends BasePlatformTestCase
{
    private static final String POM_WITH_PLACEHOLDER =
        "<project><properties><stamp>42</stamp></properties><version>${revision}</version></project>";

    private static final String POM_WITH_PLACEHOLDER_OUTSIDE_A_VERSION =
        "<project><groupId>${group}</groupId></project>";

    private static final String POM_WITH_MANAGED_DEPENDENCY =
        "<project><dependencies><dependency>"
            + "<groupId>org.apache.commons</groupId><artifactId>commons-lang3</artifactId>"
            + "</dependency></dependencies></project>";

    private static final String POM_WITH_PINNED_DEPENDENCY =
        "<project><dependencies><dependency>"
            + "<groupId>org.apache.commons</groupId><artifactId>commons-lang3</artifactId>"
            + "<version>3.13.0</version>"
            + "</dependency></dependencies></project>";

    private static final String POM_WITH_MANAGED_PLUGIN =
        "<project><build><plugins><plugin>"
            + "<groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId>"
            + "</plugin></plugins></build></project>";

    private static final String POM_WITH_PLUGIN_WITHOUT_A_GROUP =
        "<project><build><plugins><plugin>"
            + "<artifactId>maven-compiler-plugin</artifactId>"
            + "</plugin></plugins></build></project>";

    private static final String POM_WITH_PINNED_PLUGIN =
        "<project><build><plugins><plugin>"
            + "<artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version>"
            + "</plugin></plugins></build></project>";

    public void testGetActionHandler_theHandlerTheHintsPointAt_isRegistered()
    {
        assertNotNull(
            "an unregistered handler id leaves every hint inert when the user clicks it",
            InlayActionHandler.Companion.getActionHandler(PomEffectiveModelHints.HANDLER_ID));
    }

    public void testCollectFromElement_aVersionHoldingAPlaceholder_showsTheResolvedValue()
    {
        assertEquals(
            "the reader cannot see what a placeholder resolved to without leaving the file",
            List.of(" = " + ProvenanceFixture.REVISION),
            hintsIn(POM_WITH_PLACEHOLDER));
    }

    public void testCollectFromElement_aDependencyWithoutAVersion_showsTheVersionItInherits()
    {
        assertEquals(
            "a dependency with no version reads as if it had none at all",
            List.of(" " + ProvenanceFixture.LANG3_VERSION),
            hintsIn(POM_WITH_MANAGED_DEPENDENCY));
    }

    public void testCollectFromElement_aDependencyPinningItsOwnVersion_showsNothing()
    {
        assertEquals(
            "repeating a version that is already on the line is noise",
            List.of(),
            hintsIn(POM_WITH_PINNED_DEPENDENCY));
    }

    public void testCollectFromElement_aPluginWithoutAVersion_showsTheVersionItInherits()
    {
        assertEquals(
            "a plugin version comes from plugin management or the super POM, and neither is in this file",
            List.of(" " + ProvenanceFixture.COMPILER_PLUGIN_VERSION),
            hintsIn(POM_WITH_MANAGED_PLUGIN));
    }

    public void testCollectFromElement_aPluginThatOmitsItsGroup_stillShowsTheVersionItInherits()
    {
        assertEquals(
            "Maven defaults the group of a plugin, so requiring one in the POM would miss the common case",
            List.of(" " + ProvenanceFixture.COMPILER_PLUGIN_VERSION),
            hintsIn(POM_WITH_PLUGIN_WITHOUT_A_GROUP));
    }

    public void testCollectFromElement_aPluginPinningItsOwnVersion_showsNothing()
    {
        assertEquals(List.of(), hintsIn(POM_WITH_PINNED_PLUGIN));
    }

    public void testCollectFromElement_aResolvedValueWithAKnownDeclaration_isClickable()
    {
        RecordingSink sink = collect(POM_WITH_PLACEHOLDER);

        assertEquals(1, sink.texts.size());
        assertNotNull(
            "a hint that knows where the value came from but does not offer to open it wastes the answer",
            sink.actions.get(0));
    }

    public void testCollectFromElement_resolvedValuesSwitchedOff_showsNoPropertyValues()
    {
        assertEquals(
            "the option in Inlay Hints has to actually silence the hints it names",
            List.of(),
            collect(POM_WITH_PLACEHOLDER, PomEffectiveModelHints.PROPERTY_VALUES_OPTION).texts);
    }

    public void testCollectFromElement_valuesOutsideAVersionSwitchedOff_stillShowsValuesInVersions()
    {
        assertEquals(
            "a groupId is noisy where a version is not, which is the whole reason the switches are separate",
            List.of(" = " + ProvenanceFixture.REVISION),
            collect(POM_WITH_PLACEHOLDER, PomEffectiveModelHints.OTHER_VALUES_OPTION).texts);
    }

    public void testCollectFromElement_valuesOutsideAVersionSwitchedOff_showsNoGroupId()
    {
        assertEquals(
            List.of(),
            collect(POM_WITH_PLACEHOLDER_OUTSIDE_A_VERSION, PomEffectiveModelHints.OTHER_VALUES_OPTION).texts);
    }

    public void testCollectFromElement_valuesInVersionsSwitchedOff_stillShowsValuesElsewhere()
    {
        assertFalse(
            "one switch must not silence the other, or the two options are really one",
            collect(POM_WITH_PLACEHOLDER_OUTSIDE_A_VERSION, PomEffectiveModelHints.PROPERTY_VALUES_OPTION)
                .texts
                .isEmpty());
    }

    public void testCollectFromElement_inheritedVersionsSwitchedOff_showsNoVersions()
    {
        assertEquals(
            "the option in Inlay Hints has to actually silence the hints it names",
            List.of(),
            collect(POM_WITH_MANAGED_DEPENDENCY, PomEffectiveModelHints.MANAGED_VERSIONS_OPTION).texts);
    }

    public void testCollectFromElement_inheritedVersionsSwitchedOff_showsNoPluginVersions()
    {
        assertEquals(
            List.of(),
            collect(POM_WITH_MANAGED_PLUGIN, PomEffectiveModelHints.MANAGED_VERSIONS_OPTION).texts);
    }

    public void testCollectFromElement_onlyResolvedValuesSwitchedOff_stillShowsInheritedVersions()
    {
        assertEquals(
            "one switch must not silence the other, or the two options are really one",
            List.of(" " + ProvenanceFixture.LANG3_VERSION),
            collect(POM_WITH_MANAGED_DEPENDENCY, PomEffectiveModelHints.PROPERTY_VALUES_OPTION).texts);
    }

    private List<String> hintsIn(String pom)
    {
        return collect(pom).texts;
    }

    private RecordingSink collect(String pom, String... disabledOptions)
    {
        PsiFile file = myFixture.configureByText("pom.xml", pom);

        PomEffectiveModelHints.Collector collector =
            new PomEffectiveModelHints.Collector(ProvenanceFixture.hintsFor(getProject()));

        RecordingSink sink = new RecordingSink(disabledOptions);

        PsiTreeUtil.processElements(
            file,
            element ->
            {
                collector.collectFromElement(element, sink);

                return true;
            });

        return sink;
    }

    private static final class RecordingSink
        implements InlayTreeSink
    {
        private final List<String> texts = new ArrayList<>();

        private final List<InlayActionData> actions = new ArrayList<>();

        private final Set<String> disabledOptions;

        RecordingSink(String... disabledOptions)
        {
            this.disabledOptions = Set.of(disabledOptions);
        }

        @Override
        public void addPresentation(
            InlayPosition position,
            List<InlayPayload> payloads,
            String tooltip,
            HintFormat format,
            Function1<? super PresentationTreeBuilder, Unit> builder)
        {
            builder.invoke(new RecordingBuilder(texts, actions));
        }

        @Override
        public void whenOptionEnabled(String optionId, Function0<Unit> block)
        {
            if (!disabledOptions.contains(optionId))
            {
                block.invoke();
            }
        }

        private final class RecordingBuilder
            implements CollapsiblePresentationTreeBuilder
        {
            private final List<String> recordedTexts;

            private final List<InlayActionData> recordedActions;

            RecordingBuilder(List<String> recordedTexts, List<InlayActionData> recordedActions)
            {
                this.recordedTexts = recordedTexts;
                this.recordedActions = recordedActions;
            }

            @Override
            public void text(String text, InlayActionData action)
            {
                recordedTexts.add(asThePlatformWouldDrawIt(text));
                recordedActions.add(action);
            }

            private String asThePlatformWouldDrawIt(String text)
            {
                return (text.length() > InlaySegments.MAX_SEGMENT_LENGTH)
                    ? (text.substring(0, InlaySegments.MAX_SEGMENT_LENGTH) + "…")
                    : text;
            }

            @Override
            public void list(Function1<? super PresentationTreeBuilder, Unit> builder)
            {
                builder.invoke(this);
            }

            @Override
            public void collapsibleList(
                CollapseState state,
                Function1<? super CollapsiblePresentationTreeBuilder, Unit> expanded,
                Function1<? super CollapsiblePresentationTreeBuilder, Unit> collapsedForm)
            {
                collapsedForm.invoke(this);
            }

            @Override
            public void toggleButton(Function1<? super PresentationTreeBuilder, Unit> builder)
            {
                builder.invoke(this);
            }

            @Override
            public void clickHandlerScope(
                InlayActionData action, Function1<? super PresentationTreeBuilder, Unit> builder)
            {
                throw new UnsupportedOperationException("the collector attaches actions to the text itself");
            }
        }
    }
}
