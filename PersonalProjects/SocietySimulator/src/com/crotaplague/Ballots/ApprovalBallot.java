package com.crotaplague.Ballots;

import com.crotaplague.Citizen;
import com.crotaplague.Representative;
import com.crotaplague.VotingUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApprovalBallot implements Ballot {
    private final List<Representative> approved;

    public ApprovalBallot() {
        this.approved = new ArrayList<>();
    }

    public ApprovalBallot(List<Representative> approved) {
        this.approved = new ArrayList<>(approved);
    }

    /** Add a candidate to the approved list if not already approved */
    public void approveOf(Representative r) {
        if (r != null && !approved.contains(r)) {
            approved.add(r);
        }
    }

    /** Return an unmodifiable view of approved candidates */
    public List<Representative> getApproved() {
        return Collections.unmodifiableList(approved);
    }

    @Override
    public BallotType getType() {
        return BallotType.APPROVAL;
    }

    /** Compare two candidates based on approvals */
    @Override
    public Representative chooseWinnerBetween(Representative a, Representative b) {
        boolean aOk = approved.contains(a);
        boolean bOk = approved.contains(b);
        if (aOk && !bOk) return a;
        if (bOk && !aOk) return b;
        return null; // tie or neither approved
    }

    /**
     * Create an ApprovalBallot from a Citizen.
     * Example: approve all candidates the citizen prefers (0/1)
     */
    public static ApprovalBallot fromCitizen(Citizen voter, List<Representative> candidates) {
        ApprovalBallot ballot = new ApprovalBallot();
        if (voter == null || candidates == null || candidates.isEmpty()) return ballot;

        // Use the same scoring function as STAR
        Map<Representative, Double> scores = VotingUtils.scoreCandidatesForVoter(voter, candidates);
        if (scores.isEmpty()) return ballot;

        // Approve candidates above threshold (example: 0.5 normalized)
        for (Representative r : candidates) {
            double score = scores.getOrDefault(r, 0.0); // 0..1
            if (score > 0.5) {
                ballot.approveOf(r);
            }
        }
        return ballot;
    }

}
