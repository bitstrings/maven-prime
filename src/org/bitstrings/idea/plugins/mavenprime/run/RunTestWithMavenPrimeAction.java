package org.bitstrings.idea.plugins.mavenprime.run;

import org.bitstrings.idea.plugins.mavenprime.execution.ExecutionMode;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;
import org.bitstrings.idea.plugins.mavenprime.util.MavenProjects;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class RunTestWithMavenPrimeAction
    extends AnAction
    implements DumbAware
{
    private final ExecutionMode mode;

    protected RunTestWithMavenPrimeAction(ExecutionMode mode)
    {
        this.mode = mode;
    }

    @Override
    public void update(AnActionEvent event)
    {
        event.getPresentation().setEnabledAndVisible(selectionIn(event) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        TestSelection selection = selectionIn(event);

        Project project = event.getProject();

        if ((selection == null) || (project == null))
        {
            return;
        }

        MavenProject module = moduleOf(project, selection);

        if (module == null)
        {
            return;
        }

        run(
            project,
            MavenPrimeTestConfigurationProducer.requestFor(module.getDirectory(), selection),
            MavenPrimeTestConfigurationProducer.nameFor(selection));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }

    private void run(Project project, MavenPrimeRequest request, String name)
    {
        RunnerAndConfigurationSettings settings =
            RunManager
                .getInstance(project)
                .createConfiguration(name, MavenPrimeRunConfigurationType.getInstance().getFactory());

        ((MavenPrimeRunConfiguration) settings.getConfiguration()).setRequest(request);

        RunManager.getInstance(project).setTemporaryConfiguration(settings);

        ProgramRunnerUtil.executeConfiguration(settings, mode.getExecutor());
    }

    private static TestSelection selectionIn(AnActionEvent event)
    {
        if (event.getProject() == null)
        {
            return null;
        }

        TestSelection selected = TestSelections.of(event.getData(CommonDataKeys.PSI_ELEMENT));

        if (selected != null)
        {
            return selected;
        }

        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);

        Editor editor = event.getData(CommonDataKeys.EDITOR);

        return ((file == null) || (editor == null))
            ? null
            : TestSelections.of(
                elementFor(file, editor, event.getData(EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR)));
    }

    static PsiElement elementFor(PsiFile file, Editor editor, Integer clickedLine)
    {
        Document document = editor.getDocument();

        int offset =
            ((clickedLine == null) || (clickedLine.intValue() >= document.getLineCount()))
                ? editor.getCaretModel().getOffset()
                : firstTokenOffset(document, clickedLine.intValue());

        return file.findElementAt(offset);
    }

    private static int firstTokenOffset(Document document, int line)
    {
        CharSequence text = document.getCharsSequence();

        int end = document.getLineEndOffset(line);

        int offset = document.getLineStartOffset(line);

        while ((offset < end) && Character.isWhitespace(text.charAt(offset)))
        {
            offset++;
        }

        return offset;
    }

    private static MavenProject moduleOf(Project project, TestSelection selection)
    {
        PsiFile file = selection.element().getContainingFile();

        return (file == null) ? null : MavenProjects.forFile(project, file.getVirtualFile());
    }

    public static final class Run
        extends RunTestWithMavenPrimeAction
    {
        public Run()
        {
            super(ExecutionMode.RUN);
        }
    }

    public static final class Debug
        extends RunTestWithMavenPrimeAction
    {
        public Debug()
        {
            super(ExecutionMode.DEBUG);
        }
    }
}
