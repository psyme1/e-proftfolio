package com.crotaplague.Ballots;

import com.crotaplague.Party;
import com.crotaplague.Representative;

public class ListProportional implements Ballot {

    private final Party party;
    private final double weight;

    public ListProportional(Party party) {
        this.party = party;
        this.weight = 1.0;
    }

    public ListProportional(Party party, double weight) {
        this.party = party;
        this.weight = weight;
    }

    public Party getParty() {
        return party;
    }

    public double getWeight() {
        return weight;
    }

    public boolean votesForParty(Party p) {
        return party != null && party.equals(p);
    }

    @Override
    public Representative chooseWinnerBetween(Representative a, Representative b) {
        // PR is party based, not candidate based
        if (a != null && a.getParty() == party) return a;
        if (b != null && b.getParty() == party) return b;
        return null;
    }

    @Override
    public BallotType getType() {
        return BallotType.LIST_PROPORTIONAL;
    }
}

