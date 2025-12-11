package com.crotaplague;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.text.DecimalFormat;

public class MyLineChartDisplay {

    /** Named XYSeries for fast lookup */
    private static final Map<String, XYSeries> seriesMap = new LinkedHashMap<>();

    /** Adds or overwrites an entire series */
    public static void addLine(String name, double[] xs, double[] ys) {
        if (xs.length != ys.length)
            throw new IllegalArgumentException("X and Y arrays must be the same length.");

        XYSeries s = new XYSeries(name);
        for (int i = 0; i < xs.length; i++) s.add(xs[i], ys[i]);
        seriesMap.put(name, s);
    }

    public static void addLine(String name, List<Point.Double> pts) {
        XYSeries s = new XYSeries(name);
        for (Point.Double p : pts) s.add(p.x, p.y);
        seriesMap.put(name, s);
    }

    public static void addLine(String name, List<Double> xs, List<Double> ys) {
        if (xs.size() != ys.size())
            throw new IllegalArgumentException("X and Y list sizes must match.");

        XYSeries s = new XYSeries(name);
        for (int i = 0; i < xs.size(); i++) s.add(xs.get(i), ys.get(i));
        seriesMap.put(name, s);
    }

    /** NEW — Add a single point to an existing or new series */
    public static void addPoint(String name, double x, double y) {
        XYSeries s = seriesMap.computeIfAbsent(name, XYSeries::new);
        s.add(x, y);
    }

    /** Clears all cached lines */
    public static void clear() {
        seriesMap.clear();
    }

    // ---------- DISPLAY ---------- //

    public static void show(String chartTitle) {
        if (seriesMap.isEmpty()) {
            System.out.println("No lines added.");
            return;
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries s : seriesMap.values()) dataset.addSeries(s);

        JFreeChart chart = ChartFactory.createXYLineChart(
                chartTitle,
                "X",
                "Y",
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Assign distinct colors for each series
        final List<Color> colors = generateDistinctColors(dataset.getSeriesCount());
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, colors.get(i));
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(i, false);
        }

        renderer.setDefaultToolTipGenerator(
                new org.jfree.chart.labels.StandardXYToolTipGenerator(
                        "{0}: x={1}, y={2}",
                        new DecimalFormat("0.###"),
                        new DecimalFormat("0.###")
                )
        );

        plot.setRenderer(renderer);

        if (chart.getLegend() != null) {
            chart.getLegend().setVisible(false);
            chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
        }

        JScrollPane scrollPane = ChartScroller.wrap(chart, new Dimension(1200, 800));
        scrollPane.setPreferredSize(new Dimension(1000, 650));
        SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(new Point(0, 0)));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox legendToggle = new JCheckBox("Show legend");
        legendToggle.addActionListener(e -> {
            if (chart.getLegend() != null) {
                chart.getLegend().setVisible(legendToggle.isSelected());
            }
        });
        controls.add(legendToggle);

        JLabel statusLabel = new JLabel("Selected: (none)");
        statusLabel.setForeground(Color.DARK_GRAY);
        controls.add(new JSeparator(SwingConstants.VERTICAL));
        controls.add(statusLabel);

        JFrame frame = new JFrame(chartTitle);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(controls, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Click highlight logic (unchanged)
        SwingUtilities.invokeLater(() -> {
            JViewport vp = scrollPane.getViewport();
            Component view = vp.getView();
            if (view instanceof com.crotaplague.EnhancedChartPanel) {
                com.crotaplague.EnhancedChartPanel panel = (com.crotaplague.EnhancedChartPanel) view;
                panel.setDisplayToolTips(true);

                final int seriesCount = dataset.getSeriesCount();
                final float normalStroke = 2.0f;
                final float highlightStroke = 3.5f;

                ChartMouseListener listener = new ChartMouseListener() {
                    Integer selectedSeries = null;

                    private void applyStyles() {
                        for (int i = 0; i < seriesCount; i++) {
                            boolean isSelected = selectedSeries != null && selectedSeries == i;
                            renderer.setSeriesStroke(i, new BasicStroke(isSelected ? highlightStroke : normalStroke));

                            Color base = colors.get(i);
                            Color painted = (selectedSeries == null || isSelected)
                                    ? base
                                    : new Color(base.getRed(), base.getGreen(), base.getBlue(), 140);
                            renderer.setSeriesPaint(i, painted);
                        }
                    }

                    @Override
                    public void chartMouseClicked(ChartMouseEvent event) {
                        ChartEntity ent = event.getEntity();
                        if (ent instanceof XYItemEntity) {
                            XYItemEntity xye = (XYItemEntity) ent;
                            int sIdx = xye.getSeriesIndex();
                            if (selectedSeries != null && selectedSeries == sIdx) {
                                selectedSeries = null;
                                statusLabel.setText("Selected: (none)");
                            } else {
                                selectedSeries = sIdx;
                                Comparable<?> key = dataset.getSeriesKey(sIdx);
                                statusLabel.setText("Selected: " + key);
                            }
                        } else {
                            selectedSeries = null;
                            statusLabel.setText("Selected: (none)");
                        }
                        applyStyles();
                    }

                    @Override
                    public void chartMouseMoved(ChartMouseEvent event) {}
                };

                panel.addChartMouseListener(listener);
            }
        });
    }


    // ---------- Color Utility ---------- //

    private static List<Color> generateDistinctColors(int n) {
        int count = Math.max(1, n);
        List<Color> list = new ArrayList<>(count);

        final float golden = 0.61803398875f;
        float hue = 0.0f;

        for (int i = 0; i < count; i++) {
            hue = (hue + golden) % 1.0f;
            float saturation = 0.70f;
            float brightness = 0.95f;

            if ((i % 5) == 0) {
                saturation = 0.80f;
            } else if ((i % 5) == 3) {
                brightness = 0.85f;
            }

            list.add(Color.getHSBColor(hue, saturation, brightness));
        }

        return list;
    }
}
