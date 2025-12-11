package com.crotaplague.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads custom_citizens.json with strong validation + safety.
 *
 * Supports '#' comments (full-line + inline).
 */
public class CustomCitizensLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<CitizenSpec> load(File jsonFile) {
        List<CitizenSpec> result = new ArrayList<>();

        try {
            // Missing or empty file
            if (jsonFile == null || !jsonFile.exists()) {
                System.out.println("[CustomCitizens] File missing → no custom citizens.");
                return List.of();
            }

            String cleaned = stripHashComments(jsonFile);
            if (cleaned.trim().isEmpty()) {
                System.out.println("[CustomCitizens] File empty (after removing comments) → no custom citizens.");
                return List.of();
            }

            // Parse cleaned JSON text
            JsonNode root = MAPPER.readTree(cleaned);

            if (root == null || root.isNull()) {
                System.out.println("[CustomCitizens] JSON null → no custom citizens.");
                return List.of();
            }

            if (root.isArray()) {
                for (JsonNode node : root) {
                    CitizenSpec spec = validate(toSpec(node));
                    if (spec != null) result.add(spec);
                }
            } else if (root.isObject()) {
                CitizenSpec spec = validate(toSpec(root));
                if (spec != null) result.add(spec);
            } else {
                System.out.println("[CustomCitizens] Invalid JSON root (not array or object).");
            }

        } catch (Exception ex) {
            System.err.println("[CustomCitizens] Error loading file:");
            ex.printStackTrace();
        }

        return result;
    }

    /**
     * Converts a JsonNode to CitizenSpec using Jackson.
     */
    private static CitizenSpec toSpec(JsonNode node) {
        try {
            return MAPPER.convertValue(node, CitizenSpec.class);
        } catch (Exception ex) {
            System.err.println("[CustomCitizens] Invalid entry → skipped.");
            return null;
        }
    }

    /**
     * Removes '#' comments (full-line + inline).
     */
    private static String stripHashComments(File file) throws Exception {
        StringBuilder cleaned = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();

                // Skip full-line comments entirely
                if (trimmed.startsWith("#")) continue;

                // Remove inline comments
                int idx = trimmed.indexOf('#');
                if (idx >= 0) {
                    trimmed = trimmed.substring(0, idx).trim();
                }

                if (!trimmed.isEmpty()) {
                    cleaned.append(trimmed).append("\n");
                }
            }
        }

        return cleaned.toString();
    }

    /**
     * Fully validates + normalizes the citizen entry.
     */
    private static CitizenSpec validate(CitizenSpec spec) {
        if (spec == null) return null;

        if (spec.state != null && spec.state.isBlank()) spec.state = null;
        if (spec.county != null && spec.county.isBlank()) spec.county = null;

        if (spec.count == null) spec.count = 1;
        if (spec.count <= 0) {
            System.out.println("[CustomCitizens] Invalid count (" + spec.count + ") → skipping entry.");
            return null;
        }

        if (spec.age != null && (spec.age < 0 || spec.age > 120)) {
            System.out.println("[CustomCitizens] Invalid age (" + spec.age + ") → omitted.");
            spec.age = null;
        }

        if (spec.polarization != null &&
                (spec.polarization < 0 || spec.polarization > 100)) {
            System.out.println("[CustomCitizens] Invalid polarization (" + spec.polarization + ") → omitted.");
            spec.polarization = null;
        }

        if (spec.extremism != null &&
                (spec.extremism < 0.0 || spec.extremism > 1.0)) {
            System.out.println("[CustomCitizens] Invalid extremism (" + spec.extremism + ") → omitted.");
            spec.extremism = null;
        }

        if (spec.values != null) {
            spec.values.removeIf(v -> {
                boolean bad = (v.name == null || v.name.isBlank() ||
                        v.weight == null || v.weight < 0);
                if (bad) System.out.println("[CustomCitizens] Invalid value entry → removed.");
                return bad;
            });
            if (spec.values.isEmpty()) spec.values = null;
        }

        return spec;
    }

    // ---------------- DATA STRUCTURES -------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CitizenSpec {
        public Integer count = 1;
        public Integer age;
        public Integer polarization;
        public Double extremism;
        public List<ValueSpec> values;
        public String state;
        public String county;
        public boolean representative;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValueSpec {
        public String name;
        public Double weight;
    }
}
