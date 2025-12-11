package com.crotaplague.Ballots;

import com.crotaplague.Representative;
import java.util.List;

public class STVBallot extends RCVBallot {

    private final double weight;

    public STVBallot(List<Representative> prefs) {
        super(prefs);
        this.weight = 1.0;
    }

    public STVBallot(List<Representative> prefs, double weight) {
        super(prefs);
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public BallotType getType() {
        return BallotType.STV;
    }
}
