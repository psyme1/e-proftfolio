package com.crotaplague.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Safe, optional settings loader.
 *
 * Every value is optional.
 * Missing file → defaults.
 * Blank fields → defaults.
 *
 * Supports overriding party list:
 * parties = Name|bias|comma,value,list ; Name2|bias2
 *
 * If provided, the override completely replaces the built-in list.
 */
public class SimulationSettings {

    // Toggles for file usage (optional)
    public boolean useCustomStates = false;
    public boolean useCustomCitizens = false;

    // All numeric settings default to -1 meaning "unspecified"
    public int citizenCount      = -1;
    public int representativeCount = -1;
    public int chamberSize       = -1;
    public int stateCount        = -1;
    public int partyCount        = -1;
    public int minVotingAge      = -1;
    public int maxVotingAge      = -1;

    // Party override list
    public final List<PartySpec> parties = new ArrayList<>();
    public boolean partiesOverride = false;

    public static class PartySpec {
        public final String name;
        public final int bias;
        public final List<String> values;

        public PartySpec(String name, int bias, List<String> values) {
            this.name = name;
            this.bias = bias;
            this.values = values == null ? new ArrayList<>() : values;
        }
    }

    // --- MAIN LOADER ------------------------------------------------------

    public static SimulationSettings load(File settingsFile) {
        SimulationSettings s = new SimulationSettings();

        if (settingsFile == null || !settingsFile.exists()) {
            System.out.println("[Settings] No settings file found → using defaults.");
            return s;
        }

        Properties p = new Properties();

        try (BufferedReader br = new BufferedReader(new FileReader(settingsFile))) {
            StringBuilder cleaned = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // Skip empty lines
                if (line.isEmpty()) continue;
                // Remove everything after a '#' for inline comments
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) line = line.substring(0, commentIndex).trim();
                // Skip lines that are now empty after removing comment
                if (line.isEmpty()) continue;

                cleaned.append(line).append(System.lineSeparator());
            }

            // Load the cleaned content into Properties
            p.load(new java.io.StringReader(cleaned.toString()));

            // --- basic settings (all optional) ---
            s.useCustomStates = parseBool(p.getProperty("useCustomStates"), s.useCustomStates);
            s.useCustomCitizens = parseBool(p.getProperty("useCustomCitizens"), s.useCustomCitizens);

            s.citizenCount        = parseIntDefault(p.getProperty("citizenCount"), s.citizenCount);
            s.representativeCount = parseIntDefault(p.getProperty("representativeCount"), s.representativeCount);
            s.chamberSize         = parseIntDefault(p.getProperty("chamberSize"), s.chamberSize);
            s.stateCount          = parseIntDefault(p.getProperty("stateCount"), s.stateCount);
            s.partyCount          = parseIntDefault(p.getProperty("partyCount"), s.partyCount);
            s.minVotingAge        = parseIntDefault(p.getProperty("minVotingAge"), s.minVotingAge);
            s.maxVotingAge        = parseIntDefault(p.getProperty("maxVotingAge"), s.maxVotingAge);

            // ---------------- PARTY OVERRIDE SECTION -----------------
            String raw = p.getProperty("parties");
            if (raw != null && !raw.trim().isEmpty()) {
                s.partiesOverride = true;
                loadPartyOverride(s, raw);
            }

        } catch (Exception ex) {
            System.err.println("[Settings] Error reading settings file:");
            ex.printStackTrace();
        }

        return s;
    }


    /**
     * Load settings from common expected paths.
     */
    public static SimulationSettings loadSettings() {
        String[] paths = {
                "config/settings.properties",
                "src/main/resources/settings.properties",
                "config/settings.cfg"
        };

        for (String p : paths) {
            File f = new File(p);
            if (f.exists()) return load(f);
        }

        return new SimulationSettings();
    }

    // --- HELPERS ---------------------------------------------------------

    private static boolean parseBool(String s, boolean def) {
        if (s == null) return def;
        s = s.trim().toLowerCase();
        if (s.isEmpty()) return def;

        return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("y");
    }

    private static int parseIntDefault(String s, int def) {
        if (s == null) return def;
        s = s.trim();
        if (s.isEmpty()) return def;
        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            System.out.println("[Settings] Invalid int \"" + s + "\" → using default (" + def + ")");
            return def;
        }
    }

    /**
     * Parses:
     *   parties=Name|bias|values;Name2|bias2|values2
     */
    private static void loadPartyOverride(SimulationSettings s, String raw) {

        String[] entries = raw.split(";");
        for (String entry : entries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            String[] parts = entry.split("\\|", 3);

            // --- Required: name ---
            String name = parts.length > 0 ? parts[0].trim() : "";
            if (name.isEmpty()) {
                System.out.println("[Settings] Party entry missing name → skipped.");
                continue;
            }

            // --- Required: bias ---
            int bias = 0;
            if (parts.length > 1) {
                try {
                    bias = Integer.parseInt(parts[1].trim());
                } catch (Exception ignored) {
                    System.out.println("[Settings] Invalid bias for party '" + name + "' → using 0.");
                    bias = 0;
                }
            }

            // --- Optional: values ---
            List<String> vals = new ArrayList<>();
            if (parts.length > 2) {
                String[] rawVals = parts[2].split(",");
                for (String v : rawVals) {
                    v = v.trim();
                    if (!v.isEmpty()) vals.add(v);
                }
            }

            s.parties.add(new PartySpec(name, bias, vals));
        }

        if (s.parties.isEmpty()) {
            System.out.println("[Settings] Party override provided but empty or invalid → ignoring.");
            s.partiesOverride = false;
        }
    }

}
