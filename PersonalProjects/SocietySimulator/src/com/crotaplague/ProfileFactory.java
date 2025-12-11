package com.crotaplague;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds ValueProfile objects for Citizens, Parties and Representatives.
 * Returns index maps and arrays for fast lookup.
 */
public final class ProfileFactory {

    public static class Profiles {
        public final ValueProfile[] voterProfiles; // index -> voter profile
        public final ValueProfile[] repProfiles;   // index -> rep personal profile
        public final ValueProfile[] repPartyProfiles; // index -> rep's party profile (may be reused)
        public final int[] repToPartyIndex; // repIndex -> partyIndex (index in partyProfiles)
        public final ValueProfile[] partyProfiles; // index -> party profile
        public final Map<Citizen, Integer> voterIndex; // identity map
        public final Map<Representative, Integer> repIndex;
        public final Map<Party, Integer> partyIndex;

        private Profiles(int vCount, int rCount, int pCount) {
            voterProfiles = new ValueProfile[vCount];
            repProfiles = new ValueProfile[rCount];
            repPartyProfiles = new ValueProfile[rCount];
            repToPartyIndex = new int[rCount];
            partyProfiles = new ValueProfile[pCount];
            voterIndex = new IdentityHashMap<>();
            repIndex = new IdentityHashMap<>();
            partyIndex = new IdentityHashMap<>();
        }
    }

    /**
     * Build profiles for all voters and representatives. Parties are discovered from reps.
     *
     * @param voters list of citizens
     * @param reps   list of representatives
     */
    public static Profiles buildProfiles(List<Citizen> voters, List<Representative> reps) {
        Map<String, Integer> indexMap = ValueAssigner.getIssueIndexMap();
        int issueCount = ValueAssigner.getIssueCount();

        // collect unique parties
        Map<Party, Integer> partyToIdx = new IdentityHashMap<>();
        List<Party> partyList = new ArrayList<>();
        for (Representative r : reps) {
            Party p = r == null ? null : r.getParty();
            if (p != null && !partyToIdx.containsKey(p)) {
                partyToIdx.put(p, partyList.size());
                partyList.add(p);
            }
        }

        Profiles profiles = new Profiles(
                voters == null ? 0 : voters.size(),
                reps == null ? 0 : reps.size(),
                partyList.size()
        );

        // build party profiles
        for (int i = 0; i < partyList.size(); i++) {
            Party p = partyList.get(i);
            ValueProfile profile = buildProfileFromValues(p.getValues(), indexMap, issueCount);
            profiles.partyProfiles[i] = profile;
            profiles.partyIndex.put(p, i);
        }

        // build rep profiles + map rep->party index
        for (int i = 0; i < reps.size(); i++) {
            Representative r = reps.get(i);
            profiles.repIndex.put(r, i);
            profiles.repProfiles[i] = buildProfileFromValues(
                    r.getCitizen() == null ? Collections.emptyList() : r.getCitizen().getValues(),
                    indexMap, issueCount
            );
            Party p = r.getParty();
            if (p == null) {
                profiles.repToPartyIndex[i] = -1;
                profiles.repPartyProfiles[i] = new ValueProfile(issueCount); // empty
            } else {
                Integer pi = profiles.partyIndex.get(p);
                profiles.repToPartyIndex[i] = pi == null ? -1 : pi;
                profiles.repPartyProfiles[i] = (pi == null) ? new ValueProfile(issueCount) : profiles.partyProfiles[pi];
            }
        }

        // build voter profiles
        for (int i = 0; i < voters.size(); i++) {
            Citizen c = voters.get(i);
            profiles.voterIndex.put(c, i);
            profiles.voterProfiles[i] = buildProfileFromValues(c.getValues(), indexMap, issueCount);
        }

        return profiles;
    }

    private static ValueProfile buildProfileFromValues(List<Value> values, Map<String, Integer> indexMap, int issueCount) {
        ValueProfile p = new ValueProfile(issueCount);
        if (values == null || values.isEmpty()) return p;

        for (Value v : values) {
            if (v == null) continue;
            Integer idx = indexMap.get(v.getName().toLowerCase());
            if (idx == null) continue;
            p.importance[idx] = v.getPolarization() / 10.0; // 0..1
            p.opinion[idx] = v.getOpinion(); // -10..10
        }
        return p;
    }
}
