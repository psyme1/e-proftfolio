package com.crotaplague;

import com.crotaplague.Ballots.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

public class Country {
    private final List<Law> laws;
    private final List<State> states;
    private final List<Citizen> citizens;
    public Map<String, Party> parties;
    private int stateCount = 75;
    private int chamberSize = 750;
    private int countyCount = chamberSize * 2 / 3;
    private int citizenCount;
    private int partyCount = 25;
    private final List<Representative> chamber;
    // Voting age should be configurable via settings
    public static int minVotingAge = 16, maxVotingAge = 122;
    // Shared candidate pool used by ballot ranking. Make it thread-safe
    public final List<Representative> posExists = Collections.synchronizedList(new ArrayList<>());
    public static String[] names;
    // Pre-generated candidate pools to ensure repeatable elections per instantiation
    private final Map<State, List<Representative>> representativeCandidates = new ConcurrentHashMap<>();
    private final Map<State, List<Representative>> chamberlainCandidates = new ConcurrentHashMap<>();

    public Country(){
        this(true);
    }

    // New constructor that controls whether to generate citizens and representatives immediately
    public Country(boolean generatePopulationAndReps){
        laws = new ArrayList<>();
        states = new ArrayList<>();
        parties = new HashMap<>();
        chamber = new ArrayList<>(chamberSize);
        Random rand = new Random(System.currentTimeMillis());
        citizenCount = rand.nextInt(100000) + 200000;
        // Pre-size citizens list; population may be generated later depending on boolean flag
        citizens = new ArrayList<>(citizenCount);

        // Build the fixed structure and optionally the population and representatives
        initializeStructureAndMaybePopulate(generatePopulationAndReps);
    }

    // Generates states, counties, and optionally citizens + representatives
    private void initializeStructureAndMaybePopulate(boolean generatePopulationAndReps){
        // Generate states from names
        List<String> listNames = Arrays.asList(names);
        Collections.shuffle(listNames);
        for (int i = 0; i < stateCount; i++) {
            states.add(new State(this, listNames.get(i)));
        }

        // Generate and shuffle laws text (kept for potential future use)
        String[] generatedLaws = RandomScripts.generateLaws();
        List<String> listLaws = Arrays.asList(generatedLaws);
        Collections.shuffle(listLaws);

        // Always create counties structure after apportionment, but need citizens first to distribute
        if (generatePopulationAndReps) {
            generateCitizens();

            Map<State, Integer> allocation = RandomScripts.apportionByHamilton(states, getCountyCount());
            for (State s : states) {
                s.citizensToCounties(allocation.get(s));
            }

            updateParties();
            // Pre-compute representative candidate pools so repeated elections reuse them
            prepareRepresentatives();
        }
    }

    // Public method to generate population and representatives if they were deferred at construction
    public void generatePopulationAndRepresentatives() {
        if (!citizens.isEmpty()) return; // already generated
        generateCitizens();
        Map<State, Integer> allocation = RandomScripts.apportionByHamilton(states, getCountyCount());
        for (State s : states) {
            s.citizensToCounties(allocation.get(s));
        }
        updateParties();
        prepareRepresentatives();
    }

    // Helper to generate citizens and assign to states
    private void generateCitizens(){
        final int threads = 15;
        final int batchSize = Math.max(1, citizenCount / threads);
        IntStream.range(0, threads).parallel().forEach(t -> {
            List<Citizen> local = new ArrayList<>(batchSize);
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            for (int i = 0; i < batchSize; i++) {
                Citizen c = new Citizen(this);
                State s = states.get(tlr.nextInt(states.size()));
                s.addCitizen(c);
                ValueAssigner.assignValuesToCitizen(c);
                local.add(c);
            }
            synchronized (citizens) {
                citizens.addAll(local);
            }
        });
    }

