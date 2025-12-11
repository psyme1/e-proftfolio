package com.crotaplague.Ballots;

import com.crotaplague.Citizen;
import com.crotaplague.Representative;
import com.crotaplague.VotingUtils;

import java.util.*;
import java.util.stream.Collectors;

public class StarBallot implements Ballot {
    // store double scores (0.0..10.0) instead of ints
    private Map<Representative, Double> rankings;

    public StarBallot(Map<Representative, Double> scores) {
        this.rankings = new HashMap<>(scores);
    }
    public StarBallot() {
        rankings = new HashMap<>();
    }

    public Representative compareReps(Representative r1, Representative r2) {
        if(rankings.containsKey(r1) && rankings.containsKey(r2)) {
            double s1 = rankings.get(r1);
            double s2 = rankings.get(r2);
            if (s1 > s2) return r1;
            if (s2 > s1) return r2;
        }
        return null;
    }

    @Override
    public BallotType getType() { return BallotType.STAR; }

    @Override
    public Representative chooseWinnerBetween(Representative a, Representative b) {
        double sa = rankings.getOrDefault(a, 0.0);
        double sb = rankings.getOrDefault(b, 0.0);
        if (sa > sb) return a;
        if (sb > sa) return b;
        return null;
    }
    // expose doubles
    public Map<Representative, Double> getScores() {
        return rankings;
    }
    public void setScores(Map<Representative, Double> scores) {
        this.rankings = new HashMap<>(scores);
    }
    public void setRanking(Representative r, Double score){
        rankings.put(r, score);
    }
    public double getRanking(Representative r){
        return rankings.getOrDefault(r, 0.0);
    }
    public List<Representative> sortedCandidatesByScore() {
        return rankings.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    public Representative topAmong(Collection<Representative> subset) {
        Representative best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Representative r : subset) {
            double s = rankings.getOrDefault(r, 0.0);
            if (best == null || s > bestScore) {
                best = r;
                bestScore = s;
            }
        }
        return best;
    }
    public static StarBallot fromCitizen(Citizen voter, List<Representative> candidates) {
        StarBallot b = new StarBallot();
        if (voter == null || candidates == null || candidates.isEmpty()) return b;

        Map<Representative, Double> raw = VotingUtils.scoreCandidatesForVoter(voter, candidates);
        if (raw.isEmpty()) return b;

        for (Representative r : candidates) {
            double rv = raw.getOrDefault(r, 0.0);
            // keep as double in 0..10 range (no rounding)
            double finalScore = Math.max(0.0, Math.min(10.0, rv * 10.0));
            b.rankings.put(r, finalScore);
        }

        return b;
    }
}
