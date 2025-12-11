package com.crotaplague.Ballots;

import com.crotaplague.Representative;

public class FPTPBallot implements Ballot{
    private Representative vote;
    public FPTPBallot(Representative r){
        this.vote = r;
    }
    public Representative getVote(){return this.vote;}
    @Override
    public BallotType getType() { return BallotType.FPTP; }
    @Override
    public Representative chooseWinnerBetween(Representative a, Representative b) {
        return a.equals(vote) ? a : (b.equals(vote) ? b : null);
    }
    public void setVote(Representative r) {
        this.vote = r;
    }
    public boolean votesFor(Representative r) {
        return vote != null && vote.equals(r);
    }
}