    // Extracted from runElection: build and cache representative candidate pools per state
    public void prepareRepresentatives(){
        representativeCandidates.clear();
        chamberlainCandidates.clear();
        posExists.clear();

        states.parallelStream().forEach(state -> {
            // Representative candidates based on county/state blocks
            List<VotingBlock> blocks = RandomScripts.createBlocks(state);
            List<Representative> reps = RandomScripts.assignRepresentatives(blocks, RandomScripts.randomMultiplier(), Desire.REPRESENTATIVE);
            // Ensure citizens flagged as preferred are included as candidates
            Set<UUID> existing = new HashSet<>();
            for (Representative r : reps) existing.add(r.getCitizen().id);
            for (Citizen c : state.getCitizens()){
                if (c.isRepresentativePreferred() && !existing.contains(c.id)){
                    reps.add(new Representative(c, Desire.REPRESENTATIVE));
                    existing.add(c.id);
                }
            }
            posExists.addAll(reps);
            state.sortPickRepresentative(reps);
            representativeCandidates.put(state, reps);

            // Chamberlain candidates (single state-wide block)
            VotingBlock block = new VotingBlock(state);
            List<Representative> chCands = RandomScripts.assignRepresentatives(List.of(block), RandomScripts.randomMultiplier(), Desire.CHAMBERLAIN);
            // also include preferred citizens in chamberlain pool
            Set<UUID> existingCh = new HashSet<>();
            for (Representative r : chCands) existingCh.add(r.getCitizen().id);
            for (Citizen c : state.getCitizens()){
                if (c.isRepresentativePreferred() && !existingCh.contains(c.id)){
                    chCands.add(new Representative(c, Desire.CHAMBERLAIN));
                    existingCh.add(c.id);
                }
            }
            posExists.addAll(chCands);
            state.sortPickRepresentative(chCands);
            chamberlainCandidates.put(state, chCands);
        });
    }

    public List<State> getStates(){
        return List.copyOf(states);
    }

    // Configuration helpers
    public void setChamberSize(int size){
        if(size > 0){
            this.chamberSize = size;
            this.countyCount = Math.max(1, chamberSize * 2 / 3);
        }
    }
    public void setStateCount(int count){
        if(count > 0){
            this.stateCount = count;
        }
    }
    public void setCitizenCount(int count){
        if(count > 0){
            this.citizenCount = count;
            // adjust backing capacity if needed
            if (citizens != null) {
                // no-op; ArrayList will grow as needed
            }
        }
    }
    public int getChamberSize(){ return this.chamberSize; }
    public void setCountyCount(int count){ this.countyCount = Math.max(1, count); }

    public void resetStates(List<State> newStates){
        this.states.clear();
        this.states.addAll(newStates);
        this.stateCount = this.states.size();
    }

    public void runElection() {
        // Use a work-stealing pool sized to available processors
        ExecutorService executor = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
        try {
            System.out.print("...");

            // Thread-safe collectors to gather results from parallel tasks
            ConcurrentLinkedQueue<Representative> collectedReps = new ConcurrentLinkedQueue<>();

            // Aggregators for chamberlain stats
            LongAdder chamberlainBiasAdder = new LongAdder();
            LongAdder chamberlainCountAdder = new LongAdder();

            // Submit one task per state
            List<CompletableFuture<Void>> futures = new ArrayList<>(states.size());
            // Ensure we have a fixed set of candidates prepared
            if (representativeCandidates.isEmpty() || chamberlainCandidates.isEmpty()) {
                prepareRepresentatives();
            }

            for (State state : states) {
                CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                    // Create blocks and run representative STV for this state
                    List<VotingBlock> blocks = RandomScripts.createBlocks(state);

                    // Use pre-built candidate pool for representatives
                    List<Representative> r = representativeCandidates.getOrDefault(state, List.of());
                    List<Representative> out = RandomScripts.runStv(blocks, r);

                    // Chamberlain selection for the state (single-block)
                    VotingBlock block = new VotingBlock(state);
                    List<Representative> r2 = chamberlainCandidates.getOrDefault(state, List.of());
                    List<Representative> unneeded = RandomScripts.runStv(List.of(block), r2);

                    // Accumulate chamberlain biases
                    int localBias = 0;
                    for (Representative uwu : unneeded) {
                        localBias += uwu.getBias();
                    }
                    chamberlainBiasAdder.add(localBias);
                    chamberlainCountAdder.add(unneeded.size());

                    // Combine outputs and add to collector in bulk
                    out.addAll(unneeded);
                    collectedReps.addAll(out);
                }, executor);

                futures.add(f);
            }

            // Wait for all per-state tasks to finish
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // compute chamberlain average safely (avoid division by zero)
            double chamberlainAvg = 0.0;
            long andCount = chamberlainCountAdder.sum();
            if (andCount > 0) {
                chamberlainAvg = (double) chamberlainBiasAdder.sum() / (double) andCount;
            }
            System.out.println("Huh, this finishes " + chamberlainAvg + " and: " + andCount);

