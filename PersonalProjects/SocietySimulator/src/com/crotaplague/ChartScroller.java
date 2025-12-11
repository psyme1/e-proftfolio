package com.crotaplague;

import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;

public final class ChartScroller {

    private ChartScroller() {}

    /**
     * Wrap a JFreeChart in an EnhancedChartPanel and JScrollPane so:
     * - mouse wheel -> scroll the JScrollPane by default (works for trackpads)
     * - Ctrl + mouse wheel -> zoom the chart
     *
     * Usage:
     * JScrollPane chartScroll = ChartScroller.wrap(chart, new Dimension(1200, 900));
     */
    public static JScrollPane wrap(JFreeChart chart, Dimension preferredSize) {
        EnhancedChartPanel chartPanel = new EnhancedChartPanel(chart);

        if (preferredSize != null) {
            chartPanel.setPreferredSize(preferredSize);
        }

        // Put the chartPanel inside a JScrollPane
        JScrollPane scroll = new JScrollPane(chartPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Use simple scroll mode for better repaint behavior during scrolling:
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        // Tweak scroll increments for smoother wheel/trackpad scrolling. You can tune these.
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        scroll.getHorizontalScrollBar().setUnitIncrement(8);

        // If you want extra smoothness on some platforms, you can enable the following:
        // scroll.setWheelScrollingEnabled(true); // default true

        return scroll;
    }
}
