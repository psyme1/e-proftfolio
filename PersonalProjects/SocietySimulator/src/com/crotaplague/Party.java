package com.crotaplague;

import java.util.*;

public class Party implements Comparable {
    private int bias;
    private final String name;
    List<Representative> options;
    List<Value> values;
    public Party(String str, int bias){
        this.name = str;
        this.bias = bias;
        options = new ArrayList<>();
        values = new ArrayList<>();
    }
    public Party(String name){
        this.name = name;
    }
    public void addOption(Representative r){
        options.add(r);
    }

    @Override
    public int compareTo(Object o) {
        if(!(o instanceof Party)) return 1;
        Party other = (Party) o;
        if(this.bias > other.bias) return 1;
        if(this.bias < other.bias) return -1;
        return 0;
    }
    public int getBias(){return this.bias;}
    public List<Representative> getOptions(){return this.options;}
    public List<Value> getValues(){return this.values;}
    public void addValue(Value v){values.add(v);}

    @Override
    public String toString(){
        return name + " bias: " + bias;
    }
    public String getName(){return this.name;}

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Party p)) return false;
        return p.getName().equalsIgnoreCase(this.name);
    }

    @Override
    public int hashCode(){
        return this.name.toLowerCase().hashCode();
    }

}