            // Convert ConcurrentLinkedQueue to ArrayList once (pre-size with constructor)
            List<Representative> reps = new ArrayList<>(collectedReps);

            // Compute partyBias in one pass
            double partyBias = 0.0;
            if (!reps.isEmpty()) {
                for (Representative r : reps) {
                    partyBias += r.getParty().getBias();
                }
                partyBias /= reps.size();
            }
            System.out.println("partyBias: " + partyBias);

            // Reset and add to chamber on single thread for repeatable runs
            chamber.clear();
            chamber.addAll(reps);

            // If VotingUtils.runStarElection does not mutate voters list, avoid extra copy
            @SuppressWarnings("unchecked")
            List<Citizen> chVoters = (List<Citizen>)(List<?>) reps; // Representative extends Citizen

            Representative pm = VotingUtils.runStarElection(chamber, chVoters);
            System.out.println("pm: " + pm);
        } finally {
            // Ensure executor is shut down
            executor.shutdown();
        }
    }

    public static void init(){
        loadNames();
    }
    public static void loadNames(){
        names = RandomScripts.generateNames(Main.state_path);
    }
    public List<Party> getParties(){return new ArrayList<>(parties.values());}
    public List<Representative> getChamber(){return chamber;}
    public int getStateCount(){return this.stateCount;}
    public int getCountyCount(){return this.countyCount;}
    public List<Citizen> getCitizens(){return this.citizens;}
    public int getCitizenCount(){return this.citizenCount;}
    public void setPartyCount(int c){partyCount = c;}
    public void updateParties(){
        List<Party> possible = Arrays.asList(RandomScripts.loadPartiesFromFile());
        Collections.shuffle(possible);
        for(int i = 0; i < partyCount; i++){
            if(i >= possible.size()){
                break;
            }
            Party p = possible.get(i);
            parties.put(p.getName().toLowerCase(), p);
        }
        RandomScripts.loadPartyValuesFromFile(this);
    }
    public Map<String, Party> getPartiesMap(){return parties;}

    public void replaceParties(List<Party> newParties){
        this.parties.clear();
        for (Party p : newParties){
            if (p != null && p.getName() != null)
                this.parties.put(p.getName().toLowerCase(), p);
        }
        this.partyCount = this.parties.size();
    }

    public void addCitizen(State s, Citizen c){
        if (s != null) s.addCitizen(c);
        synchronized (citizens){
            citizens.add(c);
        }
    }

    public void addRandomCitizens(int n){
        if (n <= 0) return;
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++){
            Citizen c = new Citizen(this);
            State s = states.get(tlr.nextInt(states.size()));
            s.addCitizen(c);
            ValueAssigner.assignValuesToCitizen(c);
            citizens.add(c);
        }
    }

    public void monthOfDiscussion(){
        List<Representative> shuffledReps = new  ArrayList<>(chamber);
        Collections.shuffle(shuffledReps);
        PriorityQueue<BillProposal> pq = new PriorityQueue<>();
        shuffledReps.parallelStream().forEach(rep -> {
            List<BillProposal> proposals = new  ArrayList<>();
            if(!rep.values.isEmpty()){
                BillProposal p = new BillProposal(rep.values.get(0), true);
                p.setProposer(rep.getParty());
                proposals.add(p);
            }

            synchronized (pq){
                pq.addAll(proposals);
            }
        });
        long mostVotes = 0;
        while(!pq.isEmpty()){
            BillProposal current = pq.poll();
            shuffledReps.parallelStream().forEach(rep -> {
                if(rep.supportsBill(current)){
                    current.yay();
                }else{
                    current.nay();
                }
            });
            Law l = current.getLaw();
            int required = l.isGovernanceChange() ? chamber.size() * 2 / 3 : chamber.size()/2 + 1;
            if(l.isGovernanceChange()){
                System.out.println(l.getGovernanceChange());
            }
            long yes = current.getYay();
            mostVotes = Math.max(mostVotes, yes);
            if(yes >= required){
                System.out.println(l);
                for(Law law : laws){

                }
                laws.add(l);
            }else{
                if(yes == mostVotes){
                    System.out.println(l + " and " + yes + " and " + required);
                }
                if(yes >= chamber.size()/2 + 1){
                    System.out.println("wow!! " + l);
                }
            }
        }
    }

    public List<Law> getLaws(){
        return this.laws;
    }

    public List<Representative> simulate(ElectionMethod method) {
        return states.parallelStream()
                .flatMap(state -> {

                    List<Representative> localWinners = new ArrayList<>();

                    // ✅ COUNTY-LEVEL REPRESENTATIVE ELECTIONS
                    state.getCounties().parallelStream().forEach(county -> {

                        VotingBlock countyBlock = new VotingBlock(List.of(county));

                        List<Representative> candidates =
                                RandomScripts.assignRepresentatives(
                                        List.of(countyBlock),
                                        RandomScripts.randomMultiplier(),
                                        Desire.REPRESENTATIVE
                                );

                        state.sortPickRepresentative(candidates);

                        List<Citizen> voters = countyBlock.getAllCitizens();

                        if (!voters.isEmpty()) {
                            Representative repWinner = method.run(candidates, voters);
                            if (repWinner != null) {
                                synchronized (localWinners) {
                                    localWinners.add(repWinner);
                                }
                            }
                        }
                    });

                    VotingBlock stateBlock = new VotingBlock(state);

                    List<Citizen> stateVoters = stateBlock.getAllCitizens();

                    if (!stateVoters.isEmpty()) {

                        List<Representative> chamberCands =
                                RandomScripts.assignRepresentatives(
                                        List.of(stateBlock),
                                        RandomScripts.randomMultiplier(),
                                        Desire.CHAMBERLAIN
                                );

                        state.sortPickRepresentative(chamberCands);

                        Representative chWinner =
                                method.run(chamberCands, stateVoters);

                        if (chWinner != null) {
                            synchronized (localWinners) {
                                localWinners.add(chWinner);
                            }
                        }
                    }
                    return localWinners.stream();
                })
                .toList();
    }


    public List<Representative> simulateProportional() {

        if (citizens == null || citizens.isEmpty()) return List.of();
        if (parties == null || parties.isEmpty()) return List.of();

        List<Party> partyList = new ArrayList<>(parties.values());
        int partyCount = partyList.size();

        Map<Party, Integer> partyIndex = new HashMap<>(partyCount * 2);
        // Fallback by party name in case Representative.party is not the same instance
        Map<String, Integer> partyNameIndex = new HashMap<>(partyCount * 2);
        for (int i = 0; i < partyCount; i++) {
            Party p = partyList.get(i);
            partyIndex.put(p, i);
            String name = p.getName();
            if (name != null) partyNameIndex.put(name.toLowerCase(Locale.ROOT), i);
        }

        // ✅ LOCK FREE array
        AtomicIntegerArray voteCounts = new AtomicIntegerArray(partyCount);

        // ✅ TRUE parallel voting
        citizens.parallelStream().forEach(voter -> {

            Representative top = VotingUtils.findTopCandidateForVoterFast(voter);
            if (top == null) return;

            Party party = top.getParty();
            if (party == null) return;

            Integer idx = partyIndex.get(party);
            if (idx == null) {
                String nm = party.getName();
                if (nm != null) idx = partyNameIndex.get(nm.toLowerCase(Locale.ROOT));
            }
            if (idx != null) {
                voteCounts.incrementAndGet(idx);
            }
        });

        // ✅ D’Hondt allocation (already fast)
        int[] seatCounts = new int[partyCount];

        for (int seat = 0; seat < chamberSize; seat++) {
            int bestParty = -1;
            double bestQuotient = -1;

            for (int i = 0; i < partyCount; i++) {
                int votes = voteCounts.get(i);
                int seatsWon = seatCounts[i];

                double quotient = votes / (double) (seatsWon + 1);
                if (quotient > bestQuotient) {
                    bestQuotient = quotient;
                    bestParty = i;
                }
            }

            if (bestParty == -1) break;
            seatCounts[bestParty]++;
        }

        // ✅ Pull from party lists
        List<Representative> results = new ArrayList<>(chamberSize);

        for (int i = 0; i < partyCount; i++) {
            int seats = seatCounts[i];
            if (seats == 0) continue;

            Party party = partyList.get(i);
            List<Representative> options = party.getOptions();
            if (options == null || options.isEmpty()) continue;

            int limit = Math.min(seats, options.size());
            for (int j = 0; j < limit; j++) {
                results.add(options.get(j));
            }
        }

        return results;
    }

}