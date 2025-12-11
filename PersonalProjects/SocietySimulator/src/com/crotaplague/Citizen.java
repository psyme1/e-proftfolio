package com.crotaplague;

import java.util.*;

public class Citizen{
    protected int age;
    protected int politicalBias = 0;
    protected County county = null;
    protected Map<Desire, Representative[]> cVotes;
    protected UUID id;
    protected List<Value> values;
    private static final Random rand = new Random(System.currentTimeMillis());
    private double extremism = 0.25;
    private Country country;
    private boolean representativePreferred = false; // optional toggle from config
    public Citizen(Country c){
        this.country = c;
        Random random = new Random(System.currentTimeMillis());
        id = UUID.randomUUID();
        politicalBias = random.nextInt(100);
        this.age = biasedRandomBetween(Country.minVotingAge, Country.maxVotingAge);
        politicalBias += RandomScripts.biasByAge(age);
        if(politicalBias < 0) politicalBias = 0;
        if(politicalBias > 100) politicalBias = 100;
        cVotes = new HashMap<>();
        values = new ArrayList<>();
        extremism = RandomScripts.normalClamped();
    }
    public Country getCountry(){return this.country;}
    public int getBias(){
        return this.politicalBias;
    }

    public void setCounty(County c){
        this.county = c;
    }
    public int getAge(){return this.age;}

    public County getCounty(){return this.county;}
    public void addValue(Value value){values.add(value);}
    public List<Value> getValues(){return this.values;}
    public double getExtremism(){return this.extremism;}
    public void setExtremism(double e){this.extremism = e;}
    public void setBias(int b){this.politicalBias = Math.max(0, Math.min(100, b));}
    public void setAge(int a){this.age = a;}
    public void setRepresentativePreferred(boolean flag){this.representativePreferred = flag;}
    public boolean isRepresentativePreferred(){return this.representativePreferred;}

    @Override
    public String toString(){
        return "age: " + getAge() + " bias: " + getBias();
    }

    public static int randomBetween(int a, int b) {
        int lower = Math.min(a, b);
        int upper = Math.max(a, b);
        return lower + rand.nextInt(upper - lower + 1);
    }

    public Value valueByName(String name){
        for(Value v : values){
            if(v.getName().equalsIgnoreCase(name)) return v;
        }
        return null;
    }

    public static int biasedRandomBetween(int a, int b) {
        return biasedRandomBetween(a, b, 1e-6);
    }

    public static int biasedRandomBetween(int a, int b, double tailProbability) {
        int lower = Math.min(a, b);
        int upper = Math.max(a, b);
        if (lower == upper) return lower;
        if (tailProbability <= 0 || Double.isNaN(tailProbability)) tailProbability = 1e-6;
        double range = (double)(upper - lower);

        double lambda = -Math.log(tailProbability) / range;

        double u = rand.nextDouble();
        double t = -Math.log(1.0 - u) / lambda;

        int value = lower + (int)Math.floor(t);
        if (value < lower) value = lower;
        if (value > upper) value = upper;

        return value;
    }

}
