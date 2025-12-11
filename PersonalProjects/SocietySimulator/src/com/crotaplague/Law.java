package com.crotaplague;

import java.time.Instant;
import java.util.*;

/**
 * Law / Policy model for the simulation.
 *
 * Rules enforced:
 *  - No temporary/expiring laws (laws persist until explicitly repealed).
 *  - Default threshold: simple majority (50% + 1 seat).
 *  - Governance-changing laws require 2/3 (rounded up).
 */
public final class Law implements Comparable<Law> {

    public enum Punishment { NONE, LOW, MEDIUM, HIGH }

    public enum LawType { CONSTITUTIONAL, STATUTORY, REGULATORY, ADMINISTRATIVE, DECLARATIVE }

    public enum Scope { NATIONAL, STATE, LOCAL }

    public enum Governance { NONE, REPUBLIC, MONARCHY, FEDERAL, UNITARY, FEUDAL, THEOCRACY, TECHNOCRACY, COMMUNISM, FASCISM, AUTHORITARIANISM, UNCHANGED }

    public static final class PolicyOption {
        private final String id;
        private final String label;
        private final String effect;
        private final int priorityBonus;

        public PolicyOption(String id, String label, String effect, int priorityBonus) {
            this.id = Objects.requireNonNull(id).toLowerCase(Locale.ROOT).trim();
            this.label = Objects.requireNonNull(label).trim();
            this.effect = effect == null ? "" : effect.trim();
            this.priorityBonus = priorityBonus;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getEffect() { return effect; }
        public int getPriorityBonus() { return priorityBonus; }

        @Override public String toString() {
            return label + (effect.isEmpty() ? "" : " (" + effect + ")");
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PolicyOption)) return false;
            return id.equals(((PolicyOption)o).id);
        }

