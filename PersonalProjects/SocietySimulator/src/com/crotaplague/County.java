package com.crotaplague;

import java.util.ArrayList;
import java.util.List;

public class County {

    private State state;
    private Representative rep;
    private int id;
    private String name; // optional human-readable name
    private List<Citizen> citizens;
    public County(int id, State state){
        this.id = id;
        rep = null;
        this.state = state;
        this.name = "" + id;
        citizens = new ArrayList<>();
    }

    public County(int id, String name, State state){
        this.id = id;
        rep = null;
        this.state = state;
        this.name = name;
        citizens = new ArrayList<>();
    }

    public State getState(){return this.state;}
    public void addCitizen(Citizen c){citizens.add(c);}
    public List<Citizen> getCitizens(){return this.citizens;}
    public void pickReps(List<Representative> reps){
        List<Representative> re = reps.stream().filter(representative -> {return representative.county.equals(this) && representative.getDesire() == Desire.REPRESENTATIVE;}).toList();

        re.getFirst().county.state.sortPickRepresentative(re);
    }

    public int getId(){return this.id;}
    public String getName(){return this.name;}
    @Override
    public boolean equals(Object object){
        if(!(object instanceof County)) return false;
        County c = (County) object;
        if(!(this.state.getName().equals(c.getState().getName()))) return false;
        if(!(this.id == c.getId())) return false;
        return true;
    }

}