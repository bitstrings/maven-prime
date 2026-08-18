package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContext;
import org.bitstrings.idea.plugins.mavenprime.context.BuildContextProperty;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileEntry;
import org.bitstrings.idea.plugins.mavenprime.ui.ProfileState;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.ThreeStateCheckBox;

public class BuildContextPanelPlatformTest
    extends BasePlatformTestCase
{
    private static final String PROFILE = "release";

    private static final String KEY = "skipTests";

    private static final String VALUE = "true";

    private static final String EDITED_VALUE = "false";

    public void testCustomizeRenderer_anEnvironmentFieldWithNoValue_stillShowsTheSeparator()
    {
        BuildContextPanel.EnvironmentField field = BuildContextPanel.EnvironmentField.JRE;

        assertTrue(
            "this fixture has a JRE set, so the blank-value path is not the one under test",
            StringUtils.isBlank(field.valueOf(BuildContext.getInstance(getProject()).getEnvironment())));

        assertEquals(
            "an environment row that drops the separator sits ragged against the rows that keep one",
            field.getTitle() + " = ",
            textOf(field));
    }

    public void testCustomizeRenderer_aPropertyWithAValue_paintsTheValueBesideItsKey()
    {
        assertEquals(
            "a value in a column of its own leaves the user pairing it back to its row by eye",
            KEY + " = " + VALUE,
            textOf(BuildContextProperty.of(KEY, VALUE, false)));
    }

    public void testCustomizeRenderer_aPropertyWithNoValue_paintsNoDanglingSeparator()
    {
        assertEquals(
            "a separator with nothing after it reads as a value the panel failed to load",
            KEY,
            textOf(BuildContextProperty.of(KEY, "", false)));
    }

    public void testCustomizeRenderer_aPropertyPassedToTheMavenImport_marksItBesideTheValue()
    {
        assertEquals(
            "the import flag has no column left to live in, so the row itself has to carry it",
            KEY + " = " + VALUE + ' ' + marker(),
            textOf(BuildContextProperty.of(KEY, VALUE, true)));
    }

    public void testCustomizeRenderer_aPropertyPassedToTheMavenImport_setsThatMarkerApartInItalic()
    {
        SimpleTextAttributes attributes =
            attributesOf(textRendererOf(BuildContextProperty.of(KEY, VALUE, true)), ' ' + marker());

        assertNotNull("the marker was never painted as a fragment of its own", attributes);
        assertTrue(
            "a marker in the same face as the value reads as part of the value",
            (attributes.getStyle() & SimpleTextAttributes.STYLE_ITALIC) != 0);
    }

    public void testCustomizeRenderer_theImportMarker_paintsInAVisibleColor()
    {
        SimpleTextAttributes attributes =
            attributesOf(textRendererOf(BuildContextProperty.of(KEY, VALUE, true)), ' ' + marker());

        assertNotNull("the marker was never painted as a fragment of its own", attributes);
        assertTrue(
            "a fallback that resolves to the platform's transparent marker color paints the (Import) "
                + "note invisibly under every theme that leaves the key undefined",
            (attributes.getFgColor() != null) && (attributes.getFgColor().getAlpha() > 0));
    }

    public void testCustomizeRenderer_aPropertyKeptOutOfTheMavenImport_carriesNoMarker()
    {
        assertFalse(
            "a marker on every property tells the user nothing about which ones reach the importer",
            textOf(BuildContextProperty.of(KEY, VALUE, false)).contains(marker()));
    }

    public void testCustomizeRenderer_aProfileRow_paintsNoStateWordBesideItsName()
    {
        assertEquals(
            "spelling the state out beside the checkbox says the same thing twice",
            PROFILE,
            textOf(new ProfileEntry(PROFILE, ProfileState.ENABLED)));
    }

    public void testCustomizeRenderer_aProfileRow_showsTheCheckboxSoTheRowLooksSettable()
    {
        assertTrue(
            "with nothing in the marker slot the user has no way to tell the row can be changed",
            checkboxOf(new ProfileEntry(PROFILE, ProfileState.NOT_SET)).isVisible());
    }

    public void testCustomizeRenderer_aPropertyRow_hidesTheCheckbox()
    {
        assertFalse(
            "a checkbox on a property offers to toggle something the row does not have",
            checkboxOf(BuildContextProperty.of(KEY, VALUE, false)).isVisible());
    }

    public void testCustomizeRenderer_aProfileForcedOn_checksItsCheckbox()
    {
        assertSame(
            "the state Maven will actually apply has to be the one the checkbox shows",
            ThreeStateCheckBox.State.SELECTED,
            checkboxOf(new ProfileEntry(PROFILE, ProfileState.ENABLED)).getState());
    }

    public void testCustomizeRenderer_aProfileNobodyHasTouched_leavesTheDashThatMeansMavenDecides()
    {
        assertSame(
            "an untouched profile marked like a set one invents a decision the user never made",
            ThreeStateCheckBox.State.DONT_CARE,
            checkboxOf(new ProfileEntry(PROFILE, ProfileState.NOT_SET)).getState());
    }

    public void testCustomizeRenderer_aProfileForcedOff_emptiesItsCheckboxRatherThanDashingIt()
    {
        assertSame(
            "a dash is the platform's indeterminate mark, so it would read as a default rather than off",
            ThreeStateCheckBox.State.NOT_SELECTED,
            checkboxOf(new ProfileEntry(PROFILE, ProfileState.DISABLED)).getState());
    }

    public void testApplyProfileState_theStatePickedFromTheDropdown_writesItToThatProfile()
    {
        ProfileEntry entry = new ProfileEntry(PROFILE, ProfileState.NOT_SET);

        new BuildContextPanel(getProject()).applyProfileState(pathTo(entry), ProfileState.DISABLED);

        assertEquals(
            "a pick the panel drops leaves the checkbox showing a state the build will not use",
            ProfileState.DISABLED,
            entry.getState());
    }

    public void testApplyProfileState_aPropertyRow_writesNothing()
    {
        assertFalse(
            "a property row taking a profile state would overwrite the row the user actually picked",
            new BuildContextPanel(getProject())
                .applyProfileState(pathTo(BuildContextProperty.of(KEY, VALUE, false)), ProfileState.DISABLED));
    }

    public void testApplyPropertyEdit_aTreeRebuiltWhileTheDialogWasOpen_stillWritesTheEdit()
    {
        BuildContext context = BuildContext.getInstance(getProject());

        context.setProperties(List.of(BuildContextProperty.of(KEY, VALUE, false)));

        BuildContextPanel panel = new BuildContextPanel(getProject());

        panel.refresh();

        panel.applyPropertyEdit(KEY, BuildContextProperty.of(KEY, EDITED_VALUE, false));

        assertEquals(
            "an edit tied to the row the panel held before the dialog is written into a node the "
                + "refresh detached, so it never reaches the build context",
            EDITED_VALUE,
            context.getProperty(KEY));
    }

    public void testApplyPropertyEdit_aPropertyRemovedWhileTheDialogWasOpen_writesNothing()
    {
        BuildContext.getInstance(getProject()).setProperties(List.of());

        assertFalse(
            "an edit for a row that is gone would come back as a property the user never re-added",
            new BuildContextPanel(getProject())
                .applyPropertyEdit(KEY, BuildContextProperty.of(KEY, VALUE, false)));
    }

    public void testApplyPropertyEdit_aPropertyRemovedWhileTheDialogWasOpen_tellsTheUser()
    {
        List<Notification> reported = new ArrayList<>();

        getProject()
            .getMessageBus()
            .connect(getTestRootDisposable())
            .subscribe(
                Notifications.TOPIC,
                new Notifications()
                {
                    @Override
                    public void notify(Notification notification)
                    {
                        reported.add(notification);
                    }
                });

        BuildContext.getInstance(getProject()).setProperties(List.of());

        new BuildContextPanel(getProject()).applyPropertyEdit(KEY, BuildContextProperty.of(KEY, VALUE, false));

        assertFalse(
            "dropping the edit without a word is indistinguishable from the dialog doing nothing",
            reported.isEmpty());
    }

    private static String marker()
    {
        return MavenPrimeBundle.message("mavenprime.context.marker.import");
    }

    private String textOf(Object row)
    {
        return textRendererOf(row).getCharSequence(false).toString();
    }

    private SimpleColoredComponent textRendererOf(Object row)
    {
        return paint(row).getTextRenderer();
    }

    private ThreeStateCheckBox checkboxOf(Object row)
    {
        return paint(row).myCheckbox;
    }

    private CheckboxTreeBase.CheckboxTreeCellRendererBase paint(Object row)
    {
        BuildContextPanel panel = new BuildContextPanel(getProject());

        CheckboxTreeBase.CheckboxTreeCellRendererBase renderer =
            (CheckboxTreeBase.CheckboxTreeCellRendererBase) panel.tree.getCellRenderer();

        renderer.getTreeCellRendererComponent(
            panel.tree, new DefaultMutableTreeNode(row), false, true, true, 0, false);

        return renderer;
    }

    private static TreePath pathTo(Object row)
    {
        return new TreePath(new DefaultMutableTreeNode(row));
    }

    private static SimpleTextAttributes attributesOf(SimpleColoredComponent painted, String fragment)
    {
        SimpleColoredComponent.ColoredIterator fragments = painted.iterator();

        while (fragments.hasNext())
        {
            if (fragment.equals(fragments.next()))
            {
                return fragments.getTextAttributes();
            }
        }

        return null;
    }
}