        @Override public int hashCode() { return id.hashCode(); }
    }

    private final UUID id;
    private final String title;
    private final String key;
    private final String description;
    private final LawType type;
    private final Scope scope;
    private final Punishment punishment;
    private final Governance governanceChange;
    private final List<PolicyOption> options;
    private final Set<String> relatedValues;
    private final int agendaPriority;
    private final Instant createdAt;

    private Law(Builder b) {
        this.id = UUID.randomUUID();
        this.title = Objects.requireNonNull(b.title);
        this.key = normalize(b.title);
        this.description = b.description;
        this.type = b.type;
        this.scope = b.scope;
        this.punishment = b.punishment;
        this.governanceChange = b.governanceChange == null ? Governance.UNCHANGED : b.governanceChange;
        this.options = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(b.options)));
        this.relatedValues = Collections.unmodifiableSet(new HashSet<>(b.relatedValues));
        this.agendaPriority = b.agendaPriority;
        this.createdAt = Instant.now();
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", "_");
    }

    /* ---------------- public API ---------------- */

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getKey() { return key; }
    public String getDescription() { return description; }
    public LawType getType() { return type; }
    public Scope getScope() { return scope; }
    public Punishment getPunishment() { return punishment; }
    public Governance getGovernanceChange() { return governanceChange; }
    public List<PolicyOption> getOptions() { return options; }
    public Set<String> getRelatedValues() { return relatedValues; }
    public int getAgendaPriority() { return agendaPriority; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isGovernanceChange() {
        if(governanceChange == null) return false;
        if(governanceChange.equals(Governance.UNCHANGED)) {return false;}
        return true;
    }

    /**
     * Compute how many seats are required to pass this law given totalSeats in the chamber.
     * - If governance-changing -> ceil(2/3 * totalSeats)
     * - Otherwise -> simple majority = (totalSeats / 2) + 1 (integer math rounds down then +1)
     */
    public int requiredSeats(int totalSeats) {
        if (totalSeats <= 0) return Integer.MAX_VALUE; // impossible
        if (isGovernanceChange()) {
            // two-thirds, rounded up
            return (int) Math.ceil((2.0 / 3.0) * totalSeats);
        } else {
            // simple majority: floor(totalSeats/2) + 1, equivalent to (totalSeats / 2) + 1 for integer division
            return (totalSeats / 2) + 1;
        }
    }

    /**
     * Returns true if yesVotes is enough to pass given the total number of seats.
     * Use requiredSeats(totalSeats) as the ruleset described.
     */
    public boolean passesThreshold(long yesVotes, int totalSeats) {
        int needed = requiredSeats(totalSeats);
        return yesVotes >= needed;
    }

    @Override
    public String toString() {
        return String.format("Law[%s] type=%s scope=%s options=%d governance=%s reqSeatsFunc=%s punishment=%s",
                title, type, scope, options.size(), governanceChange, (isGovernanceChange() ? "2/3" : "majority"), punishment);
    }

    @Override
    public int compareTo(Law o) {
        int cmp = Integer.compare(o.agendaPriority, this.agendaPriority);
        if (cmp != 0) return cmp;
        cmp = this.type == LawType.CONSTITUTIONAL ? -1 : (o.type == LawType.CONSTITUTIONAL ? 1 : 0);
        if (cmp != 0) return cmp;
        return this.createdAt.compareTo(o.createdAt);
    }

    /* ---------------- Builder ---------------- */

    public static class Builder {
        private final String title;
        private String description = "";
        private LawType type = LawType.STATUTORY;
        private Scope scope = Scope.NATIONAL;
        private Punishment punishment = Punishment.NONE;
        private Governance governanceChange = Governance.UNCHANGED;
        private final List<PolicyOption> options = new ArrayList<>();
        private final Set<String> relatedValues = new HashSet<>();
        private int agendaPriority = 0;

        public Builder(String title) { this.title = Objects.requireNonNull(title); }

        public Builder description(String d) { this.description = d == null ? "" : d; return this; }
        public Builder type(LawType t) { this.type = t == null ? LawType.STATUTORY : t; return this; }
        public Builder scope(Scope s) { this.scope = s == null ? Scope.NATIONAL : s; return this; }
        public Builder punishment(Punishment p) { this.punishment = p == null ? Punishment.NONE : p; return this; }
        public Builder governanceChange(Governance g) { this.governanceChange = g == null ? Governance.UNCHANGED : g; return this; }
        public Builder addOption(PolicyOption opt) { this.options.add(opt); return this; }
        public Builder addOption(String id, String label, String effect, int priorityBonus) {
            return addOption(new PolicyOption(id, label, effect, priorityBonus));
        }
        public Builder addRelatedValue(String valueName) {
            if (valueName != null && !valueName.isBlank()) relatedValues.add(valueName.toLowerCase(Locale.ROOT).trim());
            return this;
        }
        public Builder agendaPriority(int p) { this.agendaPriority = p; return this; }

        public Law build() {
            if (this.options.isEmpty()) {
                // default binary yes/no if none provided
                this.addOption("yes", "Yes", "Enact law", 0);
                this.addOption("no", "No", "Reject law", 0);
            }
            return new Law(this);
        }
    }

    /**
     * Create a simple proposal (one desired option + implicit "no") from a Value.
     * - opinion (-10..10): negative -> restrict/ban, positive -> enact/allow
     * - polarization (0..10): affects agenda priority and option priority
     * - governance-related keywords still produce a constitutional "adopt" proposal
     */
    public static Law fromProposal(Value v) {
        Objects.requireNonNull(v, "Value required");

        String raw = v.getName() == null ? "" : v.getName().trim().toLowerCase(Locale.ROOT);
        String title = toTitleCase(raw.replace('_', ' '));

        // normalize input (Value may use -1 defaults)
        int pol = v.getPolarization();
        if (pol < 0 || pol > 10) pol = 5; // sensible default if unset
        int opinion = v.getOpinion();
        if (opinion < -10 || opinion > 10) opinion = 0; // fallback if unset

        // agenda priority: combine polarity (importance) and opinion magnitude
        int agenda = Math.max(1, Math.min(100, Math.abs(opinion) * 5 + pol * 5)); // 0..100

        Builder b = new Builder(title)
                .description("Proposal generated from value: " + raw)
                .agendaPriority(agenda)
                .addRelatedValue(raw);

        // Detect explicit governance-change values (these become constitutional proposals)
        if (raw.contains("monarch") || raw.contains("monarchy")) {
            b.type(LawType.CONSTITUTIONAL).governanceChange(Governance.MONARCHY);
        } else if (raw.contains("commun") || raw.contains("communism")) {
            b.type(LawType.CONSTITUTIONAL).governanceChange(Governance.COMMUNISM);
        } else if (raw.contains("fasc") || raw.contains("fascism")) {
            b.type(LawType.CONSTITUTIONAL).governanceChange(Governance.FASCISM);
        } else if (raw.contains("republic") || raw.contains("republicanism")) {
            b.type(LawType.CONSTITUTIONAL).governanceChange(Governance.REPUBLIC);
        } else if (raw.contains("theocr") || raw.contains("theocracy")) {
            b.type(LawType.CONSTITUTIONAL).governanceChange(Governance.THEOCRACY);
        } else if (raw.contains("feud") || raw.contains("feudal")) {
            b.type(LawType.CONSTITUTIONAL).governanceChange(Governance.FEUDAL);
        } else if (raw.contains("technocr") || raw.contains("technocracy")) {
            b.type(LawType.CONSTITUTIONAL).governanceChange(Governance.TECHNOCRACY);
        }else if (raw.contains("authoritarianism")){
            b.type(LawType.CONSTITUTIONAL).governanceChange(Governance.AUTHORITARIANISM);
        }

        // Decide proposer's preferred single action based on opinion:
        // positive -> enact/allow; negative -> restrict/ban; magnitude tunes intensity.
        String optId;
        String optLabel;
        String optEffect;
        int optPriority = Math.max(1, pol); // 1..10 (you can use this elsewhere)

        if (isGovernanceKeyword(raw)) {
            // governance proposal: proposer asks to "adopt" the governance change
            optId = "adopt";
            optLabel = "Adopt " + title;
            optEffect = "Change government to: " + title;
            // governance changes have no punishment setting here
            b.punishment(Law.Punishment.NONE);
        } else {
            // non-governance policy: map opinion to action/punishment
            if (opinion >= 6) {
                optId = "enact";
                optLabel = "Enact " + title;
                optEffect = "Full enactment / allow: " + title;
                b.punishment(Law.Punishment.NONE);
            } else if (opinion >= 1) {
                optId = "implement_moderate";
                optLabel = "Moderately implement " + title;
                optEffect = "Partial or regulated enactment of: " + title;
                b.punishment(Law.Punishment.LOW);
            } else if (opinion == 0) {
                optId = "statusquo_or_moderate";
                optLabel = "Maintain status quo / moderate action on " + title;
                optEffect = "No major change or modest adjustment for: " + title;
                b.punishment(Law.Punishment.NONE);
            } else if (opinion <= -6) {
                optId = "ban";
                optLabel = "Ban / Strongly restrict " + title;
                optEffect = "Strong restriction or ban of: " + title;
                b.punishment(Law.Punishment.HIGH);
            } else { // opinion between -5 and -1
                optId = "restrict";
                optLabel = "Restrict " + title;
                optEffect = "Limit or regulate: " + title;
                b.punishment(Law.Punishment.MEDIUM);
            }
        }

        // Add the single proposer option, and also add an explicit "no" reject option so ballots are binary.
        b.addOption(optId, optLabel, optEffect, optPriority)
                .addOption("no", "Reject", "Do not adopt the proposed change", 0);

        return b.build();
    }

    // small helper used above
    private static boolean isGovernanceKeyword(String rawLower) {
        return rawLower.contains("monarch") || rawLower.contains("monarchy") ||
                rawLower.contains("commun") || rawLower.contains("communism") ||
                rawLower.contains("fasc") || rawLower.contains("fascism") ||
                rawLower.contains("republic") || rawLower.contains("republicanism") ||
                rawLower.contains("theocr") || rawLower.contains("theocracy") ||
                rawLower.contains("feud") || rawLower.contains("feudal") ||
                rawLower.contains("technocr") || rawLower.contains("authoritarianism");
    }

    // reuse the toTitleCase helper you already have (or add this if not present)
    private static String toTitleCase(String s) {
        if (s == null || s.isBlank()) return "";
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

}

