package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.BasicStroke;
import java.awt.Color;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private static final double ZOOM_STEP = 1.25D;

    private static final double MAX_ZOOM = 64.0D;

    private static final int MINIMUM_WIDTH = 320;

    private static final int LANE_HEIGHT = 22;

    private static final int BAR_HEIGHT = 16;

    private static final int LEFT_GUTTER = 8;

    private static final int ARC = 6;

    private static final int MINIMUM_BAR_WIDTH = 2;

    private static final int POINTED_STROKE = 1;

    private static final int SELECTED_STROKE = 2;

    private static final int TICK_COUNT = 6;

    private static final JBColor BAR = JBColor.namedColor("MavenPrime.Timeline.bar", 0x6FA8DC, 0x3C6E9E);

    private static final JBColor CRITICAL_BAR =
        JBColor.namedColor("MavenPrime.Timeline.criticalBar", 0xE8A33D, 0xB57923);

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

    private Span spanAt(Point point)
    {
        int rulerHeight = rulerHeight();

        for (Span span : spans)
        {
            if (boundsOf(span, rulerHeight).contains(point))
            {
                return span;
            }
        }

        return null;
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
        double clamped = Math.min(MAX_ZOOM, Math.max(FIT, requested));

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

        for (ModuleTiming module : profile.getModules())
        {
            wallClockMillis = Math.max(wallClockMillis, module.endMillis());

            spans.add(
                new Span(
                    module,
                    lanes.getOrDefault(module.module(), Integer.valueOf(0)).intValue(),
                    criticalPath.contains(module.module())));
        }

        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(
            (int) (viewportWidth() * zoom),
            rulerHeight() + JBUI.scale((laneCount * LANE_HEIGHT) + LEFT_GUTTER));
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
        return JBUI.scale(LANE_HEIGHT);
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

            for (Span span : spans)
            {
                Rectangle bounds = boundsOf(span, rulerHeight);

                canvas.setColor(span.critical() ? CRITICAL_BAR : BAR);
                canvas.fillRoundRect(
                    bounds.x, bounds.y, bounds.width, bounds.height, JBUI.scale(ARC), JBUI.scale(ARC));

                paintOutline(canvas, span.module().module(), bounds);
            }
        }
        finally
        {
            canvas.dispose();
        }
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

    private void paintRuler(Graphics2D canvas)
    {
        Font rulerFont = rulerFont();

        canvas.setFont(rulerFont);

        FontMetrics metrics = canvas.getFontMetrics(rulerFont);

        Color tickColor = JBColor.border();
        Color labelColor = UIUtil.getContextHelpForeground();

        int usable = usableWidth();
        int baseline = metrics.getAscent();
        int labelGap = metrics.charWidth(' ');

        for (int tick = 0; tick <= TICK_COUNT; tick++)
        {
            int position = JBUI.scale(LEFT_GUTTER) + ((usable * tick) / TICK_COUNT);

            canvas.setColor(tickColor);
            canvas.drawLine(position, baseline, position, getHeight());

            canvas.setColor(labelColor);
            canvas.drawString(
                Formats.formatDuration((wallClockMillis * tick) / TICK_COUNT),
                position + labelGap,
                baseline);
        }
    }

    private Font rulerFont()
    {
        return UIUtil.getFont(UIUtil.FontSize.SMALL, getFont());
    }

    private int rulerHeight()
    {
        FontMetrics metrics = getFontMetrics(rulerFont());

        return metrics.getHeight() + metrics.getDescent();
    }

    private Rectangle boundsOf(Span span, int rulerHeight)
    {
        int usable = usableWidth();

        int x = JBUI.scale(LEFT_GUTTER) + scaled(span.module().startMillis(), usable);
        int width = Math.max(JBUI.scale(MINIMUM_BAR_WIDTH), scaled(span.module().durationMillis(), usable));

        return new Rectangle(
            x,
            rulerHeight + (span.lane() * JBUI.scale(LANE_HEIGHT)),
            width,
            JBUI.scale(BAR_HEIGHT));
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

    private record Span(ModuleTiming module, int lane, boolean critical)
    {
    }
}
