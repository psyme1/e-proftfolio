package com.crotaplague;

import java.util.*;
import java.util.stream.Collectors;

public class State {
    private Representative rep;
    private List<County> counties;
    private String name;
    private Country country;
    private List<Citizen> citizens;

    public State(Country country, String name){
        citizens = new ArrayList<>();
        counties = new ArrayList<>();
        this.country = country;
        this.name = name;
    }

    public List<County> getCounties(){
        return this.counties;
    }
    public void clearCounties(){ this.counties.clear(); }
    public void addCounty(County c){ this.counties.add(c); }
    public County findCountyByName(String countyName){
        for(County c : counties){
            if(c.getName().equalsIgnoreCase(countyName)) return c;
        }
        return null;
    }
    public void pickReps(List<Representative> reps){
        List<Representative> re = reps.stream().filter(representative -> {return representative.county.getState().equals(this);}).collect(Collectors.toList());
        for(County co : counties){
            co.pickReps(re);
        }
        re = reps.stream().filter(representative -> {return representative.getDesire() == Desire.CHAMBERLAIN;}).collect(Collectors.toList());

        sortPickRepresentative(re);
    }

    public void sortPickRepresentative(List<Representative> re) {
        List<Party> parties = new ArrayList<>(country.getParties());
        parties.sort(RandomScripts.unstableComparator(true));
        Party[] p = new Party[parties.size()];
        for(Representative r : re){
            Party pae = RandomScripts.search(r.getCitizen(), parties.toArray(p));
            pae.addOption(r);
            r.setParty(pae);
        }

        for(Party party : parties){
            int tracker = -1;
            if(party.getOptions() == null || party.getOptions().isEmpty()) continue;
            List<Representative> reps = new ArrayList<>(party.getOptions());
            for(int i = 0; i < reps.size(); i++){
                Representative r = reps.get(i);
                if(r == null) continue;
                if(tracker == -1 ||
                        Math.abs(party.getBias() - r.getBias()) < Math.abs(party.getBias() - reps.get(i).getBias())) tracker = i;
            }
            party.getOptions().get(tracker).setParty(party);
        }
    }
    public String getName(){return this.name;}
    public synchronized void addCitizen(Citizen c){citizens.add(c);}
    public List<Citizen> getCitizens(){return this.citizens;}
    public void citizensToCounties(Integer integer){

        integer = Math.max(1, integer);

        for(int i = 0; i < integer; i++){
            counties.add(new County(i, this));
        }
        for(int i = 0; i < citizens.size(); i++){
            Citizen c = citizens.get(i);
            County addTo = counties.get(i % counties.size());
            c.setCounty(addTo);
            addTo.addCitizen(c);
        }
    }

    public void distributeCitizensAcrossExistingCounties(){
        if (counties.isEmpty()) return;
        int idx = 0;
        for (Citizen c : citizens){
            if (c.getCounty() != null) continue;
            County addTo = counties.get(idx % counties.size());
            idx++;
            c.setCounty(addTo);
            addTo.addCitizen(c);
        }
    }
}
