package com.crotaplague;

import com.crotaplague.Ballots.StarBallot;

import java.util.*;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

public class ElectionUtils {

    private static final double MAX_POWER = 10.0;
    private static final double SMALL_THRESHOLD = 1e-3;
    private static final double EPS = 1e-9;

    // Helper compact structure for a single voter's raw ballot
    private static final class VoterData {
        final int[] candIdx;   // indices of candidates present on this ballot
        final double[] raw;    // raw scores corresponding to candIdx entries
        final double maxRaw;   // max raw found for normalization
        final double power;    // precomputed power factor for this voter

        VoterData(int[] candIdx, double[] raw, double maxRaw, double power) {
            this.candIdx = candIdx;
            this.raw = raw;
            this.maxRaw = maxRaw;
            this.power = power;
        }
    }

    public static Representative runStarElectionOptimized(
            List<Representative> candidates,
            List<Citizen> voters
    ) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (voters == null || voters.isEmpty()) return null;

        final int nCands = candidates.size();
        final int nVoters = voters.size();

        // candidate -> index
        Map<Representative, Integer> idx = new HashMap<>(nCands);
        for (int i = 0; i < nCands; i++) idx.put(candidates.get(i), i);

        // Thread-safe accumulators for totals
        DoubleAdder[] totalsAdders = new DoubleAdder[nCands];
        for (int i = 0; i < nCands; i++) totalsAdders[i] = new DoubleAdder();

        // Compact per-voter storage
        final VoterData[] voterData = new VoterData[nVoters];

        // Build per-voter compact raw ballots and accumulate totals in parallel
        IntStream.range(0, nVoters).parallel().forEach(vi -> {
            Citizen voter = voters.get(vi);
            StarBallot sb = StarBallot.fromCitizen(voter, candidates);
            Map<Representative, Double> rawMap = sb.getScores();

            int m = rawMap == null ? 0 : rawMap.size();
            int[] ids = new int[m];
            double[] vals = new double[m];

            double maxRaw = 0.0;
            int j = 0;
            if (rawMap != null) {
                for (Map.Entry<Representative, Double> e : rawMap.entrySet()) {
                    Double v = e.getValue();
                    if (v == null) continue;
                    double s = v.doubleValue();
                    if (s <= 0.0) {
                        // skip negative or zero scores but store as zero if desired
                        ids[j] = idx.get(e.getKey());
                        vals[j] = 0.0;
                    } else {
                        ids[j] = idx.get(e.getKey());
                        vals[j] = s;
                        if (s > maxRaw) maxRaw = s;
                    }
                    j++;
                }
            }

            // compute power from extremism (clamp01 assumed available)
            double extremism = clamp01(voter.getExtremism());
            double power = 1.0 + extremism * (MAX_POWER - 1.0);

            // accumulate transformed values to totals
            if (maxRaw > 0.0) {
                double invMax = 1.0 / maxRaw;
                for (int k = 0; k < m; k++) {
                    double rawScore = vals[k];
                    if (rawScore <= 0.0) continue;
                    double normalized = rawScore * invMax;
                    double t = Math.pow(normalized, power);
                    double transformed = (t < SMALL_THRESHOLD ? 0.0 : (t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t))) * 10.0;
                    if (transformed != 0.0) totalsAdders[ids[k]].add(transformed);
                }
            }
            // store compact raw ballot for the runoff pass
            voterData[vi] = new VoterData(ids, vals, maxRaw, power);
        });

        // Extract totals
        double[] totals = new double[nCands];
        for (int c = 0; c < nCands; c++) totals[c] = totalsAdders[c].sum();

        // Find top two candidates
        int first = -1, second = -1;
        double best = Double.NEGATIVE_INFINITY, secondBest = Double.NEGATIVE_INFINITY;
        for (int c = 0; c < nCands; c++) {
            double sc = totals[c];
            if (sc > best) {
                second = first;
                secondBest = best;
                first = c;
                best = sc;
            } else if (sc > secondBest) {
                second = c;
                secondBest = sc;
            }
        }

        if (first < 0) return null;
        if (second < 0) return candidates.get(first);

        // Runoff: compare only first and second for each voter
        LongAdder firstVotesAdder = new LongAdder();
        LongAdder secondVotesAdder = new LongAdder();

        int finalFirst = first;
        int finalSecond = second;
        IntStream.range(0, nVoters).parallel().forEach(vi -> {
            VoterData vd = voterData[vi];
            double aVal = 0.0;
            double bVal = 0.0;

            if (vd.maxRaw > 0.0 && vd.candIdx.length > 0) {
                // find first in compact arrays
                for (int k = 0; k < vd.candIdx.length; k++) {
                    int cand = vd.candIdx[k];
                    if (cand == finalFirst) {
                        double rawScore = vd.raw[k];
                        if (rawScore > 0.0) {
                            double normalized = rawScore / vd.maxRaw;
                            double t = Math.pow(normalized, vd.power);
                            aVal = (t < SMALL_THRESHOLD ? 0.0 : (t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t))) * 10.0;
                        }
                    } else if (cand == finalSecond) {
                        double rawScore = vd.raw[k];
                        if (rawScore > 0.0) {
                            double normalized = rawScore / vd.maxRaw;
                            double t = Math.pow(normalized, vd.power);
                            bVal = (t < SMALL_THRESHOLD ? 0.0 : (t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t))) * 10.0;
                        }
                    }
                    // micro-optimization: if both found break early
                    if (aVal != 0.0 && bVal != 0.0) break;
                }
            }

            if (aVal > bVal + EPS) firstVotesAdder.increment();
            else if (bVal > aVal + EPS) secondVotesAdder.increment();
        });

        long firstVotes = firstVotesAdder.sum();
        long secondVotes = secondVotesAdder.sum();

        return (firstVotes >= secondVotes) ? candidates.get(first) : candidates.get(second);
    }

    // Placeholder for clamp01 to match your code
    private static double clamp01(double x) {
        if (Double.isNaN(x)) return 0.0;
        if (x <= 0.0) return 0.0;
        if (x >= 1.0) return 1.0;
        return x;
    }
}

