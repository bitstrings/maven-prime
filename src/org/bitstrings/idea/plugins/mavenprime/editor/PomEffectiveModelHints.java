package org.bitstrings.idea.plugins.mavenprime.editor;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.editor.EffectiveModelHints.Hint;
import org.bitstrings.idea.plugins.mavenprime.model.EffectiveModel;
import org.bitstrings.idea.plugins.mavenprime.model.MavenModuleFacts;
import org.bitstrings.idea.plugins.mavenprime.model.ModelRefresher;
import org.bitstrings.idea.plugins.mavenprime.model.ModuleFacts;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.codeInsight.hints.declarative.HintFormat;
import com.intellij.codeInsight.hints.declarative.InlayActionData;
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.InlayTreeSink;
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition;
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector;
import com.intellij.codeInsight.hints.declarative.StringInlayActionPayload;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;

import kotlin.Unit;

public final class PomEffectiveModelHints
    implements InlayHintsProvider
{
    static final String HANDLER_ID = "mavenprime.modelOrigin";

    static final String PROPERTY_VALUES_OPTION = "mavenprime.effectiveModel.propertyValues";

    static final String OTHER_VALUES_OPTION = "mavenprime.effectiveModel.otherValues";

    static final String MANAGED_VERSIONS_OPTION = "mavenprime.effectiveModel.managedVersions";

    private static final String DEPENDENCY_TAG = "dependency";

    private static final String PLUGIN_TAG = "plugin";

    private static final String GROUP_ID_TAG = "groupId";

    private static final String ARTIFACT_ID_TAG = "artifactId";

    private static final String VERSION_TAG = "version";

    private static final String TYPE_TAG = "type";

    private static final String CLASSIFIER_TAG = "classifier";

    private static final String VALUE_PREFIX = " = ";

    private static final String VERSION_PREFIX = " ";

    @Override
    public InlayHintsCollector createCollector(PsiFile file, Editor editor)
    {
        Project project = file.getProject();

        MavenProject module = MavenProjects.byPomFile(project, file.getVirtualFile());

        if (module == null)
        {
            return null;
        }

        EffectiveModel model = EffectiveModel.getInstance(project);

        ModuleFacts facts = new MavenModuleFacts(module);

        if (!model.hasDataFor(facts.moduleKey()))
        {
            ApplicationManager
                .getApplication()
                .invokeLater(
                    () -> ModelRefresher.getInstance(project).refreshUnread(), project.getDisposed());
        }

        return new Collector(new EffectiveModelHints(model, facts));
    }

    static final class Collector
        implements SharedBypassCollector
    {
        private final EffectiveModelHints hints;

        Collector(EffectiveModelHints hints)
        {
            this.hints = hints;
        }

        @Override
        public void collectFromElement(PsiElement element, InlayTreeSink sink)
        {
            if (element instanceof XmlText)
            {
                collectPropertyValue((XmlText) element, sink);
            }
            else if (element instanceof XmlTag)
            {
                collectManagedVersion((XmlTag) element, sink);
            }
        }

        private void collectPropertyValue(XmlText text, InlayTreeSink sink)
        {
            XmlTag parent = text.getParentTag();

            sink.whenOptionEnabled(
                ((parent != null) && VERSION_TAG.equals(parent.getName()))
                    ? PROPERTY_VALUES_OPTION
                    : OTHER_VALUES_OPTION,
                () ->
                {
                    show(
                        sink,
                        text.getTextRange().getEndOffset(),
                        VALUE_PREFIX,
                        hints.propertyValue(text.getValue()));

                    return Unit.INSTANCE;
                });
        }

        private void collectManagedVersion(XmlTag tag, InlayTreeSink sink)
        {
            boolean plugin = PLUGIN_TAG.equals(tag.getName());

            if (
                (!plugin && !DEPENDENCY_TAG.equals(tag.getName()))
                    || (tag.findFirstSubTag(VERSION_TAG) != null)
            )
            {
                return;
            }

            XmlTag artifactId = tag.findFirstSubTag(ARTIFACT_ID_TAG);

            if (artifactId == null)
            {
                return;
            }

            sink.whenOptionEnabled(
                MANAGED_VERSIONS_OPTION,
                () ->
                {
                    show(
                        sink,
                        artifactId.getTextRange().getEndOffset(),
                        VERSION_PREFIX,
                        plugin ? pluginVersionOf(tag) : dependencyVersionOf(tag));

                    return Unit.INSTANCE;
                });
        }

        private Hint pluginVersionOf(XmlTag tag)
        {
            return hints.managedPluginVersion(valueOf(tag, GROUP_ID_TAG), valueOf(tag, ARTIFACT_ID_TAG));
        }

        private Hint dependencyVersionOf(XmlTag tag)
        {
            return hints.managedVersion(
                valueOf(tag, GROUP_ID_TAG),
                valueOf(tag, ARTIFACT_ID_TAG),
                valueOf(tag, TYPE_TAG),
                valueOf(tag, CLASSIFIER_TAG));
        }

        private void show(InlayTreeSink sink, int offset, String prefix, Hint hint)
        {
            if (hint == null)
            {
                return;
            }

            InlayActionData action =
                hint.origin().isNavigable()
                    ? new InlayActionData(
                        new StringInlayActionPayload(ModelOriginPayloads.encode(hint.origin())), HANDLER_ID)
                    : null;

            sink.addPresentation(
                new InlineInlayPosition(offset, true, 0),
                null,
                tooltipFor(hint),
                HintFormat.Companion.getDefault(),
                builder ->
                {
                    InlaySegments.render(builder, prefix, hint.text(), action);

                    return Unit.INSTANCE;
                });
        }

        private static String tooltipFor(Hint hint)
        {
            return hint.origin().isNavigable()
                ? MavenPrimeBundle.message(
                    "mavenprime.inlay.declaredIn", hint.origin().file(), Integer.valueOf(hint.origin().line()))
                : null;
        }

        private static String valueOf(XmlTag tag, String name)
        {
            XmlTag child = tag.findFirstSubTag(name);

            return (child == null) ? StringUtils.EMPTY : child.getValue().getTrimmedText();
        }
    }
}
