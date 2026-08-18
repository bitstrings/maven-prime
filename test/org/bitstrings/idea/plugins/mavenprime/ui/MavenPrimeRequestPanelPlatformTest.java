package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.List;

import org.bitstrings.idea.plugins.mavenprime.execution.MavenPrimeRequest;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MavenPrimeRequestPanelPlatformTest
    extends BasePlatformTestCase
{
    private static final String DIRECTORY = "/tmp";

    private static final String GOAL = "verify";

    private static final String KEY = "maven.test.skip";

    private static final String OTHER_KEY = "revision";

    private static final String VALUE = "true";

    private static final String EDITED_VALUE = "false";

    private static final int SECOND_ROW = 1;

    private static final int ROW_PAST_THE_END = 9;

    public void testApplyPropertyEdit_theRowTheUserJustEdited_leavesItSelected()
    {
        MavenPrimeRequestPanel panel = panelShowingTwoProperties();

        panel.propertiesTable.getSelectionModel().setSelectionInterval(SECOND_ROW, SECOND_ROW);

        panel.applyPropertyEdit(SECOND_ROW, OTHER_KEY, EDITED_VALUE);

        assertEquals(
            "an event that clears the selection greys out the Edit button that opened the dialog, so "
                + "the same property cannot be edited twice without clicking it again",
            SECOND_ROW,
            panel.propertiesTable.getSelectedRow());
    }

    public void testApplyPropertyEdit_aRowNoLongerInTheTable_writesNothing()
    {
        assertFalse(
            "writing past the end would grow the table with a property nobody entered",
            panelShowingTwoProperties().applyPropertyEdit(ROW_PAST_THE_END, KEY, VALUE));
    }

    private MavenPrimeRequestPanel panelShowingTwoProperties()
    {
        MavenPrimeRequest request = MavenPrimeRequest.in(DIRECTORY, List.of(GOAL));

        request.properties.put(KEY, VALUE);
        request.properties.put(OTHER_KEY, VALUE);

        MavenPrimeRequestPanel panel = new MavenPrimeRequestPanel(getProject(), true);

        panel.resetFrom(request, List.of());

        return panel;
    }
}
