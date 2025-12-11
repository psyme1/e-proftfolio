package com.crotaplague;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Representative extends Citizen {
    private Party party;
    private Citizen c;
    private boolean hasPriorityBill;
    public boolean isUnsure = false;
    public String name;

    /***
     * creates a representative from a given citizen
     * @param c the citizen to generate the representative from
     */
    public Representative(Citizen c){
        super(c.getCountry());
        Random rand = new Random(System.currentTimeMillis());
        this.politicalBias = c.getBias();
        desire = Desire.valueOf(rand.nextInt(2));
        this.age = c.age;
        if(this.age < 35){
            desire = Desire.REPRESENTATIVE;
        }
        this.county = c.county;
        this.c = c;
        this.hasPriorityBill = true;
        this.values =  new ArrayList<>(c.values);
        this.name = RandomScripts.getRandomName();
    }
    public Representative(Citizen c, Desire desire){
        this(c);
        this.desire = desire;
    }

    public Citizen getCitizen(){return this.c;}
    public void setParty(Party p){this.party = p;}
    private Desire desire;
    public Desire getDesire(){
        return this.desire;
    }

    @Override
    public String toString(){
        return party.toString() + " " + c.toString();
    }
    public Party getParty(){return this.party;}
    public boolean hasPriorityBill(){return this.hasPriorityBill;}
    public void usePriorityBill(){this.hasPriorityBill = false;}

    public boolean supportsBill(BillProposal p) {
        if (p == null || p.getValue() == null) return false;
        Value behind = p.getValue();
        if (behind.getName() == null) return false;

        String target = behind.getName().toLowerCase();

        Party proposerParty = null;
        try {
            proposerParty = p.getProposer();
        } catch (Throwable ignored) {
            proposerParty = null;
        }

        for (Value v : this.values) {
            if (v == null) continue;
            if (v.getName().equalsIgnoreCase(target)) {
                return decideSupportByValue(v, behind, false, proposerParty);
            }
        }

        if (this.party != null && this.party.getValues() != null) {
            for (Value pv : this.party.getValues()) {
                if (pv == null) continue;
                if (pv.getName().equalsIgnoreCase(target)) {
                    return decideSupportByValue(pv, behind, true, proposerParty);
                }
            }
        }

        int opinion = behind.getOpinion();
        double extremity = Math.abs(opinion) / 10.0;

        double baseChance = 0.30 * (1.0 - extremity) + 0.08;

        if (proposerParty != null && this.party != null && proposerParty.equals(this.party)) {
            double boost = (1.0 - extremity) * 0.55 + 0.15;
            baseChance = Math.min(1.0, baseChance + boost);
        } else {
            isUnsure = true;
            boolean b = discussWithParty(p);
            if (b) {
                isUnsure = false;
                return true;
            }
            baseChance *= 0.65;
        }

        if (proposerParty != null) {
            int proposerBias = proposerParty.getBias();
            double diffBias = Math.abs(proposerBias - this.politicalBias);
            double biasSimilarity = 1.0 - Math.min(1.0, diffBias / 20.0);
            double biasEffect = (biasSimilarity * 0.20) - 0.05;
            baseChance = Math.min(1.0, Math.max(0.0, baseChance + biasEffect));
        }

        isUnsure = false;
        return ThreadLocalRandom.current().nextDouble() < baseChance;
    }

    public boolean evaluateSupportWithoutDiscussion(BillProposal p) {
        if (p == null || p.getValue() == null) return false;
        Value behind = p.getValue();
        if (behind.getName() == null) return false;

        String target = behind.getName().toLowerCase();

        Party proposerParty = null;
        try {
            proposerParty = p.getProposer();
        } catch (Throwable ignored) {
            proposerParty = null;
        }

        for (Value v : this.values) {
            if (v == null) continue;
            if (v.getName().equalsIgnoreCase(target)) {
                return decideSupportByValue(v, behind,false, proposerParty);
            }
        }

        if (this.party != null && this.party.getValues() != null) {
            for (Value pv : this.party.getValues()) {
                if (pv == null) continue;
                if (pv.getName().equalsIgnoreCase(target)) {
                    return decideSupportByValue(pv, behind,true, proposerParty);
                }
            }
        }

        int opinion = behind.getOpinion();
        double extremity = Math.abs(opinion) / 10.0;

        double baseChance = 0.30 * (1.0 - extremity) + 0.08;

        if (proposerParty != null && this.party != null && proposerParty.equals(this.party)) {
            double boost = (1.0 - extremity) * 0.55 + 0.15;
            baseChance = Math.min(1.0, baseChance + boost);
        } else {
            baseChance *= 0.65;
        }

        if (proposerParty != null) {
            int proposerBias = proposerParty.getBias();
            double diffBias = Math.abs(proposerBias - this.politicalBias);
            double biasSimilarity = 1.0 - Math.min(1.0, diffBias / 20.0);
            double biasEffect = (biasSimilarity * 0.20) - 0.05;
            baseChance = Math.min(1.0, Math.max(0.0, baseChance + biasEffect));
        }

        return ThreadLocalRandom.current().nextDouble() < baseChance;
    }

    private boolean decideSupportByValue(Value repOrPartyValue, Value proposalValue, boolean isPartyFallback, Party proposerParty) {
        int repOpinion = repOrPartyValue.getOpinion();
        int propOpinion = proposalValue.getOpinion();
        int diff = Math.abs(repOpinion - propOpinion);
        int pol = repOrPartyValue.getPolarization();
        if (pol < 0) pol = 5;

        if (diff == 0) return true;

        double repNorm = repOpinion / 10.0;
        double propNorm = propOpinion / 10.0;
        double absRep = Math.abs(repNorm);
        double absProp = Math.abs(propNorm);

        double strictFactor = 0.7 + (pol / 6.0);
        if (isPartyFallback) strictFactor *= 0.9;
        double lambda = 8.0;
        double decay = Math.exp(- (diff * strictFactor) / lambda);

        double prob = 0.11 + 0.7 * decay;

        double repModerateness = 1.0 - absRep;
        double propModerate = 1.0 - absProp;

        double moderateBoost = repModerateness * propModerate * 0.35;
        prob = Math.min(1.0, prob + moderateBoost);

        double sameSideProduct = repNorm * propNorm;
        if (sameSideProduct > 0.0) {
            double sameSideAffinity = (absRep * 0.25) + (absProp * 0.35);
            double extraWhenPropMoreExtreme = Math.max(0.0, absProp - absRep);
            double extremeBoost = sameSideAffinity * 0.25 + extraWhenPropMoreExtreme * 0.35;
            prob = Math.min(1.0, prob + extremeBoost);
        }

        if (proposerParty != null && this.party != null && proposerParty.equals(this.party)) {
            prob = Math.min(1.0, prob + 0.22);
        }

        if (proposerParty != null) {
            int proposerBias = proposerParty.getBias();
            double diffBias = Math.abs(proposerBias - this.politicalBias);
            double biasSimilarity = 1.0 - Math.min(1.0, diffBias / 20.0);
            double biasEffect = (biasSimilarity * 0.12) - 0.03;
            prob = Math.min(1.0, Math.max(0.002, prob + biasEffect));
        }

        prob = Math.max(0.002, Math.min(0.99, prob));

        return ThreadLocalRandom.current().nextDouble() < prob;
    }

    public boolean discussWithParty(BillProposal p) {
        int count = 0;
        if (party == null || party.options == null || party.options.isEmpty()) return false;

        List<Representative> order = new ArrayList<>(party.options);
        Collections.shuffle(order);

        int maxToTalk = (int) Math.ceil(0.90 * party.options.size());
        int toTalkTo = randomBetween(1, Math.max(1, maxToTalk));

        int available = 0;
        for (Representative r : order) {
            if (!r.isUnsure) available++;
        }
        if (available == 0) return false;

        toTalkTo = Math.min(toTalkTo, available);

        for (Representative r : order) {
            if (r.isUnsure) continue;
            boolean stance = r.evaluateSupportWithoutDiscussion(p);
            if (stance) count += 2;
            else count -= 1;
            if (--toTalkTo <= 0) break;
        }

        return count > 0;
    }

}
