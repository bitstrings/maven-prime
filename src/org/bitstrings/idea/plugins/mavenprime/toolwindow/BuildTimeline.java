package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BasicStroke;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.BuildProfile;
import org.bitstrings.idea.plugins.mavenprime.profile.ProfileEvent.ModuleTiming;

import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

public final class BuildTimeline
    extends JComponent
    implements Scrollable
{
    private static final long serialVersionUID = 1L;

    public static final double FIT = 1.0D;

    static final int NO_LABEL = -1;

    private static final double ZOOM_STEP = 1.25D;

    private static final double MAX_ZOOM = 64.0D;

    private static final int MINIMUM_WIDTH = 320;

    private static final int RULER_HEIGHT = 20;

    private static final int BAR_HEIGHT = 20;

    private static final int LANE_HEIGHT = 28;

    private static final int BAR_GAP = 1;

    private static final int BOTTOM_MARGIN = 8;

    private static final int BORDER_STROKE = 1;

    private static final int LABEL_PADDING = 4;

    private static final int LEFT_GUTTER = 8;

    private static final int ARC = 6;

    private static final int MINIMUM_BAR_WIDTH = 2;

    private static final int HIT_SLOP = 4;

    private static final int POINTED_STROKE = 1;

    private static final int SELECTED_STROKE = 2;

    private static final int TICK_COUNT = 6;

    private static final int NO_LANE = -1;

    private static final long NO_NEIGHBOUR = -1L;

    private static final JBColor BAR = JBColor.namedColor("MavenPrime.Timeline.bar", 0x6FA8DC, 0x3C6E9E);

    private static final JBColor CRITICAL_BAR =
        JBColor.namedColor("MavenPrime.Timeline.criticalBar", 0xE8A33D, 0xB57923);

    private static final JBColor BAR_BORDER =
        JBColor.namedColor("MavenPrime.Timeline.barBorder", 0x4C86B6, 0x27516F);

    private static final JBColor CRITICAL_BAR_BORDER =
        JBColor.namedColor("MavenPrime.Timeline.criticalBarBorder", 0xC4832B, 0x8C5C18);

    private static final JBColor BAR_TEXT =
        JBColor.namedColor("MavenPrime.Timeline.barText", 0x1F2E3D, 0xE8F0F7);

    private static final JBColor CRITICAL_BAR_TEXT =
        JBColor.namedColor("MavenPrime.Timeline.criticalBarText", 0x3A2705, 0xFFF3E0);

    private final transient List<Span> spans = new ArrayList<>();

    private transient long wallClockMillis;

    private transient int laneCount;

    private transient double zoom = FIT;

    private transient String pointedAt;

    private transient String selected;

    private transient Consumer<String> onModulePointedAt = module -> { };

    private transient Consumer<String> onModuleChosen = module -> { };

    public BuildTimeline()
    {
        setOpaque(true);

        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseWheelListener(this::wheelMoved);
        addMouseMotionListener(
            new MouseAdapter()
            {
                @Override
                public void mouseMoved(MouseEvent event)
                {
                    pointAt(moduleAt(event.getPoint()));
                }
            });
        addMouseListener(
            new MouseAdapter()
            {
                @Override
                public void mouseExited(MouseEvent event)
                {
                    pointAt(null);
                }

                @Override
                public void mousePressed(MouseEvent event)
                {
                    String module = moduleAt(event.getPoint());

                    if (module != null)
                    {
                        onModuleChosen.accept(module);
                    }
                }
            });
    }

    public void setOnModulePointedAt(Consumer<String> listener)
    {
        onModulePointedAt = listener;
    }

    public void setOnModuleChosen(Consumer<String> listener)
    {
        onModuleChosen = listener;
    }

    public void setSelectedModule(String module)
    {
        if (Objects.equals(selected, module))
        {
            return;
        }

        selected = module;

        repaint();

        Span span = spanOf(module);

        if (span != null)
        {
            scrollRectToVisible(boundsOf(span, rulerHeight()));
        }
    }

    private Span spanOf(String module)
    {
        for (Span span : spans)
        {
            if (span.module().module().equals(module))
            {
                return span;
            }
        }

        return null;
    }

    private void pointAt(String module)
    {
        if (Objects.equals(pointedAt, module))
        {
            return;
        }

        pointedAt = module;

        setCursor(Cursor.getPredefinedCursor((module == null) ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        repaint();

        onModulePointedAt.accept(module);
    }

    private String moduleAt(Point point)
    {
        Span span = spanAt(point);

        return (span == null) ? null : span.module().module();
    }

    // A bar clamped to its minimum width is a few pixels wide, so the whole lane row answers for it.
    private Span spanAt(Point point)
    {
        int lane = laneAt(point.y);

        if (lane < 0)
        {
            return null;
        }

        int rulerHeight = rulerHeight();
        int slop = JBUI.scale(HIT_SLOP);

        Span nearest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Span span : spans)
        {
            if (span.lane() != lane)
            {
                continue;
            }

            int distance = distanceTo(boundsOf(span, rulerHeight), point.x);

            if ((distance <= slop) && (distance < nearestDistance))
            {
                nearest = span;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private int laneAt(int y)
    {
        int rulerHeight = rulerHeight();

        if (y < rulerHeight)
        {
            return NO_LANE;
        }

        int lane = (y - rulerHeight) / laneHeight();

        return (lane < laneCount) ? lane : NO_LANE;
    }

    private static int distanceTo(Rectangle bounds, int x)
    {
        if (x < bounds.x)
        {
            return bounds.x - x;
        }

        return (x > (bounds.x + bounds.width)) ? (x - bounds.x - bounds.width) : 0;
    }

    public double getZoom()
    {
        return zoom;
    }

    public void zoomIn()
    {
        setZoom(zoom * ZOOM_STEP);
    }

    public void zoomOut()
    {
        setZoom(zoom / ZOOM_STEP);
    }

    public void zoomToFit()
    {
        setZoom(FIT);
    }

    private void setZoom(double requested)
    {
        double clamped = Math.clamp(requested, FIT, MAX_ZOOM);

        if (clamped == zoom)
        {
            return;
        }

        zoom = clamped;

        revalidate();
        repaint();
    }

    // A wheel listener swallows the scroll pane's own events, so non-zoom ones are handed back.
    private void wheelMoved(MouseWheelEvent event)
    {
        if (!event.isControlDown())
        {
            getParent().dispatchEvent(SwingUtilities.convertMouseEvent(this, event, getParent()));

            return;
        }

        setZoom((event.getWheelRotation() < 0) ? (zoom * ZOOM_STEP) : (zoom / ZOOM_STEP));
    }

    public void setProfile(BuildProfile profile, Set<String> criticalPath)
    {
        pointAt(null);
        setSelectedModule(null);

        spans.clear();

        Map<String, Integer> lanes = profile.getLaneAssignment();

        laneCount = Math.max(1, lanes.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1);
        wallClockMillis = 0L;

        Map<Integer, List<ModuleTiming>> byLane = new TreeMap<>();

        for (ModuleTiming module : profile.getModules())
        {
            wallClockMillis = Math.max(wallClockMillis, module.endMillis());

            byLane
                .computeIfAbsent(
                    lanes.getOrDefault(module.module(), Integer.valueOf(0)), lane -> new ArrayList<>())
                .add(module);
        }

        for (Map.Entry<Integer, List<ModuleTiming>> lane : byLane.entrySet())
        {
            addLane(lane.getKey().intValue(), lane.getValue(), criticalPath);
        }

        revalidate();
        repaint();
    }

    private void addLane(int lane, List<ModuleTiming> modules, Set<String> criticalPath)
    {
        modules.sort(Comparator.comparingLong(ModuleTiming::startMillis));

        for (int index = 0; index < modules.size(); index++)
        {
            ModuleTiming module = modules.get(index);

            spans.add(
                new Span(
                    module,
                    lane,
                    criticalPath.contains(module.module()),
                    ((index + 1) < modules.size()) ? modules.get(index + 1).startMillis() : NO_NEIGHBOUR));
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(
            (int) (viewportWidth() * zoom),
            rulerHeight() + (laneCount * laneHeight()) + JBUI.scale(BOTTOM_MARGIN));
    }

    private int viewportWidth()
    {
        Container parent = getParent();

        return (parent instanceof JViewport)
            ? Math.max(JBUI.scale(MINIMUM_WIDTH), parent.getWidth())
            : JBUI.scale(MINIMUM_WIDTH);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction)
    {
        return laneHeight();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction)
    {
        return (orientation == SwingConstants.HORIZONTAL) ? visible.width : visible.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth()
    {
        return zoom <= FIT;
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();

        try
        {
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(UIUtil.getPanelBackground());
            canvas.fillRect(0, 0, getWidth(), getHeight());

            paintRuler(canvas);

            int rulerHeight = rulerHeight();

            canvas.setFont(smallFont());

            FontMetrics metrics = canvas.getFontMetrics();

            for (Span span : spans)
            {
                Rectangle bounds = boundsOf(span, rulerHeight);

                paintBar(canvas, span, bounds);
                paintOutline(canvas, span.module().module(), bounds);
                paintDuration(canvas, span, bounds, metrics);
            }
        }
        finally
        {
            canvas.dispose();
        }
    }

    private void paintBar(Graphics2D canvas, Span span, Rectangle bounds)
    {
        int arc = JBUI.scale(ARC);

        canvas.setColor(span.critical() ? CRITICAL_BAR : BAR);
        canvas.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);

        canvas.setColor(span.critical() ? CRITICAL_BAR_BORDER : BAR_BORDER);
        canvas.setStroke(new BasicStroke(JBUI.scale(BORDER_STROKE)));
        canvas.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, arc, arc);
    }

    private void paintOutline(Graphics2D canvas, String module, Rectangle bounds)
    {
        boolean chosen = module.equals(selected);

        if (!chosen && !module.equals(pointedAt))
        {
            return;
        }

        canvas.setColor(JBColor.foreground());
        canvas.setStroke(new BasicStroke(JBUI.scale(chosen ? SELECTED_STROKE : POINTED_STROKE)));
        canvas.drawRoundRect(
            bounds.x, bounds.y, bounds.width, bounds.height, JBUI.scale(ARC), JBUI.scale(ARC));
    }

    private void paintDuration(Graphics2D canvas, Span span, Rectangle bounds, FontMetrics metrics)
    {
        if (metrics.getHeight() > bounds.height)
        {
            return;
        }

        String text = Formats.formatDuration(span.module().durationMillis());
        int textWidth = metrics.stringWidth(text);
        int x = durationLabelX(bounds, textWidth, JBUI.scale(LABEL_PADDING), roomAfter(span));

        if (x == NO_LABEL)
        {
            return;
        }

        canvas.setColor(
            (x < (bounds.x + bounds.width))
                ? (span.critical() ? CRITICAL_BAR_TEXT : BAR_TEXT)
                : UIUtil.getContextHelpForeground());
        canvas.drawString(
            text, x, bounds.y + ((bounds.height + metrics.getAscent() - metrics.getDescent()) / 2));
    }

    static int durationLabelX(Rectangle bar, int textWidth, int padding, int limit)
    {
        if ((textWidth + (2 * padding)) <= bar.width)
        {
            return bar.x + padding;
        }

        int outside = bar.x + bar.width + padding;

        return ((outside + textWidth) <= limit) ? outside : NO_LABEL;
    }

    private int roomAfter(Span span)
    {
        int gutter = JBUI.scale(LEFT_GUTTER);

        return (span.nextStartMillis() == NO_NEIGHBOUR)
            ? (gutter + usableWidth())
            : ((gutter + scaled(span.nextStartMillis(), usableWidth())) - JBUI.scale(LABEL_PADDING));
    }

    private void paintRuler(Graphics2D canvas)
    {
        canvas.setFont(smallFont());

        FontMetrics metrics = canvas.getFontMetrics();

        int usable = usableWidth();
        int gutter = JBUI.scale(LEFT_GUTTER);

        canvas.setColor(JBColor.border());

        for (int tick = 0; tick <= TICK_COUNT; tick++)
        {
            int position = gutter + ((usable * tick) / TICK_COUNT);

            canvas.drawLine(position, rulerHeight(), position, getHeight());
        }

        Graphics2D band = (Graphics2D) canvas.create();

        try
        {
            band.setColor(UIUtil.getContextHelpForeground());
            band.clipRect(0, 0, getWidth(), rulerHeight());

            int drawnTo = 0;

            for (int tick = 0; tick <= TICK_COUNT; tick++)
            {
                String label = Formats.formatDuration((wallClockMillis * tick) / TICK_COUNT);

                int width = metrics.stringWidth(label);
                int x = tickLabelX(gutter + ((usable * tick) / TICK_COUNT), width);

                if (x >= drawnTo)
                {
                    band.drawString(label, x, metrics.getAscent());

                    drawnTo = x + width + JBUI.scale(LABEL_PADDING);
                }
            }
        }
        finally
        {
            band.dispose();
        }
    }

    // The last tick sits at the right edge, so its label is hung to the left of the line instead.
    private int tickLabelX(int position, int textWidth)
    {
        int gap = JBUI.scale(LABEL_PADDING);

        return ((position + gap + textWidth) <= getWidth())
            ? (position + gap)
            : (position - gap - textWidth);
    }

    private Font smallFont()
    {
        return UIUtil.getFont(UIUtil.FontSize.SMALL, getFont());
    }

    private int rulerHeight()
    {
        return JBUI.scale(RULER_HEIGHT);
    }

    int barHeight()
    {
        return JBUI.scale(BAR_HEIGHT);
    }

    private int laneHeight()
    {
        return JBUI.scale(LANE_HEIGHT);
    }

    private Rectangle boundsOf(Span span, int rulerHeight)
    {
        int usable = usableWidth();

        int x = JBUI.scale(LEFT_GUTTER) + scaled(span.module().startMillis(), usable);
        int width =
            Math.max(
                JBUI.scale(MINIMUM_BAR_WIDTH),
                scaled(span.module().durationMillis(), usable) - JBUI.scale(BAR_GAP));

        return new Rectangle(x, rulerHeight + (span.lane() * laneHeight()), width, barHeight());
    }

    private int scaled(long millis, int usable)
    {
        return (wallClockMillis == 0L) ? 0 : (int) ((millis * usable) / wallClockMillis);
    }

    private int usableWidth()
    {
        return Math.max(1, getWidth() - (2 * JBUI.scale(LEFT_GUTTER)));
    }

    @Override
    public String getToolTipText(MouseEvent event)
    {
        Span span = spanAt(event.getPoint());

        return (span == null)
            ? null
            : MavenPrimeBundle.message(
                span.critical() ? "mavenprime.profile.tooltip.critical" : "mavenprime.profile.tooltip",
                span.module().module(),
                Formats.formatDuration(span.module().durationMillis()));
    }

    private record Span(ModuleTiming module, int lane, boolean critical, long nextStartMillis)
    {
    }
}
