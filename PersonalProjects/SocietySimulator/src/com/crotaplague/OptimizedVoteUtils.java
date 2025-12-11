package com.crotaplague;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Drop-in optimized + accurate version of OptimizedVoteUtils.
 *
 * Preserves original semantics (tie-breaking, party-name resolution, selection heuristics),
 * but keeps sparse scoring and chunked parallelism for speed. Avoids stale TL buffers by
 * using per-voter sized local buffers (prevents correctness regressions).
 */
public final class OptimizedVoteUtils {

    private static final String INDEPENDENT = "Independent/None";

    private static final ForkJoinPool VOTE_POOL = new ForkJoinPool(
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(),
                    Math.max(1, Runtime.getRuntime().availableProcessors() - 1)))
    );

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    // If a voter has more than this many non-zero issues use dense (vectorized) path.
    private static final int DENSE_THRESHOLD = 64;

    // Same threshold for deciding whether to precompute voter×party matrix.
    private static final long MATRIX_DOUBLE_THRESHOLD = 25_000_000L;

    public static Map<String, Double> computePopularPartyShares(
            List<Representative> candidates,
            List<Citizen> voters,
            VotingUtils.FavoriteMode mode,
            boolean parallel) {
        return computePopularPartyShares(candidates, voters, mode, parallel, false, Integer.MAX_VALUE, 0);
    }

    public static Map<String, Double> computePopularPartyShares(
            List<Representative> candidates,
            List<Citizen> voters,
            VotingUtils.FavoriteMode mode,
            boolean parallel,
            boolean partyOnly,
            int topPartiesToConsider,
            int maxRepsToEvaluatePerVoter) {

        if (candidates == null || candidates.isEmpty() || voters == null || voters.isEmpty()) {
            return Collections.emptyMap();
        }

        final ProfileFactory.Profiles prof = ProfileFactory.buildProfiles(voters, candidates);
        final int numVoters = voters.size();
        final int numReps = candidates.size();
        final int numParties = prof.partyProfiles.length;

        // Pre-extract rep arrays identical to original behavior
        final double[] repBias = new double[numReps];
        final ValueProfile[] repProfiles = new ValueProfile[numReps];
        final int[] repToPartyIdx = new int[numReps];
        final String[] repPartyName = new String[numReps];
        for (int ri = 0; ri < numReps; ri++) {
            Representative r = candidates.get(ri);
            repBias[ri] = r == null ? 0.0 : r.getBias();
            repProfiles[ri] = prof.repProfiles[ri];
            repToPartyIdx[ri] = prof.repToPartyIndex[ri];
            Party p = r == null ? null : r.getParty();
            repPartyName[ri] = (p == null || p.getName() == null) ? INDEPENDENT : p.getName();
        }

        // Build party -> rep arrays (primitive)
        final int[][] partyToRepsArr = new int[numParties][];
        // Also collect independent (no party) reps once to ensure they are considered
        int[] independentReps;
        {
            Map<Integer, List<Integer>> tmp = new HashMap<>();
            List<Integer> indep = new ArrayList<>();
            for (int ri = 0; ri < numReps; ri++) {
                int pidx = repToPartyIdx[ri];
                if (pidx < 0) {
                    indep.add(ri);
                } else {
                    tmp.computeIfAbsent(pidx, k -> new ArrayList<>()).add(ri);
                }
            }
            for (int pi = 0; pi < numParties; pi++) {
                List<Integer> list = tmp.get(pi);
                if (list == null || list.isEmpty()) partyToRepsArr[pi] = new int[0];
                else {
                    int[] arr = new int[list.size()];
                    for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
                    partyToRepsArr[pi] = arr;
                }
            }
            if (indep.isEmpty()) independentReps = new int[0];
            else {
                independentReps = new int[indep.size()];
                for (int i = 0; i < indep.size(); i++) independentReps[i] = indep.get(i);
            }
        }

        // Precompute party names exactly as original getPartyNameByIndex mapping
        final String[] partyNames = new String[numParties];
        {
            Party[] partyByIndex = new Party[numParties];
            for (Map.Entry<Party, Integer> e : prof.partyIndex.entrySet()) {
                Party p = e.getKey();
                int idx = e.getValue();
                if (idx >= 0 && idx < numParties) partyByIndex[idx] = p;
            }
            for (int i = 0; i < numParties; i++) {
                Party p = partyByIndex[i];
                partyNames[i] = (p == null || p.getName() == null) ? INDEPENDENT : p.getName();
            }
        }

        // Determine if we need an extra bucket for independents (no party profile present)
        int independentIndex = -1;
        for (int i = 0; i < numParties; i++) {
            if (INDEPENDENT.equals(partyNames[i])) { independentIndex = i; break; }
        }
        final int totalSlots = (independentIndex >= 0) ? numParties : (numParties + 1);
        final int indepSlot = (independentIndex >= 0) ? independentIndex : numParties;
        final String[] outputNames;
        if (independentIndex >= 0) {
            outputNames = partyNames;
        } else {
            outputNames = Arrays.copyOf(partyNames, totalSlots);
            outputNames[indepSlot] = INDEPENDENT;
        }

        // ---------------- build sparse voter views ----------------
        final int[][] voterIndices = new int[numVoters][];
        final double[][] voterImportance = new double[numVoters][];
        final double[][] voterOpinion = new double[numVoters][];
        final int[] voterLens = new int[numVoters];

        for (int vi = 0; vi < numVoters; vi++) {
            ValueProfile vp = prof.voterProfiles[vi];
            double[] imp = vp.importance;
            double[] op = vp.opinion;
            final int len = imp.length;
            int cnt = 0;
            for (int i = 0; i < len; i++) if (imp[i] > 0.0) cnt++;
            voterLens[vi] = cnt;
            if (cnt == 0) {
                voterIndices[vi] = new int[0];
                voterImportance[vi] = new double[0];
                voterOpinion[vi] = new double[0];
            } else {
                int[] idx = new int[cnt];
                double[] viImp = new double[cnt];
                double[] viOp = new double[cnt];
                int pos = 0;
                for (int i = 0; i < len; i++) {
                    double w = imp[i];
                    if (w > 0.0) {
                        idx[pos] = i;
                        viImp[pos] = w;
                        viOp[pos] = op[i];
                        pos++;
                    }
                }
                voterIndices[vi] = idx;
                voterImportance[vi] = viImp;
                voterOpinion[vi] = viOp;
            }
        }

        // Decide precompute matrix (sparse scoring)
        final long neededDoubles = (long) numVoters * (long) numParties;
        final boolean precomputeMatrix = neededDoubles <= MATRIX_DOUBLE_THRESHOLD;

        final double[][] voterPartyScores;
        if (precomputeMatrix) {
            voterPartyScores = new double[numVoters][numParties];
            if (parallel && numVoters > 1) {
                int threads = Math.min(VOTE_POOL.getParallelism(), Math.max(1, numVoters));
                int chunk = (numVoters + threads - 1) / threads;
                List<Callable<Void>> tasks = new ArrayList<>(threads);
                for (int t = 0; t < threads; t++) {
                    final int start = t * chunk;
                    final int end = Math.min(numVoters, start + chunk);
                    tasks.add(() -> {
                        for (int vi = start; vi < end; vi++) {
                            if (voterLens[vi] >= DENSE_THRESHOLD) {
                                ValueProfile vprof = prof.voterProfiles[vi];
                                for (int pi = 0; pi < numParties; pi++) {
                                    voterPartyScores[vi][pi] = alignmentScore(SPECIES, vprof, prof.partyProfiles[pi]);
                                }
                            } else {
                                int[] idx = voterIndices[vi];
                                double[] vimp = voterImportance[vi];
                                double[] vop = voterOpinion[vi];
                                for (int pi = 0; pi < numParties; pi++) {
                                    voterPartyScores[vi][pi] = alignmentScoreSparse(idx, vimp, vop, prof.partyProfiles[pi]);
                                }
                            }
                        }
                        return null;
                    });
                }
                try {
                    List<Future<Void>> futs = VOTE_POOL.invokeAll(tasks);
                    for (Future<Void> f : futs) f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                for (int vi = 0; vi < numVoters; vi++) {
                    if (voterLens[vi] >= DENSE_THRESHOLD) {
                        ValueProfile vprof = prof.voterProfiles[vi];
                        for (int pi = 0; pi < numParties; pi++) {
                            voterPartyScores[vi][pi] = alignmentScore(SPECIES, vprof, prof.partyProfiles[pi]);
                        }
                    } else {
                        int[] idx = voterIndices[vi];
                        double[] vimp = voterImportance[vi];
                        double[] vop = voterOpinion[vi];
                        for (int pi = 0; pi < numParties; pi++) {
                            voterPartyScores[vi][pi] = alignmentScoreSparse(idx, vimp, vop, prof.partyProfiles[pi]);
                        }
                    }
                }
            }
        } else {
            voterPartyScores = null;
        }

        // ---------------- party-only fast path (keeps original semantics) ----------------
        if (partyOnly) {
            final int threads = (parallel && numVoters > 1) ? Math.min(VOTE_POOL.getParallelism(), Math.max(1, numVoters)) : 1;
            final int chunk = (numVoters + threads - 1) / threads;
            List<Callable<long[]>> tasks = new ArrayList<>(threads);
            for (int t = 0; t < threads; t++) {
                final int start = t * chunk;
                final int end = Math.min(numVoters, start + chunk);
                tasks.add(() -> {
                    long[] localCounts = new long[totalSlots];
                    double[] localPScores = precomputeMatrix ? null : new double[numParties];
                    for (int vi = start; vi < end; vi++) {
                        final double[] pScores = precomputeMatrix ? voterPartyScores[vi] :
                                computePartyScoresSparse(vi, prof, voterIndices, voterImportance, voterOpinion, voterLens, localPScores);
                        int bestParty = -1;
                        double best = Double.NEGATIVE_INFINITY;
                        for (int pi = 0; pi < numParties; pi++) {
                            double s = pScores[pi];
                            if (s > best) { best = s; bestParty = pi; }
                        }
                        if (bestParty >= 0) localCounts[bestParty]++;
                        else { localCounts[indepSlot]++; }
                    }
                    return localCounts;
                });
            }

            long[] total = new long[totalSlots];
            try {
                List<Future<long[]>> futs = VOTE_POOL.invokeAll(tasks);
                for (Future<long[]> f : futs) {
                    long[] lc = f.get();
                    for (int i = 0; i < totalSlots; i++) total[i] += lc[i];
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            long totalVotes = 0L;
            for (long v : total) totalVotes += v;
            if (totalVotes == 0) return Collections.emptyMap();

            LinkedHashMap<String, Double> out = new LinkedHashMap<>();
            Integer[] idx = new Integer[totalSlots];
            for (int i = 0; i < totalSlots; i++) idx[i] = i;
            Arrays.sort(idx, (a, b) -> Long.compare(total[b], total[a]));
            for (int id : idx) {
                if (total[id] > 0) out.put(outputNames[id], total[id] / (double) totalVotes);
            }
            return out;
        }

        // ---------------------------- HYBRID PATH ----------------------------
        final int threads = (parallel && numVoters > 1) ? Math.min(VOTE_POOL.getParallelism(), Math.max(1, numVoters)) : 1;
        final int chunk = (numVoters + threads - 1) / threads;
        List<Callable<long[]>> tasks = new ArrayList<>(threads);

        for (int t = 0; t < threads; t++) {
            final int start = t * chunk;
            final int end = Math.min(numVoters, start + chunk);

            tasks.add(() -> {
                long[] localCounts = new long[totalSlots];
                double[] localPScores = precomputeMatrix ? null : new double[numParties];

                for (int vi = start; vi < end; vi++) {
                    ValueProfile voterProf = prof.voterProfiles[vi];
                    double voterBias = voters.get(vi).getBias();

                    final double[] pScores = precomputeMatrix ? voterPartyScores[vi] :
                            computePartyScoresSparse(vi, prof, voterIndices, voterImportance, voterOpinion, voterLens, localPScores);

                    // pick top K parties (same semantics as older code)
                    int K = Math.max(1, Math.min(topPartiesToConsider, numParties));
                    int[] topParties = topKPartiesByHeap(pScores, K);

                    // gather candidate indices from those parties into a fresh candidate buffer sized to pos
                    int[] candBufTemp = new int[0];
                    int totalCandidates = 0;
                    for (int p : topParties) {
                        if (p < 0 || p >= partyToRepsArr.length) continue;
                        totalCandidates += partyToRepsArr[p].length;
                    }
                    // Also include independents so they can be evaluated
                    totalCandidates += independentReps.length;
                    if (totalCandidates == 0) continue;
                    candBufTemp = new int[totalCandidates];
                    int pos = 0;
                    for (int p : topParties) {
                        if (p < 0 || p >= partyToRepsArr.length) continue;
                        int[] reps = partyToRepsArr[p];
                        for (int r : reps) candBufTemp[pos++] = r;
                    }
                    // append independents
                    for (int r : independentReps) candBufTemp[pos++] = r;

                    final int[] selected;
                    if (maxRepsToEvaluatePerVoter > 0 && pos > maxRepsToEvaluatePerVoter) {
                        // select top M by party alignment proxy EXACTLY like old code
                        int need = maxRepsToEvaluatePerVoter;
                        int[] selHeapIdx = new int[need];
                        double[] selHeapVals = new double[need];
                        selected = selectTopMByPartyAlignmentInplace(candBufTemp, pos, need, pScores, prof, selHeapIdx, selHeapVals);
                    } else {
                        selected = Arrays.copyOf(candBufTemp, pos);
                    }

                    final int selLen = selected.length;
                    if (selLen == 0) continue;

                    // allocate exact-size arrays for alignment values (avoid stale tails)
                    double[] personalAlign = new double[selLen];
                    double[] repPartyAlign = new double[selLen];

                    // compute rep party alignment (from pScores) and personal align for selected candidates
                    int[] vIdx = voterIndices[vi];
                    double[] vImp = voterImportance[vi];
                    double[] vOp = voterOpinion[vi];
                    int vlen = voterLens[vi];

                    for (int j = 0; j < selLen; j++) {
                        final int ri = selected[j];
                        int pidx = repToPartyIdx[ri];
                        repPartyAlign[j] = (pidx >= 0 && pidx < pScores.length) ? pScores[pidx] : 0.0;
                        ValueProfile rprof = repProfiles[ri];
                        if (vlen >= DENSE_THRESHOLD) {
                            personalAlign[j] = alignmentScore(SPECIES, voterProf, rprof);
                        } else {
                            personalAlign[j] = alignmentScoreSparse(vIdx, vImp, vOp, rprof);
                        }
                    }

                    // evaluate selected candidates (same tie-breaking as original)
                    int bestRep = -1;
                    double bestScore = Double.NEGATIVE_INFINITY;
                    for (int j = 0; j < selLen; j++) {
                        int ri = selected[j];
                        double repB = repBias[ri];
                        double partyAlignment = repPartyAlign[j];
                        double personalAlignment = personalAlign[j];

                        double score = computeScore(mode, voterBias, repB, partyAlignment, personalAlignment);
                        if (score > bestScore) {
                            bestScore = score;
                            bestRep = ri;
                        } else if (score == bestScore && bestRep >= 0) {
                            double da = Math.abs(voterBias - repBias[bestRep]);
                            double db = Math.abs(voterBias - repB);
                            if (db < da) bestRep = ri;
                            else if (db == da && ri < bestRep) bestRep = ri; // deterministic tiebreaker by rep index
                        }
                    }

                    if (bestRep >= 0) {
                        int partyIdx = repToPartyIdx[bestRep];
                        if (partyIdx >= 0 && partyIdx < numParties) localCounts[partyIdx]++;
                        else { localCounts[indepSlot]++; }
                    }
                }
                return localCounts;
            });
        }

        long[] total = new long[totalSlots];
        try {
            List<Future<long[]>> futs = VOTE_POOL.invokeAll(tasks);
            for (Future<long[]> f : futs) {
                long[] lc = f.get();
                for (int i = 0; i < totalSlots; i++) total[i] += lc[i];
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        long totalVotes = 0L;
        for (long v : total) totalVotes += v;
        if (totalVotes == 0) return Collections.emptyMap();

        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        Integer[] idx = new Integer[totalSlots];
        for (int i = 0; i < totalSlots; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Long.compare(total[b], total[a]));
        for (int id : idx) {
            if (total[id] > 0) out.put(outputNames[id], total[id] / (double) totalVotes);
        }
        return out;
    }

    // -------------------- helper alignment functions --------------------

    private static double alignmentScore(VectorSpecies<Double> species, ValueProfile a, ValueProfile b) {
        final double[] ai = a.importance;
        final double[] bi = b.importance;
        final double[] ao = a.opinion;
        final double[] bo = b.opinion;
        final int len = ai.length;
        final int s = species.length();

        int i = 0;
        DoubleVector vsumContrib = DoubleVector.zero(species);
        DoubleVector vsumWeight = DoubleVector.zero(species);

        DoubleVector v20 = DoubleVector.broadcast(species, 20.0);
        DoubleVector vone = DoubleVector.broadcast(species, 1.0);
        DoubleVector vzero = DoubleVector.zero(species);

        for (; i + s <= len; i += s) {
            DoubleVector va = DoubleVector.fromArray(species, ai, i);
            DoubleVector vb = DoubleVector.fromArray(species, bi, i);
            DoubleVector wa = va.mul(vb);

            DoubleVector aoV = DoubleVector.fromArray(species, ao, i);
            DoubleVector boV = DoubleVector.fromArray(species, bo, i);

            DoubleVector diff = aoV.sub(boV).abs();
            DoubleVector sim = vone.sub(diff.div(v20));
            sim = sim.max(vzero).min(vone);

            DoubleVector contrib = wa.mul(sim);

            vsumContrib = vsumContrib.add(contrib);
            vsumWeight = vsumWeight.add(wa);
        }

        double sumContrib = vsumContrib.reduceLanes(VectorOperators.ADD);
        double sumWeight = vsumWeight.reduceLanes(VectorOperators.ADD);

        for (; i < len; i++) {
            double w = ai[i] * bi[i];
            double sim = 1.0 - (Math.abs(ao[i] - bo[i]) / 20.0);
            if (sim < 0.0) sim = 0.0;
            else if (sim > 1.0) sim = 1.0;
            sumContrib += w * sim;
            sumWeight += w;
        }
        return sumWeight > 0.0 ? (sumContrib / sumWeight) : 0.0;
    }

    private static double alignmentScoreSparse(int[] idx, double[] vimp, double[] vop, ValueProfile rep) {
        final double[] rip = rep.importance;
        final double[] rop = rep.opinion;
        final int n = idx.length;
        double sumW = 0.0;
        double sumContrib = 0.0;
        for (int k = 0; k < n; k++) {
            int i = idx[k];
            double ai = vimp[k];
            double bi = rip[i];
            if (bi <= 0.0 || ai <= 0.0) continue;
            double w = ai * bi;
            double sim = 1.0 - (Math.abs(vop[k] - rop[i]) / 20.0);
            if (sim < 0.0) sim = 0.0;
            else if (sim > 1.0) sim = 1.0;
            sumContrib += w * sim;
            sumW += w;
        }
        return sumW > 0.0 ? (sumContrib / sumW) : 0.0;
    }

    private static double[] computePartyScoresSparse(int vi,
                                                     ProfileFactory.Profiles prof,
                                                     int[][] voterIndices,
                                                     double[][] voterImportance,
                                                     double[][] voterOpinion,
                                                     int[] voterLens,
                                                     double[] buf) {
        final int nParties = prof.partyProfiles.length;
        if (buf == null || buf.length < nParties) buf = new double[nParties];
        int[] idx = voterIndices[vi];
        double[] vimp = voterImportance[vi];
        double[] vop = voterOpinion[vi];
        int vlen = voterLens[vi];

        if (vlen >= DENSE_THRESHOLD) {
            ValueProfile vprof = prof.voterProfiles[vi];
            for (int pi = 0; pi < nParties; pi++) buf[pi] = alignmentScore(SPECIES, vprof, prof.partyProfiles[pi]);
            return buf;
        }

        for (int pi = 0; pi < nParties; pi++) {
            buf[pi] = alignmentScoreSparse(idx, vimp, vop, prof.partyProfiles[pi]);
        }
        return buf;
    }

    // --- helpers (same semantics as your earlier helpers) ---
    private static int[] topKPartiesByHeap(double[] scores, int k) {
        final int n = scores.length;
        if (k >= n) {
            int[] all = new int[n];
            for (int i = 0; i < n; i++) all[i] = i;
            return all;
        }
        int[] heapIdx = new int[k];
        double[] heapVals = new double[k];
        int size = 0;
        for (int i = 0; i < n; i++) {
            double v = scores[i];
            if (size < k) {
                heapIdx[size] = i;
                heapVals[size] = v;
                siftUp(heapIdx, heapVals, size);
                size++;
            } else if (v > heapVals[0]) {
                heapIdx[0] = i;
                heapVals[0] = v;
                siftDown(heapIdx, heapVals, 0, size);
            }
        }
        int[] out = new int[size];
        System.arraycopy(heapIdx, 0, out, 0, size);
        return out;
    }

    private static int[] selectTopMByPartyAlignmentInplace(
            int[] candIdx, int len, int m,
            double[] pScores, ProfileFactory.Profiles prof,
            int[] heapIdx, double[] heapVals) {

        if (m <= 0) return new int[0];
        if (m >= len) return Arrays.copyOf(candIdx, len);

        int heapSize = 0;
        for (int i = 0; i < len; i++) {
            int ri = candIdx[i];
            int partyIdx = prof.repToPartyIndex[ri];
            double val = (partyIdx >= 0 && partyIdx < pScores.length) ? pScores[partyIdx] : 0.0;

            if (heapSize < m) {
                heapIdx[heapSize] = ri;
                heapVals[heapSize] = val;
                siftUp(heapIdx, heapVals, heapSize);
                heapSize++;
            } else if (val > heapVals[0]) {
                heapIdx[0] = ri;
                heapVals[0] = val;
                siftDown(heapIdx, heapVals, 0, heapSize);
            }
        }

        int[] out = new int[heapSize];
        System.arraycopy(heapIdx, 0, out, 0, heapSize);
        return out;
    }

    private static void siftUp(int[] heap, double[] vals, int idx) {
        int i = idx;
        while (i > 0) {
            int parent = (i - 1) >>> 1;
            if (vals[i] < vals[parent]) {
                swap(heap, vals, i, parent);
                i = parent;
            } else break;
        }
    }

    private static void siftDown(int[] heap, double[] vals, int idx, int size) {
        int i = idx;
        while (true) {
            int left = (i << 1) + 1;
            if (left >= size) break;
            int right = left + 1;
            int smallest = left;
            if (right < size && vals[right] < vals[left]) smallest = right;
            if (vals[smallest] < vals[i]) {
                swap(heap, vals, i, smallest);
                i = smallest;
            } else break;
        }
    }

    private static void swap(int[] heap, double[] vals, int i, int j) {
        int t = heap[i]; heap[i] = heap[j]; heap[j] = t;
        double d = vals[i]; vals[i] = vals[j]; vals[j] = d;
    }

    // scoring math preserved
    private static double computeScore(VotingUtils.FavoriteMode mode, double voterBias, double repBias, double partyAlign, double personalAlign) {
        final double PARTY_VS_PERSONAL = 0.70;
        double combined = clamp01(partyAlign * PARTY_VS_PERSONAL + personalAlign * (1.0 - PARTY_VS_PERSONAL));
        if (mode == VotingUtils.FavoriteMode.TOP_RANK) {
            final double BW = 0.60, VW = 0.40;
            double biasSim = clamp01(1.0 - (Math.abs(voterBias - repBias) / 100.0));
            double normBiasDist = Math.abs(voterBias - repBias) / 100.0;
            double valuesMult = 1.0 - (normBiasDist * normBiasDist);
            return clamp01(BW * biasSim + VW * combined * valuesMult);
        } else {
            double biasDiff = Math.abs(voterBias - repBias) / 100.0;
            double biasSim = Math.exp(-4.0 * biasDiff);
            return clamp01(biasSim * Math.pow(combined, 2.2));
        }
    }

    private static double clamp01(double v) {
        if (v <= 0.0) return 0.0;
        if (v >= 1.0) return 1.0;
        return v;
    }
}
