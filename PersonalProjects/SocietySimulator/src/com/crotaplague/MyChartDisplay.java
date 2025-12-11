package com.crotaplague;

import org.jfree.chart.ChartFactory;
// import org.jfree.chart.ChartPanel; // replaced by EnhancedChartPanel/ChartScroller for scroll-friendly behavior
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyChartDisplay {

    // vampy's creampie
    public static void myPieDisplay(List<Representative> reps){
        myPieDisplay(reps, "Party Distribution");
    }

    public static void myPieDisplay(List<Representative> reps, String name) {
        // make a defensive copy
        reps = new ArrayList<>(reps);
        Map<String, List<Representative>> partyMap = new HashMap<>();
        for (Representative r : reps) {
            partyMap.computeIfAbsent(r.getParty().getName(), k -> new ArrayList<>()).add(r);
        }

        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, List<Representative>> entry : partyMap.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue().size());
        }

        JFreeChart chart = ChartFactory.createPieChart(
                name,
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        // Reduce clutter: hide section labels by default so the pie stays readable
        plot.setLabelGenerator(null);
        for (Map.Entry<String, List<Representative>> entry : partyMap.entrySet()) {
            String party = entry.getKey();
            List<Representative> partyReps = entry.getValue();

            double avgBias = partyReps.stream().mapToInt(Representative::getBias).average().orElse(50.0);
            Color partyColor = getRedBlueGradient((int) avgBias);

            plot.setSectionPaint(party, partyColor);
        }

        // Make legend collapsible: start hidden so the pie is visible immediately
        if (chart.getLegend() != null) {
            chart.getLegend().setVisible(false);
            chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
        }

        // Use EnhancedChartPanel + JScrollPane wrapper for proper scrolling
        JScrollPane scrollPane = ChartScroller.wrap(chart, new Dimension(1000, 800));
        scrollPane.setPreferredSize(new Dimension(900, 600));
        // Ensure viewport starts at the top-left
        SwingUtilities.invokeLater(() -> {
            scrollPane.getViewport().setViewPosition(new Point(0, 0));
            scrollPane.getVerticalScrollBar().setValue(0);
            scrollPane.getHorizontalScrollBar().setValue(0);
        });

        // Controls: toggle legend visibility
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox showLegend = new JCheckBox("Show legend");
        showLegend.addActionListener(e -> {
            if (chart.getLegend() != null) {
                chart.getLegend().setVisible(showLegend.isSelected());
            }
        });
        controls.add(showLegend);

        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(controls, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    public static Color getRedBlueGradient(int value) {
        value = Math.max(0, Math.min(100, value));

        if (value <= 50) {
            double t = value / 50.0;
            int r = (int) (255 - (127 * t));
            int g = 0;
            int b = (int) (0 + (128 * t));
            return new Color(r, g, b);
        } else {
            double t = (value - 50) / 50.0;
            int r = (int) (128 - (128 * t));
            int g = 0;
            int b = (int) (128 + (127 * t));
            return new Color(r, g, b);
        }
    }

    // --------------------- Multi-Chart Support ---------------------

    private static class ChartData {
        String name;
        List<Representative> reps;
        Map<String, Double> supportMap;

        ChartData(String name, List<Representative> reps) {
            this.name = name;
            this.reps = new ArrayList<>(reps);
            this.supportMap = null;
        }

        ChartData(String name, Map<String, Double> supportMap) {
            this.name = name;
            this.reps = null;
            this.supportMap = new HashMap<>(supportMap);
        }

        boolean isSupportChart() {
            return supportMap != null;
        }
    }


    private static final List<ChartData> multiCharts = new ArrayList<>();

    /** Add a chart to the multi-chart display */
    public static void addChart(String name, List<Representative> reps) {
        multiCharts.add(new ChartData(name, reps));
    }

    public static void addChart(String name, Map<String, Double> partySupport) {
        multiCharts.add(new ChartData(name, partySupport));
    }


    /** Display all charts with Previous/Next navigation */
    public static void showCharts() {
        if (multiCharts.isEmpty()) return;

        JFrame frame = new JFrame("Party Pie Charts");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel chartPanelContainer = new JPanel(new CardLayout());
        final int[] index = {0};

        // Keep references so we can toggle legend on the active chart and adjust scroll position
        Map<String, JFreeChart> chartByName = new HashMap<>();
        Map<String, JScrollPane> scrollByName = new HashMap<>();

        // create each chart, put it in a ChartPanel -> JScrollPane -> add as a card
        for (ChartData data : multiCharts) {
            DefaultPieDataset dataset = new DefaultPieDataset();
            JFreeChart chart;
            PiePlot plot;

            if (!data.isSupportChart()) {
                Map<String, List<Representative>> partyMap = new HashMap<>();
                for (Representative r : data.reps) {
                    partyMap.computeIfAbsent(r.getParty().getName(), k -> new ArrayList<>()).add(r);
                }

                for (Map.Entry<String, List<Representative>> entry : partyMap.entrySet()) {
                    dataset.setValue(entry.getKey(), entry.getValue().size());
                }

                chart = ChartFactory.createPieChart(
                        data.name,
                        dataset,
                        true,
                        true,
                        false
                );

                plot = (PiePlot) chart.getPlot();
                // Reduce clutter: hide section labels by default
                plot.setLabelGenerator(null);

                for (Map.Entry<String, List<Representative>> entry : partyMap.entrySet()) {
                    double avgBias = entry.getValue().stream()
                            .mapToInt(Representative::getBias)
                            .average()
                            .orElse(50.0);

                    Color partyColor = getRedBlueGradient((int) avgBias);
                    plot.setSectionPaint(entry.getKey(), partyColor);
                }

            } else {
                for (Map.Entry<String, Double> entry : data.supportMap.entrySet()) {
                    dataset.setValue(entry.getKey(), entry.getValue());
                }

                chart = ChartFactory.createPieChart(
                        data.name,
                        dataset,
                        true,
                        true,
                        false
                );

                plot = (PiePlot) chart.getPlot();
                // Reduce clutter: hide section labels by default
                plot.setLabelGenerator(null);
                for (String party : data.supportMap.keySet()) {
                    try {
                        // best-effort coloring — if Main or Party are present in your project this will use them,
                        // otherwise the try/catch prevents crashes.
                        Party p = Main.country.getPartiesMap().get(party.toLowerCase());
                        double avgBias = p.options.stream()
                                .mapToInt(Representative::getBias)
                                .average()
                                .orElse(50.0);
                        plot.setSectionPaint(party, getRedBlueGradient((int) avgBias));
                    } catch (Exception e) {
                        System.out.println("This was the name that caused issues: \"" + party + "\"");
                        e.printStackTrace();
                    }
                }
            }

            // Start with legend hidden; user can toggle it on if desired
            if (chart.getLegend() != null) {
                chart.getLegend().setVisible(false);
                chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
            }

            JScrollPane scrollPane = ChartScroller.wrap(chart, new Dimension(1000, 800));
            scrollPane.setPreferredSize(new Dimension(900, 600));
            // ensure initial viewport shows top-left
            SwingUtilities.invokeLater(() -> {
                scrollPane.getViewport().setViewPosition(new Point(0, 0));
                scrollPane.getVerticalScrollBar().setValue(0);
                scrollPane.getHorizontalScrollBar().setValue(0);
            });

            // add as card using the chart name as the card key
            chartPanelContainer.add(scrollPane, data.name);
            chartByName.put(data.name, chart);
            scrollByName.put(data.name, scrollPane);
        }


        // Controls + Navigation
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox showLegend = new JCheckBox("Show legend");
        showLegend.addActionListener(e -> {
            String current = multiCharts.get(index[0]).name;
            JFreeChart currentChart = chartByName.get(current);
            if (currentChart != null && currentChart.getLegend() != null) {
                currentChart.getLegend().setVisible(showLegend.isSelected());
            }
        });
        JButton prev = new JButton("< Previous");
        JButton next = new JButton("Next >");
        buttons.add(showLegend);
        buttons.add(prev);
        buttons.add(next);
        frame.add(chartPanelContainer, BorderLayout.CENTER);
        frame.add(buttons, BorderLayout.SOUTH);

        CardLayout cl = (CardLayout) chartPanelContainer.getLayout();

        // Show the first card (by name) and ensure scroll at top
        cl.show(chartPanelContainer, multiCharts.get(index[0]).name);
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = scrollByName.get(multiCharts.get(index[0]).name);
            if (sp != null) {
                sp.getViewport().setViewPosition(new Point(0, 0));
            }
        });

        prev.addActionListener(e -> {
            index[0] = (index[0] - 1 + multiCharts.size()) % multiCharts.size();
            cl.show(chartPanelContainer, multiCharts.get(index[0]).name);
            SwingUtilities.invokeLater(() -> {
                JScrollPane sp = scrollByName.get(multiCharts.get(index[0]).name);
                if (sp != null) sp.getViewport().setViewPosition(new Point(0, 0));
            });
        });

        next.addActionListener(e -> {
            index[0] = (index[0] + 1) % multiCharts.size();
            cl.show(chartPanelContainer, multiCharts.get(index[0]).name);
            SwingUtilities.invokeLater(() -> {
                JScrollPane sp = scrollByName.get(multiCharts.get(index[0]).name);
                if (sp != null) sp.getViewport().setViewPosition(new Point(0, 0));
            });
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
