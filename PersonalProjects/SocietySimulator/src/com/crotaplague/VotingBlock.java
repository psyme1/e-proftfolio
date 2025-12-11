package com.crotaplague;

import java.util.ArrayList;
import java.util.List;

class VotingBlock {
    private final List<County> counties;

    public VotingBlock(List<County> counties) {
        this.counties = counties;
    }
    public VotingBlock(State s){
        counties = new ArrayList<>(s.getCounties());
    }
    public List<County> getCounties() {return this.counties;}

    public List<Citizen> getAllCitizens() {
        List<Citizen> citizens = new ArrayList<>();
        for (County c : counties) {
            citizens.addAll(c.getCitizens());
        }
        return citizens;
    }
}
