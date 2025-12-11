package com.crotaplague;

public final class ValueProfile {
    // importance scaled 0.0..1.0 (from polarization 0..10)
    public final double[] importance;
    // opinions in -10..+10 (same scale as Value.opinion)
    public final double[] opinion;

    public ValueProfile(int issueCount) {
        this.importance = new double[issueCount];
        this.opinion = new double[issueCount];
    }
}
