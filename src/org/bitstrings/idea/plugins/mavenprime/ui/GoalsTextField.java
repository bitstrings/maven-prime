package org.bitstrings.idea.plugins.mavenprime.ui;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.execution.MavenGoalVocabulary;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.concurrency.AppExecutorUtil;

public final class GoalsTextField
{
    private GoalsTextField()
    {
    }

    public static TextFieldWithAutoCompletion<String> create(Project project)
    {
        TextFieldWithAutoCompletion<String> field =
            TextFieldWithAutoCompletion.create(project, MavenGoalVocabulary.phases(), false, StringUtils.EMPTY);

        field.setPlaceholder(MavenPrimeBundle.message("mavenprime.field.goals.empty"));

        ReadAction
            .nonBlocking(() -> MavenGoalVocabulary.allVariants(project))
            .expireWith(project)
            .finishOnUiThread(ModalityState.any(), field::setVariants)
            .submit(AppExecutorUtil.getAppExecutorService());

        return field;
    }
}
