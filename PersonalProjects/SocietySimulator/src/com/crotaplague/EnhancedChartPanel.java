package com.crotaplague;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

/**
 * ChartPanel that plays nicely with JScrollPane:
 * - Implements Scrollable so it doesn't try to force-fit to the viewport.
 * - Exposes small unit/block increments for smoother trackpad scrolling.
 * - Keeps ChartPanel's ability to zoom with Ctrl+wheel (handled here).
 */
public class EnhancedChartPanel extends ChartPanel implements Scrollable {

    // tune this for smoothness: smaller = smoother but more sensitive
    private static final int UNIT_INCREMENT = 8;

    public EnhancedChartPanel(JFreeChart chart) {
        super(chart);
        // We want JScrollPane to receive wheel events by default:
        setMouseWheelEnabled(false);

        // allow drawing at large sizes so scrollbars can expose the full area:
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);
        setMinimumDrawWidth(0);
        setMinimumDrawHeight(0);

        // Optional: disable chart auto-resize so the chart area size is stable
        // (ChartPanel will still center/fit the plot within its drawing area).
        // setPreferredSize(...) should still be used when wrapping.

        // Ctrl + wheel -> zoom chart; otherwise leave wheel for JScrollPane.
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    Point p = e.getPoint();
                    Point2D java2D = translateScreenToJava2D(p);
                    if (e.getWheelRotation() < 0) {
                        zoomInBoth(java2D.getX(), java2D.getY());
                    } else {
                        zoomOutBoth(java2D.getX(), java2D.getY());
                    }
                    e.consume();
                }
                // If CTRL not down -> do nothing here so JScrollPane gets the event.
            }
        });
    }

    // ---------------- Scrollable implementation ----------------

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // micro scroll amount (units)
        return UNIT_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        // page scroll amount (blocks): roughly viewport size minus an overlap
        if (orientation == SwingConstants.VERTICAL) {
            return Math.max(UNIT_INCREMENT, visibleRect.height - UNIT_INCREMENT * 2);
        } else {
            return Math.max(UNIT_INCREMENT, visibleRect.width - UNIT_INCREMENT * 2);
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // false -> don't force the chart to match viewport width (allow horizontal scrolling)
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // false -> don't force the chart to match viewport height (allow vertical scrolling)
        return false;
    }
}
