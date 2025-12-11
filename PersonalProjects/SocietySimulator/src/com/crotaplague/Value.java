package com.crotaplague;

public class Value {
    private final String name;        // e.g. "isolationism", "fund healthcare initiative"
    private final int polarization;   // 0-10: how much weight this value has in decisions
    private final int opinion;        // -10 to +10: how extreme/lenient the stance is

    public Value(String name, int polarization, int opinion) {
        this.name = name.toLowerCase(); // normalize
        this.polarization = Math.max(0, Math.min(10, polarization)); // clamp 0-10
        this.opinion = Math.max(-10, Math.min(10, opinion)); // clamp -10 to +10
    }
    public Value(String name){
        this(name, -1, -1);
    }

    public String getName() { return name; }
    public int getPolarization() { return polarization; }
    public int getOpinion() { return opinion; }

    @Override
    public String toString() {
        return String.format("%s (importance=%d, stance=%d)", name, polarization, opinion);
    }
    @Override
    public boolean equals(Object o){
        if(!(o instanceof Value v)){
            return false;
        }
        return v.name.equalsIgnoreCase(this.name);
    }
    @Override
    public int hashCode(){
        return name.hashCode();
    }
}