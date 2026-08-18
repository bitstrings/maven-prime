package org.bitstrings.idea.plugins.mavenprime.settings;

import org.bitstrings.idea.plugins.mavenprime.goals.GoalDefinition;
import org.bitstrings.idea.plugins.mavenprime.goals.GoalScope;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class GoalTablePanelPlatformTest
    extends BasePlatformTestCase
{
    public void testAppend_aGoalJustAdded_selectsItSoTheUserSeesWhatAddCreated()
    {
        GoalTablePanel panel = panel();

        panel.append(GoalDefinition.of("clean install"));

        assertNotNull(
            "nothing is selected after Add, so a new row is indistinguishable from no row",
            panel.goalList.getSelectedValue());
    }

    public void testAppend_aSecondGoal_movesTheSelectionToTheNewOne()
    {
        GoalTablePanel panel = panel();

        panel.append(GoalDefinition.of("clean install"));

        GoalDefinition second = GoalDefinition.of("verify");

        panel.append(second);

        assertEquals(
            "Add left the selection on the previous goal, so the new one goes unnoticed",
            second,
            panel.goalList.getSelectedValue().definition());
    }

    public void testAppend_aDuplicateOfTheSelectedGoal_movesTheSelectionToTheNewRow()
    {
        GoalTablePanel panel = panel();

        panel.append(new GoalDefinition());

        panel.append(new GoalDefinition());

        assertEquals(
            "Add left the selection on the value-equal goal above, so Edit and Remove act on that one",
            1,
            panel.goalList.getSelectedIndex());
    }

    private GoalTablePanel panel()
    {
        return new GoalTablePanel(getProject(), GoalScope.PROJECT, (scope, definition) -> { });
    }
}
