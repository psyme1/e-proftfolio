package com.crotaplague.Ballots;

import com.crotaplague.Citizen;
import com.crotaplague.Party;
import com.crotaplague.Representative;

import java.util.List;

@FunctionalInterface
public interface ElectionMethod {
    Representative run(List<Representative> candidates, List<Citizen> voters);
}
