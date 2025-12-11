package com.crotaplague;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class NationalPopularityCache {

    private Party cachedTopParty = null;
    private Party cachedSecondParty = null;
    private int cachedPartyCompositionHash = 0;
    private int SAMPLE_SIZE = 5000;


    public void init() {
        int computed = Math.toIntExact(Math.round(Main.country.getCountyCount() * 0.17));
        SAMPLE_SIZE = Math.max(1000, computed);
    }

    private int computeCandidatePartyHash(List<Representative> candidates) {
        if (candidates == null) return 0;
        Map<String, Integer> counts = new HashMap<>();
        for (Representative r : candidates) {
            if (r == null || r.getParty() == null) continue;
            String name = r.getParty().getName().toLowerCase(Locale.ROOT);
            counts.merge(name, 1, Integer::sum);
        }
        List<String> keys = new ArrayList<>(counts.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            sb.append(k).append(':').append(counts.get(k)).append(';');
        }
        return sb.toString().hashCode();
    }

    // ---------------- NATIONAL PARTY SAMPLING ----------------

    private final AtomicBoolean computing = new AtomicBoolean(false);

    public void computeTopParties(List<Representative> candidates) {
        // Fast-path: if both are already computed, return immediately
        if (cachedTopParty != null && cachedSecondParty != null) return;

        // Attempt to become the single computing thread.
        // If another thread is already computing, wait until it finishes (busy-wait briefly).
        if (!computing.compareAndSet(false, true)) {
            // Another thread is computing. Wait for it to finish (simple spin/wait).
            // In a production system you'd prefer a proper wait/notify or a CountDownLatch.
            int waitLoops = 0;
            while (computing.get()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
                if (++waitLoops > 2000) break; // don't spin forever
            }
            // After waiting, cached values may be set by the other thread — return.
            if (cachedTopParty != null && cachedSecondParty != null) return;
            // If still not set, try to become the computing thread again.
            if (!computing.compareAndSet(false, true)) {
                // give up — either another thread will finish, or we'll try later when someone calls again
                return;
            }
        }

        // Now THIS thread is the designated computing thread.
        try {
            // Double-check the cache in case another thread already set it just before we acquired the lock.
            if (cachedTopParty != null && cachedSecondParty != null) return;

            // Defensive null/empty checks
            if (candidates == null || candidates.isEmpty()) {
                cachedTopParty = null;
                cachedSecondParty = null;
                return;
            }

            // Build canonical party map
            Map<String, Party> canonicalPartyByName = new HashMap<>();
            for (Representative r : candidates) {
                if (r == null || r.getParty() == null) continue;
                canonicalPartyByName.putIfAbsent(
                        r.getParty().getName().toLowerCase(Locale.ROOT),
                        r.getParty()
                );
            }
            if (canonicalPartyByName.isEmpty()) {
                cachedTopParty = null;
                cachedSecondParty = null;
                return;
            }

            Set<Party> allParties = new HashSet<>(canonicalPartyByName.values());
            List<Citizen> allCitizens = Main.country.getCitizens();
            if (allCitizens == null || allCitizens.isEmpty()) {
                cachedTopParty = null;
                cachedSecondParty = null;
                return;
            }

            // sample without replacement
            List<Citizen> pool = new ArrayList<>(allCitizens);
            Collections.shuffle(pool, ThreadLocalRandom.current());
            int sampleSize = Math.min(pool.size(), SAMPLE_SIZE);

            Map<String, Integer> rawCounts = new HashMap<>();
            for (int i = 0; i < sampleSize; i++) {
                Citizen c = pool.get(i);
                Party pref = VotingUtils.getTruePreferredParty(c, allParties);
                if (pref == null) continue;
                String pname = pref.getName().toLowerCase(Locale.ROOT);
                rawCounts.merge(pname, 1, Integer::sum);
            }

            String rawTop = null, rawSecond = null;
            int rawTopVotes = -1, rawSecondVotes = -1;
            for (Map.Entry<String, Integer> e : rawCounts.entrySet()) {
                int v = e.getValue();
                String n = e.getKey();
                if (v > rawTopVotes) {
                    rawSecond = rawTop; rawSecondVotes = rawTopVotes;
                    rawTop = n; rawTopVotes = v;
                } else if (v > rawSecondVotes) {
                    rawSecond = n; rawSecondVotes = v;
                }
            }

            if (rawTop == null) {
                // nothing to cache
                cachedTopParty = null;
                cachedSecondParty = null;
                return;
            }

            // Set cached parties (final assignment happens atomically by field writes)
            cachedTopParty = canonicalPartyByName.get(rawTop);
            cachedSecondParty = rawSecond == null
                    ? null
                    : canonicalPartyByName.get(rawSecond);

            // Diagnostic — print which thread computed and what it set
            System.err.printf(Locale.ROOT,
                    "computeTopParties computed by %s -> top=%s (opts=%d) second=%s (opts=%d)%n",
                    Thread.currentThread().getName(),
                    cachedTopParty == null ? "null" : cachedTopParty.getName(),
                    cachedTopParty == null ? 0 : cachedTopParty.options == null ? 0 : cachedTopParty.options.size(),
                    cachedSecondParty == null ? "null" : cachedSecondParty.getName(),
                    cachedSecondParty == null ? 0 : cachedSecondParty.options == null ? 0 : cachedSecondParty.options.size()
            );

        } finally {
            // Release the computing flag so other threads can proceed or see final result.
            computing.set(false);
        }
    }


    // ---------------- STRATEGIC NATIONAL FPTP ----------------

    /**
     * For each local voter:
     * 1) They select which of the two national top parties they prefer.
     * 2) If that preferred party has a local candidate, vote for best local candidate of that party.
     * 3) If not, use the district-level strategic winner (computed once via VotingUtils.runFPTPStrategic).
     * 4) If that fallback is null for safety, use honest ranking as final fallback.
     *
     * This makes the fallback strategic while still modeling preference for the national top party first,
     * and avoids calling the strategic aggregator per voter.
     */
    public Representative runFPTPNationalStrategic(
            List<Representative> candidates,
            List<Citizen> localVoters
    ) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (localVoters == null || localVoters.isEmpty()) return null;

        computeTopParties(candidates);

        if (cachedTopParty == null) return null;
        if (cachedSecondParty == null) return VotingUtils.runFPTP(candidates, localVoters);

        // group local candidates by lowercased party name
        Map<String, List<Representative>> localsByParty = new HashMap<>();
        for (Representative r : candidates) {
            if (r == null || r.getParty() == null) continue;
            String pname = r.getParty().getName().toLowerCase(Locale.ROOT);
            localsByParty.computeIfAbsent(pname, k -> new ArrayList<>()).add(r);
        }

        // compute district-level strategic fallback once
        Representative districtStrategicFallback = VotingUtils.runFPTPStrategic(candidates, localVoters);

        Map<Representative, Integer> counts = new HashMap<>();
        for (Representative r : candidates) counts.put(r, 0);

        for (Citizen voter : localVoters) {

            // Step 1: voter chooses between the two national parties
            Party preferred = VotingUtils.chooseBetweenTwoParties(voter, cachedTopParty, cachedSecondParty);

            Representative chosen = null;

            if (preferred != null) {
                // Step 2: try to find local candidate from the preferred top party
                List<Representative> partyLocals = localsByParty.get(preferred.getName().toLowerCase(Locale.ROOT));
                if (partyLocals != null && !partyLocals.isEmpty()) {
                    List<Representative> ranked = VotingUtils.rankCandidatesForVoter(voter, partyLocals);
                    if (!ranked.isEmpty()) chosen = ranked.get(0);
                }
            }

            // Step 3: if no local candidate for preferred top party, use district-level strategic winner
            if (chosen == null) {
                if (districtStrategicFallback != null) {
                    chosen = districtStrategicFallback;
                } else {
                    // final safety fallback: honest ranking
                    List<Representative> honestRanked = VotingUtils.rankCandidatesForVoter(voter, candidates);
                    if (honestRanked != null && !honestRanked.isEmpty()) chosen = honestRanked.get(0);
                }
            }

            if (chosen != null && counts.containsKey(chosen)) {
                counts.put(chosen, counts.get(chosen) + 1);
            }
        }

        Representative winner = null;
        int best = -1;
        for (Map.Entry<Representative, Integer> e : counts.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                winner = e.getKey();
            }
        }
        return winner;
    }
    public void reset() {
        cachedTopParty = null;
        cachedSecondParty = null;
    }

}
