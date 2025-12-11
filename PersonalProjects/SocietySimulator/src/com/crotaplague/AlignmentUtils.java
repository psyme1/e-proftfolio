package com.crotaplague;

import java.util.concurrent.ConcurrentHashMap;

public final class AlignmentUtils {

    // cache for computed alignments: key = (voterIndex << 32) | targetIndex
    private static final ConcurrentHashMap<Long, Double> ALIGN_CACHE = new ConcurrentHashMap<>();

    /**
     * Compute alignment between two profiles (voter vs other), not using cache.
     * This is the tight hot loop.
     */
    public static double computeAlignmentScoreNoCache(ValueProfile voter, ValueProfile other) {
        if (voter == null || other == null) return 0.0;
        double sum = 0.0;
        double weightSum = 0.0;
        double[] vi = voter.importance;
        double[] vp = voter.opinion;
        double[] oi = other.importance;
        double[] op = other.opinion;

        // linear scan across issues
        for (int k = 0; k < vi.length; k++) {
            double vImp = vi[k];
            if (vImp <= 0.0) continue; // voter doesn't care about issue
            double oImp = oi[k];
            if (oImp <= 0.0) continue; // candidate/party doesn't have issue
            // similarity of opinion (0..1)
            double diff = Math.abs(vp[k] - op[k]) / 20.0; // opinion range is 20
            double sim = 1.0 - diff;
            if (sim < 0.0) sim = 0.0;
            sum += sim * vImp; // weight by voter importance
            weightSum += vImp;
        }
        if (weightSum == 0.0) return 0.0;
        return sum / weightSum;
    }

    /** Cached compute. voterIndex and targetIndex must be stable indices assigned by ProfileFactory. */
    public static double computeAlignmentScoreCached(int voterIndex, int targetIndex, ValueProfile voter, ValueProfile target) {
        long key = (((long) voterIndex) << 32) | (targetIndex & 0xffffffffL);
        return ALIGN_CACHE.computeIfAbsent(key, k -> computeAlignmentScoreNoCache(voter, target));
    }

    /** Helper to clear cache if you want to free memory between runs. */
    public static void clearCache() {
        ALIGN_CACHE.clear();
    }
}
