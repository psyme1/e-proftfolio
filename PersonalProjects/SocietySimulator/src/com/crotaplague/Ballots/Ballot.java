package com.crotaplague.Ballots;

import com.crotaplague.Representative;

public interface Ballot {
    BallotType getType();
    Representative chooseWinnerBetween(Representative a, Representative b);
}
