package com.crotaplague.Ballots;

import com.crotaplague.Citizen;
import com.crotaplague.Country;
import com.crotaplague.Representative;
import com.crotaplague.VotingUtils;

import java.util.List;
import java.util.Collections;

public final class BallotFactory {

    private BallotFactory() { /* no instances */ }

    /**
     * Create a ballot representing the voter's preferences.
     * Uses the full candidate pool Country.posExists for ranking.
     * Returns an RCVBallot when a ranking is available, otherwise
     * falls back to a single-choice FPTPBallot.
     */
    public static Ballot fromCitizen(Citizen voter) {
        if (voter == null) {
            return new FPTPBallot(null);
        }

        List<Representative> all = voter.getCountry().posExists;
        if (all == null || all.isEmpty()) {
            return new FPTPBallot(null);
        }

        // Use your existing ranking util which expects a voter and candidate list
        List<Representative> ranked = VotingUtils.rankCandidatesForVoter(voter, all);

        if (ranked == null || ranked.isEmpty()) {
            // no ranking available, try to pick a single top candidate quickly
            Representative top = null;
            if (!all.isEmpty()) top = all.get(0);
            return new FPTPBallot(top);
        }

        // Return RCV ballot carrying full ranking
        return new RCVBallot(ranked);
    }
}
