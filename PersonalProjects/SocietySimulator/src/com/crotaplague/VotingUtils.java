package com.crotaplague;

import com.crotaplague.Ballots.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class VotingUtils {

    private static Random rand = new Random(System.currentTimeMillis());

    /**
     * Rank blockCandidates for a given voter taking into account bias and values.
     * Returns a new sorted list (best first).
     */
    public static List<Representative> rankCandidatesForVoter(Citizen voter, List<Representative> blockCandidates) {
        if (voter == null || blockCandidates == null) return Collections.emptyList();

        final double BIAS_WEIGHT = 0.60;    // how much raw bias similarity matters
        final double VALUES_WEIGHT = 0.40;  // how much values/policy matter overall
        final double PARTY_VS_PERSONAL = 0.70; // within values: party vs personal candidate

        double voterBias = voter.getBias();

        List<Value> voterValues = voter.getValues() == null ? Collections.emptyList() : voter.getValues();

        // score each candidate
        Map<Representative, Double> scoreMap = new HashMap<>(blockCandidates.size());
        for (Representative rep : blockCandidates) {
            // bias similarity (0..1)
            double biasSim = 1.0 - (Math.abs(voterBias - rep.getBias()) / 100.0);
            biasSim = clamp01(biasSim);

            // party alignment (0..1)
            double partyAlignment = 0.0;
            if (rep.getParty() != null) {
                partyAlignment = computeAlignmentScore(voterValues, rep.getParty().getValues());
            }

            // personal (candidate) alignment (0..1)
            double personalAlignment = computeAlignmentScore(voterValues, rep.getCitizen() == null ? Collections.emptyList() : rep.getCitizen().getValues());

            // combine party/personal into a single values score
            double combinedValues = (partyAlignment * PARTY_VS_PERSONAL) + (personalAlignment * (1.0 - PARTY_VS_PERSONAL));
            combinedValues = clamp01(combinedValues);

            // values multiplier: values matter more when bias is close
            double normBiasDist = Math.abs(voterBias - rep.getBias()) / 100.0; // 0..1
            double valuesMultiplier = 1.0 - (normBiasDist * normBiasDist); // near bias => ~1, far => smaller

            // detect if voter has a very strong single-issue that aligns strongly with party (helpful if present)
            double bestSingleAlignment = computeBestSingleIssueAlignment(voterValues, rep.getParty() == null ? Collections.emptyList() : rep.getParty().getValues());
            double singleIssueBoost = 0.0;
            if (bestSingleAlignment >= 0.75 && biasSim >= 0.10) {
                // modest boost — lets passionate single issues sway among near-ish parties
                singleIssueBoost = 0.15 * bestSingleAlignment; // scale down
            }

            // final composite score
            double score = (BIAS_WEIGHT * biasSim) + (VALUES_WEIGHT * combinedValues * valuesMultiplier) + singleIssueBoost;

            // small tie-breaker jitter to avoid deterministic ties (but tiny)
            score += (ThreadLocalRandom.current().nextDouble(-1e-6, 1e-6));

            scoreMap.put(rep, score);
        }

        // produce sorted list by descending score
        List<Representative> ranking = new ArrayList<>(blockCandidates);
        ranking.sort((a, b) -> {
            double sa = scoreMap.getOrDefault(a, 0.0);
            double sb = scoreMap.getOrDefault(b, 0.0);

            int cmp = Double.compare(sb, sa); // descending
            if (cmp != 0) return cmp;

            // secondary tie-breaker: bias proximity
            double da = Math.abs(voterBias - a.getBias());
            double db = Math.abs(voterBias - b.getBias());
            cmp = Double.compare(da, db);
            if (cmp != 0) return cmp;

            // final: deterministic fallback to avoid cycles
            return Integer.compare(
                    System.identityHashCode(a),
                    System.identityHashCode(b)
            );
        });


        return ranking;
    }
    public static Map<Representative, Double> scoreCandidatesForVoter(
            Citizen voter, List<Representative> blockCandidates) {

        Map<Representative, Double> scoreMap = new LinkedHashMap<>();
        if (voter == null || blockCandidates == null || blockCandidates.isEmpty()) return scoreMap;

        final double PARTY_VS_PERSONAL = 0.70;

        double voterBias = voter.getBias();
        List<Value> voterValues = voter.getValues() == null ? Collections.emptyList() : voter.getValues();

        for (Representative rep : blockCandidates) {

            double biasDiff = Math.abs(voterBias - rep.getBias()) / 100.0;
            double biasSim = Math.exp(-4.0 * biasDiff);

            double partyAlignment = rep.getParty() != null
                    ? computeAlignmentScore(voterValues, rep.getParty().getValues())
                    : 0.0;

            double personalAlignment = computeAlignmentScore(
                    voterValues,
                    rep.getCitizen() == null ? Collections.emptyList() : rep.getCitizen().getValues()
            );

            double combinedValues = (partyAlignment * PARTY_VS_PERSONAL) +
                    (personalAlignment * (1.0 - PARTY_VS_PERSONAL));

            combinedValues = Math.pow(clamp01(combinedValues), 2.2);

            double bestSingleAlignment = computeBestSingleIssueAlignment(
                    voterValues,
                    rep.getParty() == null ? Collections.emptyList() : rep.getParty().getValues()
            );

            double singleIssueBoost = 0.0;
            if (bestSingleAlignment >= 0.75 && biasSim >= 0.05) {
                singleIssueBoost = 0.20 * bestSingleAlignment;
            }

            double score = biasSim * combinedValues + singleIssueBoost;

            score = clamp01(score);

            scoreMap.put(rep, score);
        }

        return scoreMap;
    }

    private static Map<String, Integer> ISSUE_INDEX = null;
    private static int ISSUE_COUNT = -1;

    private static synchronized void ensureIssueIndexLoaded() {
        if (ISSUE_INDEX == null) {
            ISSUE_INDEX = ValueAssigner.getIssueIndexMap();
            ISSUE_COUNT = ValueAssigner.getIssueCount();
        }
    }

    private static ValueProfile buildTempProfile(List<Value> values) {
        ValueProfile p = new ValueProfile(ISSUE_COUNT);
        if (values == null) return p;
        for (Value v : values) {
            if (v == null) continue;
            Integer idx = ISSUE_INDEX.get(v.getName().toLowerCase());
            if (idx == null) continue;
            p.importance[idx] = v.getPolarization() / 10.0;
            p.opinion[idx] = v.getOpinion();
        }
        return p;
    }

    private static double computeBestSingleIssueFromProfiles(ValueProfile voter, ValueProfile cand) {
        double best = 0.0;
        for (int i = 0; i < ISSUE_COUNT; i++) {
            double vi = voter.importance[i];
            double ci = cand.importance[i];
            if (vi == 0 || ci == 0) continue;
            double diff = Math.abs(voter.opinion[i] - cand.opinion[i]) / 20.0;
            double sim = 1.0 - diff;
            if (sim < 0) sim = 0;
            double score = sim * vi;
            if (score > best) best = score;
        }
        return best;
    }

    private static double computeAlignmentScore(List<Value> voterValues, List<Value> candidateValues) {
        ensureIssueIndexLoaded();
        ValueProfile voter = buildTempProfile(voterValues);
        ValueProfile cand = buildTempProfile(candidateValues);
        return AlignmentUtils.computeAlignmentScoreNoCache(voter, cand);
    }

    private static double computeBestSingleIssueAlignment(List<Value> voterValues, List<Value> candidateValues) {
        ensureIssueIndexLoaded();
        ValueProfile voter = buildTempProfile(voterValues);
        ValueProfile cand = buildTempProfile(candidateValues);
        return computeBestSingleIssueFromProfiles(voter, cand);
    }


    private static double clamp01(double v) {
        if (v <= 0.0) return 0.0;
        if (v >= 1.0) return 1.0;
        return v;
    }

    public static Representative runStarElection(
            List<Representative> candidates,
            List<Citizen> voters
    ) {

        return ElectionUtils.runStarElectionOptimized(candidates, voters);
    }



    public static Representative runFPTP(List<Representative> candidates, List<Citizen> voters) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (voters == null || voters.isEmpty()) return null;

        final int nCands = candidates.size();

        // candidate -> index for O(1) updates
        Map<Representative, Integer> idx = new HashMap<>(nCands);
        for (int i = 0; i < nCands; i++) idx.put(candidates.get(i), i);

        // Use LongAdders for contention-free parallel counting
        java.util.concurrent.atomic.LongAdder[] adders = new java.util.concurrent.atomic.LongAdder[nCands];
        for (int i = 0; i < nCands; i++) adders[i] = new java.util.concurrent.atomic.LongAdder();

        // Parallel evaluation of first choices without allocating ballot objects
        voters.parallelStream().forEach(voter -> {
            List<Representative> ranked = VotingUtils.rankCandidatesForVoter(voter, candidates);
            if (ranked != null && !ranked.isEmpty()) {
                Integer i = idx.get(ranked.get(0));
                if (i != null) adders[i].increment();
            }
        });

        // Find winner
        int bestIdx = -1;
        long bestCount = Long.MIN_VALUE;
        for (int i = 0; i < nCands; i++) {
            long c = adders[i].sum();
            if (c > bestCount) {
                bestCount = c;
                bestIdx = i;
            }
        }

        return bestIdx < 0 ? null : candidates.get(bestIdx);
    }

    public static Representative runFPTPStrategic(
            List<Representative> candidates,
            List<Citizen> voters
    ) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (voters == null || voters.isEmpty()) return null;

        final int nCands = candidates.size();

        // candidate -> index
        Map<Representative, Integer> idx = new HashMap<>(nCands);
        for (int i = 0; i < nCands; i++) idx.put(candidates.get(i), i);

        // Result container for each voter
        class Result {
            int[] ranks;    // ranking as cand-indices
            int first;      // first-choice index, -1 if none
            Result(int[] r, int f) { ranks = r; first = f; }
        }

        // PARALLEL: compute ranked preferences + first choices
        List<Result> allResults =
                voters.parallelStream().map(voter -> {

                    List<Representative> ranked =
                            VotingUtils.rankCandidatesForVoter(voter, candidates);

                    if (ranked == null || ranked.isEmpty())
                        return new Result(new int[0], -1);

                    int[] r = new int[ranked.size()];
                    for (int i = 0; i < ranked.size(); i++)
                        r[i] = idx.get(ranked.get(i));

                    int first = r.length > 0 ? r[0] : -1;

                    return new Result(r, first);

                }).toList();

        // Reduce first-round counts
        int[] firstCounts = new int[nCands];
        for (Result res : allResults) {
            if (res.first >= 0) firstCounts[res.first]++;
        }

        // Find top two
        int top = -1, second = -1;
        int vTop = -1, vSecond = -1;

        for (int i = 0; i < nCands; i++) {
            int v = firstCounts[i];
            if (v > vTop) {
                second = top;   vSecond = vTop;
                top = i;        vTop = v;
            } else if (v > vSecond) {
                second = i;     vSecond = v;
            }
        }

        if (top < 0) return null;
        if (second < 0) return candidates.get(top);

        // PARALLEL: strategic counting
        class Strategic {
            int topCount, secondCount;
            Strategic(int t, int s) { topCount = t; secondCount = s; }
        }

        int finalTop = top;
        int finalSecond = second;
        Strategic reduced = allResults.parallelStream().map(res -> {
            int tc = 0, sc = 0;
            for (int pos : res.ranks) {
                if (pos == finalTop) { tc++; break; }
                if (pos == finalSecond) { sc++; break; }
            }
            return new Strategic(tc, sc);

        }).reduce(
                new Strategic(0, 0),
                (a, b) -> new Strategic(a.topCount + b.topCount, a.secondCount + b.secondCount)
        );

        return (reduced.topCount >= reduced.secondCount)
                ? candidates.get(top)
                : candidates.get(second);
    }


    public enum FavoriteMode {
        TOP_RANK,
        TOP_SCORE
    }

    public static double computeMSEBetweenSeatsAndPopularVote(
            List<Representative> winners,
            Map<String, Double> popularShares) {

        if ((winners == null || winners.isEmpty()) &&
                (popularShares == null || popularShares.isEmpty())) {
            return 0.0;
        }

        Map<String, Integer> seatCounts = new HashMap<>();
        int totalSeats = 0;
        if (winners != null) {
            totalSeats = winners.size();
            for (Representative r : winners) {
                if (r == null) continue;
                String pname = (r.getParty() == null || r.getParty().getName() == null)
                        ? "Independent/None" : r.getParty().getName();
                seatCounts.merge(pname, 1, Integer::sum); // faster than getOrDefault+put
            }
        }

        // Compute MSE in a single pass over the union of parties
        Set<String> allParties = new HashSet<>();
        if (popularShares != null) allParties.addAll(popularShares.keySet());
        allParties.addAll(seatCounts.keySet());

        if (allParties.isEmpty()) return 0.0;

        double sumSq = 0.0;
        for (String p : allParties) {
            double seatShare = totalSeats > 0 ? seatCounts.getOrDefault(p, 0) / (double) totalSeats : 0.0;
            double popShare = popularShares == null ? 0.0 : popularShares.getOrDefault(p, 0.0);
            double diff = seatShare - popShare;
            sumSq += diff * diff;
        }

        return sumSq / allParties.size();
    }

    public static Representative runApprovalElection(
            List<Representative> candidates,
            List<Citizen> voters
    ) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (voters == null || voters.isEmpty()) return null;

        final int nCands = candidates.size();
        final int nVoters = voters.size();

        // Map candidate -> index for O(1) lookup
        Map<Representative, Integer> idx = new HashMap<>(nCands);
        for (int i = 0; i < nCands; i++) idx.put(candidates.get(i), i);

        // Step 1: Build approvals bitsets per voter in parallel
        long[][] bitsets; // use 64-bit chunks for memory locality when many candidates
        int longsPerRow = (nCands + 63) >>> 6;
        bitsets = new long[nVoters][longsPerRow];

        java.util.stream.IntStream.range(0, nVoters).parallel().forEach(vi -> {
            Citizen voter = voters.get(vi);
            ApprovalBallot ballot = ApprovalBallot.fromCitizen(voter, candidates);
            long[] row = bitsets[vi];
            for (Representative r : ballot.getApproved()) {
                Integer c = idx.get(r);
                if (c != null) {
                    int word = c >>> 6;
                    int bit = c & 63;
                    row[word] |= (1L << bit);
                }
            }
        });

        // Step 2: Count total approvals per candidate
        int[] totalApprovals = new int[nCands];
        for (int c = 0; c < nCands; c++) {
            int word = c >>> 6;
            long mask = 1L << (c & 63);
            int sum = 0;
            for (int vi = 0; vi < nVoters; vi++) {
                if ((bitsets[vi][word] & mask) != 0L) sum++;
            }
            totalApprovals[c] = sum;
        }

        // Step 3: Find top two candidates
        int first = -1, second = -1;
        int topCount = -1, secondCount = -1;
        for (int c = 0; c < nCands; c++) {
            int v = totalApprovals[c];
            if (v > topCount) {
                second = first; secondCount = topCount;
                first = c; topCount = v;
            } else if (v > secondCount) {
                second = c; secondCount = v;
            }
        }

        if (second < 0) return candidates.get(first);

        // Step 4: Runoff count in parallel
        int finalFirst = first;
        int finalSecond = second;
        java.util.concurrent.atomic.LongAdder firstVotes = new java.util.concurrent.atomic.LongAdder();
        java.util.concurrent.atomic.LongAdder secondVotes = new java.util.concurrent.atomic.LongAdder();

        java.util.stream.IntStream.range(0, nVoters).parallel().forEach(vi -> {
            long[] row = bitsets[vi];
            boolean approveFirst = ((row[finalFirst >>> 6] >>> (finalFirst & 63)) & 1L) != 0L;
            boolean approveSecond = ((row[finalSecond >>> 6] >>> (finalSecond & 63)) & 1L) != 0L;
            if (approveFirst && !approveSecond) firstVotes.increment();
            else if (approveSecond && !approveFirst) secondVotes.increment();
            else if (approveFirst && approveSecond) firstVotes.increment(); // tie-break to first
        });

        return (firstVotes.sum() >= secondVotes.sum()) ? candidates.get(first) : candidates.get(second);
    }

    private static String cachedTopParty = null;
    private static String cachedSecondParty = null;
    private static int cachedCandidateHash = 0;

    public static Representative runFPTPNationalStrategic(
            List<Representative> candidates,
            List<Citizen> localVoters
    ) {
        if (candidates == null || candidates.isEmpty()) return null;

        final int nCands = candidates.size();
        final List<Citizen> allCitizens = Main.country.getCitizens();

        // Compute hash of candidates to detect change
        int candidateHash = candidates.hashCode();
        Map<String, List<Representative>> partyToCandidates = new HashMap<>();
        for (Representative r : candidates) {
            partyToCandidates.computeIfAbsent(r.getParty().getName(), k -> new ArrayList<>()).add(r);
        }

        // ------------------- Compute national top two parties -------------------
        if (candidateHash != cachedCandidateHash) {
            Map<String, Integer> partyCounts = new HashMap<>();

            allCitizens.parallelStream().forEach(voter -> {
                List<Representative> ranked = VotingUtils.rankCandidatesForVoter(voter, candidates);
                if (ranked != null && !ranked.isEmpty()) {
                    String party = ranked.get(0).getParty().getName();
                    synchronized (partyCounts) {
                        partyCounts.put(party, partyCounts.getOrDefault(party, 0) + 1);
                    }
                }
            });

            String top = null, second = null;
            int vTop = -1, vSecond = -1;
            for (Map.Entry<String, Integer> entry : partyCounts.entrySet()) {
                int v = entry.getValue();
                if (v > vTop) {
                    second = top; vSecond = vTop;
                    top = entry.getKey(); vTop = v;
                } else if (v > vSecond) {
                    second = entry.getKey(); vSecond = v;
                }
            }

            cachedTopParty = top;
            cachedSecondParty = second;
            cachedCandidateHash = candidateHash;
        }

        if (cachedTopParty == null) return null;
        if (cachedSecondParty == null) return runFPTPStrategic(candidates, localVoters);

        final String topParty = cachedTopParty;
        final String secondParty = cachedSecondParty;

        // ------------------- Local strategic voting -------------------
        Map<Representative, Integer> idx = new HashMap<>();
        for (int i = 0; i < nCands; i++) idx.put(candidates.get(i), i);

        class StrategicCount {
            int topCount, secondCount;
            StrategicCount(int t, int s) { topCount = t; secondCount = s; }
        }

        StrategicCount total = localVoters.parallelStream().map(voter -> {
            List<Representative> ranked = VotingUtils.rankCandidatesForVoter(voter, candidates);
            if (ranked == null || ranked.isEmpty()) return new StrategicCount(0, 0);

            // Determine which of the two parties the voter prefers
            String preferredParty = null;
            for (Representative r : ranked) {
                String party = r.getParty().getName();
                if (party.equals(topParty) || party.equals(secondParty)) {
                    preferredParty = party;
                    break;
                }
            }

            // Attempt to vote for a candidate of that party locally
            Representative chosen = null;
            if (preferredParty != null) {
                for (Representative r : candidates) {
                    if ((r.getParty().getName().equals(preferredParty))) {
                        chosen = r;
                        break;
                    }
                }
            }

            // Fallback: if no candidate from preferred party, use local strategic vote
            if (chosen == null) {
                chosen = runFPTPStrategic(candidates, List.of(voter));
            }

            int tc = 0, sc = 0;
            if (chosen != null) {
                if (chosen.getParty().getName().equals(topParty)) tc = 1;
                else if (chosen.getParty().getName().equals(secondParty)) sc = 1;
            }

            return new StrategicCount(tc, sc);
        }).reduce(new StrategicCount(0,0),
                (a,b) -> new StrategicCount(a.topCount + b.topCount, a.secondCount + b.secondCount));

        return (total.topCount >= total.secondCount)
                ? partyToCandidates.get(topParty).get(0)
                : partyToCandidates.get(secondParty).get(0);
    }

    public static Party chooseBetweenTwoParties(Citizen voter, Party a, Party b) {
        if (voter == null) return null;
        if (a == null) return b;
        if (b == null) return a;

        List<Value> voterValues = voter.getValues() == null
                ? Collections.emptyList()
                : voter.getValues();

        double aAlign = computeAlignmentScore(voterValues, a.getValues());
        double bAlign = computeAlignmentScore(voterValues, b.getValues());

        // mild realism noise so elections are not perfectly deterministic
        aAlign += ThreadLocalRandom.current().nextDouble(-0.01, 0.01);
        bAlign += ThreadLocalRandom.current().nextDouble(-0.01, 0.01);

        return (aAlign >= bAlign) ? a : b;
    }
    public static Party getTruePreferredParty(Citizen voter, Collection<Party> allParties) {
        if (voter == null || allParties == null || allParties.isEmpty()) return null;

        List<Value> voterValues = voter.getValues() == null
                ? Collections.emptyList()
                : voter.getValues();

        Party best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Party p : allParties) {
            if (p == null) continue;

            double align = computeAlignmentScore(voterValues, p.getValues());

            align += ThreadLocalRandom.current().nextDouble(-0.01, 0.01);

            if (align > bestScore) {
                bestScore = align;
                best = p;
            }
        }

        return best;
    }

    public static Party getPreferredPartyForCitizen(
            Citizen voter,
            Party topParty,
            Party secondParty
    ) {
        if (voter == null) return null;

        return chooseBetweenTwoParties(voter, topParty, secondParty);
    }

    public static Representative runRCVElection(
            List<Representative> candidates,
            List<Citizen> voters
    ) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (voters == null || voters.isEmpty()) return null;

        final int nCands = candidates.size();
        final int nVoters = voters.size();

        // candidate -> index and back
        Map<Representative, Integer> idx = new HashMap<>(nCands);
        for (int i = 0; i < nCands; i++) idx.put(candidates.get(i), i);

        // Build compact preference lists (indices) once
        int[][] prefs = new int[nVoters][];
        for (int vi = 0; vi < nVoters; vi++) {
            Citizen voter = voters.get(vi);
            List<Representative> ranked = VotingUtils.rankCandidatesForVoter(voter, candidates);
            if (ranked == null || ranked.isEmpty()) {
                prefs[vi] = new int[0];
            } else {
                int m = ranked.size();
                int[] arr = new int[m];
                for (int j = 0; j < m; j++) arr[j] = idx.get(ranked.get(j));
                prefs[vi] = arr;
            }
        }

        boolean[] eliminated = new boolean[nCands];
        int remaining = nCands;

        // temporary counts
        int[] counts = new int[nCands];

        while (remaining > 1) {
            Arrays.fill(counts, 0);
            int totalVotes = 0;

            // Count each ballot's highest-ranked non-eliminated candidate
            for (int vi = 0; vi < nVoters; vi++) {
                int[] p = prefs[vi];
                for (int k = 0; k < p.length; k++) {
                    int c = p[k];
                    if (!eliminated[c]) {
                        counts[c]++;
                        totalVotes++;
                        break;
                    }
                }
            }

            if (totalVotes == 0) return null;

            // Check majority
            for (int c = 0; c < nCands; c++) {
                if (!eliminated[c] && counts[c] * 2 > totalVotes) {
                    return candidates.get(c);
                }
            }

            // Find lowest among active
            int lowest = -1;
            int worst = Integer.MAX_VALUE;
            for (int c = 0; c < nCands; c++) {
                if (!eliminated[c] && counts[c] < worst) {
                    worst = counts[c];
                    lowest = c;
                }
            }

            if (lowest < 0) break;
            eliminated[lowest] = true;
            remaining--;
        }

        // Return any remaining active candidate
        for (int c = 0; c < nCands; c++) if (!eliminated[c]) return candidates.get(c);
        return null;
    }

    public static Representative findTopCandidateForVoterFast(Citizen voter) {
        if (voter == null) return null;

        List<Representative> candidates = voter.getCountry().posExists;
        if (candidates == null || candidates.isEmpty()) return null;

        // Fast: pick voter's preferred party (cheap: iterates parties, not candidates)
        Party prefParty = getTruePreferredParty(voter, voter.getCountry().getParties());
        if (prefParty != null) {
            for (Representative r : candidates) {
                if (r == null) continue;
                Party rp = r.getParty();
                if (rp != null && rp.equals(prefParty)) return r;
            }
        }

        // Fallback: pick nearest-by-bias (very cheap)
        double voterBias = voter.getBias();
        Representative best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Representative r : candidates) {
            if (r == null) continue;
            double d = Math.abs(voterBias - r.getBias());
            if (d < bestDist) {
                bestDist = d;
                best = r;
            }
        }

        return best;
    }




}
