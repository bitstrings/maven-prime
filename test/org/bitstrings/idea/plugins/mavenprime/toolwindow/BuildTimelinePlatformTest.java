package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.JViewport;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;

import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.MojoTiming;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BuildTimelinePlatformTest
    extends BasePlatformTestCase
{
    private static final int MANY = 40;

    private static final String MODULE = "org.example:core";

    private static final String LONG_MODULE = "org.example:a-module-named-at-considerably-greater-length";

    private static final String GOAL = "compiler:compile";

    private static final String EXECUTION = "default-compile";

    private static final int WIDTH = 400;

    private static final int HEIGHT = 200;

    private static final int MODULE_COLUMN = 0;

    private static final int DURATION_COLUMN = 2;

    private static final int FIRST_GOAL_ROW = 1;

    private static final int WIDE_WIDTH = 1200;

    private static final int NARROW_WIDTH = 40;

    private static final int DRAGGED_WIDTH = 1400;

    private static final Font FIXED_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);

    private static final long LONG_DURATION_MILLIS = 1_000_000_000_000L;

    private static final int RESIZED_WIDTH = 640;

    public void testGetZoom_aFreshTimeline_showsTheWholeBuild()
    {
        assertEquals(BuildTimeline.FIT, new BuildTimeline().getZoom());
    }

    public void testZoomOut_alreadyShowingTheWholeBuild_doesNotShrinkFurther()
    {
        BuildTimeline timeline = new BuildTimeline();

        timeline.zoomOut();

        assertEquals(
            "zooming out past the whole build would leave dead space", BuildTimeline.FIT, timeline.getZoom());
    }

    public void testZoomIn_fromFit_stopsTrackingTheViewportSoItCanScroll()
    {
        BuildTimeline timeline = new BuildTimeline();

        timeline.zoomIn();

        assertTrue("zooming in must widen the timeline", timeline.getZoom() > BuildTimeline.FIT);
        assertFalse(
            "a zoomed timeline must scroll rather than be squeezed into the viewport",
            timeline.getScrollableTracksViewportWidth());
    }

    public void testZoomToFit_afterZoomingIn_returnsToTheWholeBuild()
    {
        BuildTimeline timeline = new BuildTimeline();

        timeline.zoomIn();
        timeline.zoomIn();
        timeline.zoomToFit();

        assertEquals(BuildTimeline.FIT, timeline.getZoom());
        assertTrue(timeline.getScrollableTracksViewportWidth());
    }

    public void testZoomIn_repeatedly_stopsAtABoundedMaximum()
    {
        BuildTimeline timeline = new BuildTimeline();

        for (int step = 0; step < MANY; step++)
        {
            timeline.zoomIn();
        }

        double atLimit = timeline.getZoom();

        timeline.zoomIn();

        assertEquals("zoom must be bounded, or the component width overflows", atLimit, timeline.getZoom());
    }

    public void testMousePressed_onABar_selectsThatModuleInTheTable()
    {
        BuildTimeline timeline = timelineShowingOneModule();
        ProfileTable table = tableShowingOneModule();

        timeline.setOnModuleChosen(table::select);

        timeline.dispatchEvent(mouseEvent(timeline, MouseEvent.MOUSE_PRESSED, pointOnTheBar(timeline)));

        assertEquals(
            "clicking a bar has to take you to the row it stands for",
            MODULE,
            selectedModuleOf(table));
    }

    public void testMouseMoved_ontoABar_reportsTheModuleItStandsFor()
    {
        BuildTimeline timeline = timelineShowingOneModule();

        List<String> pointedAt = new ArrayList<>();

        timeline.setOnModulePointedAt(pointedAt::add);

        timeline.dispatchEvent(mouseEvent(timeline, MouseEvent.MOUSE_MOVED, pointOnTheBar(timeline)));

        assertEquals(List.of(MODULE), pointedAt);
    }

    public void testMouseExited_afterPointingAtABar_reportsThatNothingIsPointedAt()
    {
        BuildTimeline timeline = timelineShowingOneModule();

        List<String> pointedAt = new ArrayList<>();

        timeline.dispatchEvent(mouseEvent(timeline, MouseEvent.MOUSE_MOVED, pointOnTheBar(timeline)));
        timeline.setOnModulePointedAt(pointedAt::add);
        timeline.dispatchEvent(mouseEvent(timeline, MouseEvent.MOUSE_EXITED, new Point(0, 0)));

        assertEquals(
            "leaving the timeline has to clear the highlight it put on the table",
            1,
            pointedAt.size());
        assertNull(pointedAt.get(0));
    }

    private static BuildTimeline timelineShowingOneModule()
    {
        BuildTimeline timeline = new BuildTimeline();

        timeline.setProfile(profileWithOneModule(), Set.of());
        timeline.setSize(WIDTH, HEIGHT);

        return timeline;
    }

    private static ProfileTable tableShowingOneModule()
    {
        ProfileTable table = new ProfileTable();

        BuildProfile profile = profileWithOneModule();

        table.setProfile(profile, profile.summarize());

        return table;
    }

    private static BuildProfile profileWithOneModule()
    {
        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming(MODULE, 0L, 100L));

        return profile;
    }

    private static String selectedModuleOf(ProfileTable table)
    {
        Object node = table.getTree().getSelectionPath().getLastPathComponent();

        return ((ModuleTiming) ((DefaultMutableTreeNode) node).getUserObject()).module();
    }

    private static Point pointOnTheBar(BuildTimeline timeline)
    {
        for (int y = 0; y < HEIGHT; y++)
        {
            for (int x = 0; x < WIDTH; x++)
            {
                Point point = new Point(x, y);

                if (timeline.getToolTipText(mouseEvent(timeline, MouseEvent.MOUSE_MOVED, point)) != null)
                {
                    return point;
                }
            }
        }

        throw new AssertionError("the fixture painted no bar, so there is nothing to point at");
    }

    private static MouseEvent mouseEvent(Component source, int id, Point point)
    {
        return new MouseEvent(source, id, 0L, 0, point.x, point.y, 1, false);
    }

    public void testSetProfile_aModuleWithALongDuration_sizesTheDurationColumnToTheCell()
    {
        int headerSized =
            tableShowingOneModule().getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth();

        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming(MODULE, 0L, LONG_DURATION_MILLIS));

        ProfileTable table = new ProfileTable();

        table.setProfile(profile, profile.summarize());

        assertTrue(
            "the duration column sized to its header and ignored the cell it has to fit: " + headerSized
                + " vs " + table.getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth(),
            table.getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth() > headerSized);
    }

    public void testSetProfile_aSecondBuildArriving_keepsTheWidthTheUserChose()
    {
        ProfileTable table = tableShowingOneModule();

        table.getColumnModel().getColumn(DURATION_COLUMN).setPreferredWidth(RESIZED_WIDTH);

        BuildProfile profile = profileWithOneModule();

        table.setProfile(profile, profile.summarize());

        assertEquals(
            "re-sizing the columns for every build throws away the width the user dragged them to",
            RESIZED_WIDTH,
            table.getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth());
    }

    public void testSetProfile_anyProfile_capsNoColumnWidth()
    {
        ProfileTable table = tableShowingOneModule();

        for (int column = 0; column < table.getColumnModel().getColumnCount(); column++)
        {
            assertEquals(
                "a maximum pinned to the content clamps the column, so its divider cannot be dragged right",
                Integer.MAX_VALUE,
                table.getColumnModel().getColumn(column).getMaxWidth());
        }
    }

    public void testSetProfile_aMojoInsideACollapsedModule_widensTheDurationColumnForItToo()
    {
        int moduleOnly =
            tableShowingOneModule().getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth();

        BuildProfile profile = profileWithOneModule();

        profile.accept(new MojoTiming(MODULE, "compile", "default-compile", 0L, LONG_DURATION_MILLIS));

        ProfileTable table = new ProfileTable();

        table.setProfile(profile, profile.summarize());

        assertTrue(
            "a width measured from the visible rows of a collapsed tree never sees a mojo, so every "
                + "goal duration is clipped once the module is expanded: " + moduleOnly + " vs "
                + table.getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth(),
            table.getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth() > moduleOnly);
    }

    public void testSetProfile_aFirstBuildArrivingBeforeAnyFont_sizesTheColumnsOnTheNextBuild()
    {
        BuildProfile profile = profileWithOneModule();

        ProfileTable late = new ProfileTable();

        late.setFont(null);
        late.setProfile(profile, profile.summarize());

        late.setFont(FIXED_FONT);
        late.setProfile(profile, profile.summarize());

        ProfileTable reference = new ProfileTable();

        reference.setFont(FIXED_FONT);
        reference.setProfile(profile, profile.summarize());

        assertEquals(
            "a latch spent before a font was resolvable leaves the timing columns at the JTable "
                + "default for the rest of the session",
            reference.getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth(),
            late.getColumnModel().getColumn(DURATION_COLUMN).getPreferredWidth());
    }

    public void testDoLayout_aViewportWiderThanTheColumns_givesTheSurplusToTheNameColumn()
    {
        ProfileTable table = laidOutIn(WIDE_WIDTH);

        int timings = 0;

        for (int column = MODULE_COLUMN + 1; column < table.getColumnModel().getColumnCount(); column++)
        {
            timings += table.getColumnModel().getColumn(column).getWidth();
        }

        assertEquals(
            "columns that stop short of the viewport leave a dead strip down the right of the tab",
            WIDE_WIDTH,
            table.getColumnModel().getColumn(MODULE_COLUMN).getWidth() + timings);
        assertTrue(
            "the surplus belongs to the names, not spread across timings that already fit their content",
            table.getColumnModel().getColumn(MODULE_COLUMN).getWidth() > (WIDE_WIDTH / 2));
    }

    public void testGetScrollableTracksViewportWidth_aViewportNarrowerThanTheNames_scrollsInstead()
    {
        assertFalse(
            "names squeezed below the width they need are unreadable, and no scrollbar offers them back",
            laidOutIn(NARROW_WIDTH).getScrollableTracksViewportWidth());
    }

    public void testGetScrollableTracksViewportWidth_aColumnBeingDragged_stopsTrackingTheViewport()
    {
        ProfileTable table = laidOutIn(WIDE_WIDTH);

        table.getTableHeader().setResizingColumn(table.getColumnModel().getColumn(DURATION_COLUMN));

        assertFalse(
            "a table still tracking its viewport hands the dragged column back the width it just lost",
            table.getScrollableTracksViewportWidth());
    }

    public void testDoLayout_theNameColumnDraggedPastTheViewport_keepsTheWidthItWasDraggedTo()
    {
        ProfileTable table = laidOutIn(WIDE_WIDTH);

        TableColumn name = table.getColumnModel().getColumn(MODULE_COLUMN);

        table.getTableHeader().setResizingColumn(name);
        name.setWidth(DRAGGED_WIDTH);
        layout(table);

        table.getTableHeader().setResizingColumn(null);
        layout(table);

        assertEquals(
            "a name column pulled back to the viewport on the next layout cannot be widened at all",
            DRAGGED_WIDTH,
            name.getWidth());
        assertFalse(
            "a name column wider than the viewport has to bring a scrollbar with it",
            table.getScrollableTracksViewportWidth());
    }

    private static ProfileTable laidOutIn(int width)
    {
        ProfileTable table = tableShowingOneModule();

        JViewport viewport = new JViewport();

        viewport.setView(table);
        viewport.setSize(width, HEIGHT);

        layout(table);

        return table;
    }

    private static void layout(ProfileTable table)
    {
        table.setSize(
            table.getScrollableTracksViewportWidth()
                ? table.getParent().getWidth()
                : table.getPreferredSize().width,
            HEIGHT);
        table.doLayout();
    }

    public void testSetProfile_aModuleWithALongName_sizesTheModuleColumnToFitIt()
    {
        int shortName =
            tableShowingOneModule().getColumnModel().getColumn(MODULE_COLUMN).getPreferredWidth();

        BuildProfile profile = new BuildProfile();

        profile.accept(new ModuleTiming(LONG_MODULE, 0L, 100L));

        ProfileTable table = new ProfileTable();

        table.setProfile(profile, profile.summarize());

        assertTrue(
            "a module column left at the JTable default clips every name in it: " + shortName + " vs "
                + table.getColumnModel().getColumn(MODULE_COLUMN).getPreferredWidth(),
            table.getColumnModel().getColumn(MODULE_COLUMN).getPreferredWidth() > shortName);
    }

    public void testSelect_aModuleRow_reportsThatModuleToTheTimeline()
    {
        ProfileTable table = tableShowingOneModule();

        List<String> selected = new ArrayList<>();

        table.setOnModuleSelected(selected::add);
        table.select(MODULE);

        assertEquals(List.of(MODULE), selected);
    }

    public void testSetSelectionRow_aGoalRow_reportsTheModuleThatOwnsIt()
    {
        BuildProfile profile = profileWithOneModule();

        profile.accept(new MojoTiming(MODULE, GOAL, EXECUTION, 0L, 100L));

        ProfileTable table = new ProfileTable();

        table.setProfile(profile, profile.summarize());
        table.getTree().expandRow(0);

        List<String> selected = new ArrayList<>();

        table.setOnModuleSelected(selected::add);
        table.getTree().setSelectionRow(FIRST_GOAL_ROW);

        assertEquals(
            "a goal belongs to one module, and that module is the bar the chart draws for it",
            List.of(MODULE),
            selected);
    }

    public void testSetSelectedModule_aModuleChosenInTheTable_marksItsBar()
    {
        BuildTimeline timeline = timelineShowingOneModule();

        int[] unmarked = pixelsOf(timeline);

        timeline.setSelectedModule(MODULE);

        assertFalse(
            "a row chosen in the table has to be findable in the chart beside it",
            Arrays.equals(unmarked, pixelsOf(timeline)));
    }

    private static int[] pixelsOf(BuildTimeline timeline)
    {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Graphics2D canvas = image.createGraphics();

        try
        {
            timeline.paint(canvas);
        }
        finally
        {
            canvas.dispose();
        }

        return image.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);
    }
}
