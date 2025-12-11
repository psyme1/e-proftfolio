package com.crotaplague;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public final class ValueAssigner {
    private static final Random rand = new Random();
    private static String filename = "C:\\Users\\dsyme\\Downloads\\SocietySimulation\\SocietySimulation\\src\\PoliticalValues.txt";

    // Define issues with their weight (likelihood), salience (polarization bias), and alignment (average stance)
    private static final List<Issue> ISSUES = new ArrayList<>();

    public static void init(){
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue; // allow comments
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String name = parts[0].trim();
                    int weight = Integer.parseInt(parts[1].trim());
                    int salience = Integer.parseInt(parts[2].trim());
                    double alignment = Double.parseDouble(parts[3].trim());
                    ISSUES.add(new Issue(name, weight, salience, alignment));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void assignValuesToCitizen(Citizen citizen) {
        int count = randomSkewedCount();

        Set<String> picked = new HashSet<>();
        while (citizen.getValues().size() < count && picked.size() < ISSUES.size()) {
            Issue issue = pickWeightedIssue();
            if (picked.add(issue.name)) {
                int polarization = biasedPolarization(issue.salienceBias);
                int opinion = biasedOpinion(issue.alignment); // now aligned to average stance
                citizen.addValue(new Value(issue.name, polarization, opinion));
            }
        }
    }

    /** Weighted random issue pick */
    private static Issue pickWeightedIssue() {
        int totalWeight = ISSUES.stream().mapToInt(i -> i.weight).sum();
        int roll = rand.nextInt(totalWeight);
        int cumulative = 0;
        for (Issue issue : ISSUES) {
            cumulative += issue.weight;
            if (roll < cumulative) return issue;
        }
        throw new IllegalStateException("Weighted pick failed");
    }

    /** Polarization biased toward the middle, shifted by salience */
    private static int biasedPolarization(int salienceBias) {
        // Gaussian around center 5
        double base = rand.nextGaussian() * 1.5 + 2.5;
        double adjusted = (base * 0.65) + (salienceBias * 0.35);
        adjusted += rand.nextGaussian() * 0.3; // small nudge

        return Math.max(0, Math.min(10, (int) Math.round(adjusted)));
    }

    /** Opinion now centered around the issue's average alignment */
    private static int biasedOpinion(double alignment) {
        // Gaussian around the given alignment (scaled from 0–10 to -10–10)
        double mean = (alignment - 5.0) * 2.0; // shift to opinion scale
        double base = rand.nextGaussian() * 2 + mean; // ~2 stdev spread
        return Math.max(-10, Math.min(10, (int) Math.round(base)));
    }

    /** Geometric-like distribution: most citizens only get a few values */
    private static int randomSkewedCount() {
        double p = 0.96;
        int count = 0;
        while (rand.nextDouble() < p) {
            count++;
            p *= 0.95;
        }
        return count;
    }

    // Add these to ValueAssigner (after init() builds ISSUES)
    public static int getIssueCount() {
        return ISSUES.size();
    }

    public static Map<String, Integer> getIssueIndexMap() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < ISSUES.size(); i++) {
            map.put(ISSUES.get(i).name.toLowerCase(), i);
        }
        return map;
    }


    /** Internal helper */
    private static class Issue {
        final String name;
        final int weight;
        final int salienceBias;
        final double alignment; // new average stance 0–10

        Issue(String name, int weight, int salienceBias, double alignment) {
            this.name = name;
            this.weight = weight;
            this.salienceBias = salienceBias;
            this.alignment = alignment;
        }
    }
}
