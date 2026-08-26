package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.util.ui.ThreeStateCheckBox;
import com.intellij.util.ui.UIUtil;

public class ProfileListPanelPlatformTest
    extends BasePlatformTestCase
{
    private static final String PROFILE = "release";

    private static final String OTHER = "docs";

    private static final int MANY = 60;

    private static final String LONG_PROFILE = "a-profile-named-at-considerably-greater-length";

    public void testSetProfiles_aLongerNameArriving_asksForTheWidthItNeeds()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(PROFILE), Map.of());

        int narrow = panel.getPreferredSize().width;

        panel.setProfiles(List.of(PROFILE, LONG_PROFILE), Map.of());

        assertTrue(
            "a profile can bring more profiles with it, and a width fixed by the old list clips the "
                + "names the new one added: " + narrow + " vs " + panel.getPreferredSize().width,
            panel.getPreferredSize().width > narrow);
    }

    public void testSetProfiles_aShorterListArriving_asksForLessWidth()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(PROFILE, LONG_PROFILE), Map.of());

        int wide = panel.getPreferredSize().width;

        panel.setProfiles(List.of(PROFILE), Map.of());

        assertTrue(
            "a popup that only ever grows ends up mostly empty space",
            panel.getPreferredSize().width < wide);
    }

    public void testSetProfiles_moreProfilesThanFitOnScreen_stopsGrowingTaller()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(namesOf(MANY), Map.of());

        int capped = panel.getPreferredSize().height;

        panel.setProfiles(namesOf(MANY * 2), Map.of());

        assertEquals(
            "an uncapped height runs the popup off the screen instead of scrolling",
            capped,
            panel.getPreferredSize().height);
    }

    private static List<String> namesOf(int count)
    {
        List<String> names = new ArrayList<>(count);

        for (int index = 0; index < count; index++)
        {
            names.add(PROFILE + index);
        }

        return names;
    }

    public void testGetSelectedProfiles_aProfileNobodyHasTouched_leavesItOutSoMavenDecides()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(PROFILE), Map.of());

        assertEquals(
            "reporting an untouched profile puts a -P flag on the command line the user never asked for",
            Map.of(),
            panel.getSelectedProfiles());
    }

    public void testGetSelectedProfiles_aProfileForcedOff_reportsItRatherThanOmittingIt()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(PROFILE), Map.of(PROFILE, Boolean.FALSE));

        assertEquals(
            "omitting it loses the deactivation, so Maven would activate the profile by default",
            Map.of(PROFILE, Boolean.FALSE),
            panel.getSelectedProfiles());
    }

    public void testGetSelectedProfiles_aProfileTheReactorNeverDeclared_keepsTheStateItWasGiven()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(OTHER), Map.of(PROFILE, Boolean.TRUE));

        assertEquals(
            "dropping a profile the reactor no longer declares silently changes a saved goal",
            Map.of(PROFILE, Boolean.TRUE),
            panel.getSelectedProfiles());
    }

    public void testApply_theStatePickedFromTheDropdown_writesItToThatProfile()
    {
        ProfileEntry entry = new ProfileEntry(PROFILE, ProfileState.NOT_SET);

        new ProfileListPanel().apply(new TreePath(new DefaultMutableTreeNode(entry)), ProfileState.DISABLED);

        assertEquals(
            "a pick the panel drops leaves the checkbox showing a state the goal will not carry",
            ProfileState.DISABLED,
            entry.getState());
    }

    public void testSetProfiles_anyNumberOfProfiles_asksForExactlyThatManyRows()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(namedProfiles(7), Map.of());

        assertEquals(
            "a fixed row count leaves the popup asking for space no profile will ever fill",
            7,
            panel.tree.getVisibleRowCount());
    }

    public void testSetProfiles_reloadedWithFewerProfiles_shrinksBackInsteadOfKeepingTheOldHeight()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(namedProfiles(9), Map.of());
        panel.setProfiles(namedProfiles(2), Map.of());

        assertEquals(
            "keeping the previous count reopens the popup with the empty space of the bigger reactor",
            2,
            panel.tree.getVisibleRowCount());
    }

    public void testSetProfiles_aLongProfileName_widensTheListToFitIt()
    {
        assertTrue(
            "a width that ignores the text clips names at any font size or script",
            widthOf("a-really-quite-long-profile-name-indeed") > widthOf("a"));
    }

    public void testSetProfiles_aReactorDeclaringNoProfiles_stillAsksForARowToPaintTheEmptyTextIn()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(), Map.of());

        assertTrue(
            "a zero-row tree is zero pixels high, so the no-profiles text has nowhere to paint",
            panel.tree.getVisibleRowCount() > 0);
    }

    public void testSetProfiles_moreProfilesThanFitOnScreen_capsTheRowsItAsksFor()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(namedProfiles(MANY), Map.of());

        assertTrue(
            "an uncapped row count makes the tabbed pane ask for every profile at once, so the goal "
                + "dialog opens at full screen height whichever tab is showing",
            panel.tree.getVisibleRowCount() < MANY);
    }

    public void testSetProfiles_aReloadWhileTheUserHadARowSelected_keepsThatRowSelected()
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(PROFILE, OTHER), Map.of());
        panel.tree.setSelectionRow(rowOf(panel, PROFILE));

        panel.setProfiles(List.of(PROFILE, OTHER), Map.of(OTHER, Boolean.TRUE));

        assertEquals(
            "the popup wires this reload to the pick that caused it, so a selection dropped here ends "
                + "the keyboard flow after a single profile",
            PROFILE,
            selectedProfileNameOf(panel));
    }

    public void testCustomizeRenderer_aProfileRow_leavesTheCheckboxGutterTransparent()
    {
        assertFalse(
            "an opaque checkbox paints the panel background over the tree background, and over the "
                + "selection colour on the selected row",
            paint(new ProfileEntry(PROFILE, ProfileState.NOT_SET)).myCheckbox.isOpaque());
    }

    public void testGetHorizontalScrollBarPolicy_theProfilesList_neverScrollsSideways()
    {
        assertEquals(
            "no popup list in the IDE scrolls sideways, and the scrollbar steals height when it appears",
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
            scrollPaneOf(new ProfileListPanel()).getHorizontalScrollBarPolicy());
    }

    public void testApply_theStatePickedFromTheDropdown_tellsTheListenerWhichProfileChanged()
    {
        List<ProfileEntry> notified = new ArrayList<>();

        ProfileEntry entry = new ProfileEntry(PROFILE, ProfileState.NOT_SET);

        new ProfileListPanel(notified::add)
            .apply(new TreePath(new DefaultMutableTreeNode(entry)), ProfileState.ENABLED);

        assertEquals(
            "without the callback the toolbar popup mutates rows nobody reads back, so the pick is "
                + "lost the moment it closes",
            List.of(entry),
            notified);
    }

    public void testApply_aRowThatIsNotAProfile_tellsTheListenerNothing()
    {
        List<ProfileEntry> notified = new ArrayList<>();

        new ProfileListPanel(notified::add)
            .apply(new TreePath(new DefaultMutableTreeNode("not a profile")), ProfileState.ENABLED);

        assertEquals(
            "a callback fired for a row carrying no profile would write a state under a blank name",
            List.of(),
            notified);
    }

    public void testCustomizeRenderer_aProfileForcedOn_checksItsCheckbox()
    {
        assertSame(
            "the goal dialog has to mark a profile the same way the Build Context tab does",
            ThreeStateCheckBox.State.SELECTED,
            paint(new ProfileEntry(PROFILE, ProfileState.ENABLED)).myCheckbox.getState());
    }

    public void testCustomizeRenderer_aProfileRow_showsTheCheckbox()
    {
        assertTrue(
            "with nothing in the marker slot the user has no way to tell the row can be changed",
            paint(new ProfileEntry(PROFILE, ProfileState.NOT_SET)).myCheckbox.isVisible());
    }

    private static int widthOf(String profile)
    {
        ProfileListPanel panel = new ProfileListPanel();

        panel.setProfiles(List.of(profile), Map.of());

        return panel.tree.getPreferredSize().width;
    }

    private static int rowOf(ProfileListPanel panel, String profile)
    {
        for (int row = 0; row < panel.tree.getRowCount(); row++)
        {
            ProfileEntry entry = ProfileEntries.at(panel.tree.getPathForRow(row));

            if ((entry != null) && profile.equals(entry.getName()))
            {
                return row;
            }
        }

        throw new AssertionError("the list never showed " + profile + ", so there is nothing to select");
    }

    private static String selectedProfileNameOf(ProfileListPanel panel)
    {
        ProfileEntry entry = ProfileEntries.at(panel.tree.getSelectionPath());

        return (entry == null) ? null : entry.getName();
    }

    private static JScrollPane scrollPaneOf(ProfileListPanel panel)
    {
        JScrollPane scroll = UIUtil.findComponentOfType(panel, JScrollPane.class);

        assertNotNull("the list is no longer inside a scroll pane, so this test proves nothing", scroll);

        return scroll;
    }

    private static List<String> namedProfiles(int count)
    {
        List<String> profiles = new ArrayList<>(count);

        for (int index = 0; index < count; index++)
        {
            profiles.add("profile-" + index);
        }

        return profiles;
    }

    private static CheckboxTreeBase.CheckboxTreeCellRendererBase paint(ProfileEntry entry)
    {
        ProfileListPanel panel = new ProfileListPanel();

        CheckboxTreeBase.CheckboxTreeCellRendererBase renderer =
            (CheckboxTreeBase.CheckboxTreeCellRendererBase) panel.tree.getCellRenderer();

        renderer.getTreeCellRendererComponent(
            panel.tree, new DefaultMutableTreeNode(entry), false, true, true, 0, false);

        return renderer;
    }
}
