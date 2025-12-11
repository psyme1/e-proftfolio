package com.crotaplague.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomStatesLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<StateSpec> load(File jsonFile) {
        List<StateSpec> result = new ArrayList<>();

        try {
            if (jsonFile == null || !jsonFile.exists()) {
                System.out.println("[States] No custom states file found: using defaults.");
                return List.of();
            }

            if (jsonFile.length() == 0L) {
                System.out.println("[States] custom_states.json is empty: using defaults.");
                return List.of();
            }

            // Read + strip '#' comments
            String cleanedJson = stripHashComments(jsonFile);

            // CASE 1: Try array
            try {
                List<StateSpec> arr = MAPPER.readValue(cleanedJson, new TypeReference<List<StateSpec>>() {});
                if (arr != null) {
                    validateStates(arr);
                    return arr;
                }
            } catch (JsonMappingException ignored) {}

            // CASE 2: Try single object
            try {
                StateSpec single = MAPPER.readValue(cleanedJson, StateSpec.class);
                if (single != null) {
                    validateStates(Collections.singletonList(single));
                    return List.of(single);
                }
            } catch (Exception ex) {
                System.err.println("[States] Failed reading single JSON object: " + ex.getMessage());
            }

        } catch (Exception ex) {
            System.err.println("[States] Failed to load custom_states.json: " + ex.getMessage());
        }

        return List.of();
    }

    /**
     * Removes all '#' comments (full-line + inline) and returns cleaned JSON.
     */
    private static String stripHashComments(File file) throws Exception {
        StringBuilder cleaned = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {

                String trimmed = line.trim();

                // Skip full-line comment
                if (trimmed.startsWith("#")) continue;

                // Remove inline comment
                int hashIndex = trimmed.indexOf('#');
                if (hashIndex >= 0) {
                    trimmed = trimmed.substring(0, hashIndex).trim();
                }

                if (!trimmed.isEmpty()) {
                    cleaned.append(trimmed).append("\n");
                }
            }
        }
        return cleaned.toString();
    }

    // ---------------- VALIDATION -------------------

    private static void validateStates(List<StateSpec> list) {
        list.removeIf(spec -> {

            if (spec.name == null || spec.name.isBlank()) {
                System.err.println("[States] Skipping state with no name.");
                return true;
            }

            if (spec.counties != null && !spec.counties.isEmpty()) {
                spec.counties.removeIf(c -> c == null || c.isBlank());
                if (spec.counties.isEmpty()) spec.counties = null;
            }

            if ((spec.counties == null || spec.counties.isEmpty()) && spec.countyCount != null) {
                if (spec.countyCount < 0) {
                    System.err.println("[States] Negative countyCount ignored for state: " + spec.name);
                    spec.countyCount = null;
                } else if (spec.countyCount > 0) {
                    spec.counties = new ArrayList<>();
                    for (int i = 0; i < spec.countyCount; i++) {
                        spec.counties.add(String.valueOf(i));
                    }
                }
            }

            if (spec.counties == null || spec.counties.isEmpty()) {
                spec.counties = List.of("0");
            }

            return false;
        });
    }

    // ---------------- DATA STRUCTURE -------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StateSpec {
        public String name;
        public List<String> counties;
        public Integer countyCount;
    }
}
