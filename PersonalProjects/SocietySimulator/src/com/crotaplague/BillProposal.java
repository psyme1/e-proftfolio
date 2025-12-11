package com.crotaplague;

import java.util.concurrent.atomic.LongAdder;

public class BillProposal implements Comparable<BillProposal>{
    private LongAdder yay;
    private LongAdder nay;
    private final Value value;
    private boolean hasDiscussed;
    private final boolean isOfPrioritySlot;
    private Law law;
    private Party proposer;

    public BillProposal(Value value, boolean isOfPrioritySlot) {
        this.isOfPrioritySlot = isOfPrioritySlot;
        yay = new LongAdder();
        nay = new LongAdder();
        this.value = value;
        hasDiscussed = false;
        law = Law.fromProposal(value);
    }

    public BillProposal(Value value){
        this(value, false);
    }

    public void discuss(){this.hasDiscussed = true;}
    public void yay(){yay.add(1);}
    public long getYay(){return yay.sum();}
    public void nay(){nay.add(1);}
    public long getNay(){return nay.sum();}
    public Value getValue(){return this.value;}
    public boolean hasDiscussed(){return hasDiscussed;}
    public Law getLaw(){return this.law;}
    public Party getProposer(){return this.proposer;}
    public void setProposer(Party p){this.proposer = p;}

    @Override
    public int compareTo(BillProposal o) {
        int thisBoost = boolAsInt(isOfPrioritySlot) + boolAsInt(hasDiscussed);
        int oBoost = boolAsInt(o.isOfPrioritySlot) + boolAsInt(o.hasDiscussed);
        return Integer.compare(oBoost, thisBoost);
    }

    private static int boolAsInt(boolean b){
        if(b) return 1;
        else return 0;
    }

    @Override
    public String toString(){
        return law.toString();
    }

}
