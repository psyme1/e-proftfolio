package com.crotaplague.Ballots;

import com.crotaplague.Representative;
import java.util.List;

public class RCVBallot implements Ballot {

    protected final List<Representative> prefs;

    public RCVBallot(List<Representative> prefs) {
        this.prefs = prefs;
    }

    public List<Representative> getPreferences() {
        return prefs;
    }

    @Override
    public Representative chooseWinnerBetween(Representative a, Representative b) {
        int ia = prefs.indexOf(a);
        int ib = prefs.indexOf(b);

        if (ia >= 0 && ib >= 0) return ia < ib ? a : b;
        if (ia >= 0) return a;
        if (ib >= 0) return b;
        return null;
    }

    @Override
    public BallotType getType() {
        return BallotType.RCV;
    }
}

