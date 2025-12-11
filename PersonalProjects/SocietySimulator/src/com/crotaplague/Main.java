package com.crotaplague;

import com.crotaplague.Ballots.ElectionMethod;
import com.crotaplague.config.CustomCitizensLoader;
import com.crotaplague.config.CustomStatesLoader;
import com.crotaplague.config.SimulationSettings;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;

public class Main {
    final public static String path = "C:\\Users\\dsyme\\Downloads\\SocietySimulation\\SocietySimulation\\src\\PartyIdeologies.txt";
    final public static String state_path = "C:\\Users\\dsyme\\Downloads\\SocietySimulation\\SocietySimulation\\src\\StateNames.txt";
    public static Country country;
    // Instance-based cache instead of static methods
    public static final NationalPopularityCache nationalPopularity = new NationalPopularityCache();
    public static void main(String[] args) {
        ValueAssigner.init();
        Country.init();
        // Try to load optional configs
        tryLoadAndApplyConfigs();
        Random rand = new Random(System.currentTimeMillis());
        Map<String, ElectionMethod> helloThere = new HashMap<>(Map.of("FPTP", VotingUtils::runFPTP, "Strategic FPTP", VotingUtils::runFPTPStrategic, "National Strategic FPTP", (cands, voters) -> nationalPopularity.runFPTPNationalStrategic(cands, voters),
                "Star", VotingUtils::runStarElection, "RCV", VotingUtils::runRCVElection, "Approval", VotingUtils::runApprovalElection));
        int partyCount = 10; // rand.nextInt(15) + 2;

        List<Representative> finale = null;
        boolean completed = false;
        int tracker = 0;
        // Parallel sweep across number of parties and chart MSE curves
        PartySweepParallel.runByChamberSize();
        /*country = new Country();
        country.setPartyCount(partyCount);
        country.runElection();
        nationalPopularity.init();
        finale = country.getChamber();
        System.out.println("This starts here");
        Map<String, Double> d = OptimizedVoteUtils.computePopularPartyShares(Country.posExists, country.getCitizens(), VotingUtils.FavoriteMode.TOP_RANK, true);
        System.out.println("This ends here");


        MyChartDisplay.addChart("STV Election", finale);
        double generalKenobi = VotingUtils.computeMSEBetweenSeatsAndPopularVote(finale, d);
        System.out.println("MSE for STV is: " + generalKenobi);
        for(Map.Entry<String, ElectionMethod> youAreABoldOne : helloThere.entrySet()){
            System.out.println("Starting simulation of: " + youAreABoldOne.getKey());
            List<Representative> killHim = country.simulate(youAreABoldOne.getValue());
            generalKenobi = VotingUtils.computeMSEBetweenSeatsAndPopularVote(killHim, d);
            System.out.println("MSE for " + youAreABoldOne.getKey() + " is: " + generalKenobi);
            MyChartDisplay.addChart(youAreABoldOne.getKey(), killHim);
        }

        System.out.println("Starting simulation of list proportional");
        List<Representative> reps = country.simulateProportional();
        generalKenobi = VotingUtils.computeMSEBetweenSeatsAndPopularVote(reps, d);
        System.out.println("MSE for list proportional is: " + generalKenobi);
        MyChartDisplay.addChart("List Proportional", reps);

        MyChartDisplay.addChart("Real Proportions", d);

        MyChartDisplay.showCharts();

        /*while(country.getLaws().isEmpty()){
            country.monthOfDiscussion();
        }
        System.out.print(country.getLaws().get(0).toString() + " final");

        double citizenAverageBias = 0;
        for(crotaplague.com.Citizen c : country.getCitizens()){
            citizenAverageBias += c.politicalBias;
        }
        citizenAverageBias /= country.getCitizens().size();
        System.out.println("crotaplague.com.Citizen average bias: " + citizenAverageBias);
        */
    }

    private static void tryLoadAndApplyConfigs() {
        try {
            java.io.File cfgDir = new java.io.File("config");
            if (!cfgDir.exists()) {
                cfgDir = new java.io.File("C:\\Users\\dsyme\\Downloads\\SocietySimulation\\SocietySimulation\\config");
            }
            if (!cfgDir.exists()) return; // nothing to do

            java.io.File settingsFile = new java.io.File(cfgDir, "settings.properties");
            java.io.File statesFile = new java.io.File(cfgDir, "custom_states.json");
            java.io.File citizensFile = new java.io.File(cfgDir, "custom_citizens.json");

            SimulationSettings settings = SimulationSettings.load(settingsFile);

            // Create country without auto population
            country = new Country(false);

            // Apply settings (optional)
            if (settings.minVotingAge > 0) Country.minVotingAge = settings.minVotingAge;
            if (settings.maxVotingAge > 0) Country.maxVotingAge = settings.maxVotingAge;
            if (settings.chamberSize > 0) country.setChamberSize(settings.chamberSize);
            if (settings.stateCount > 0) country.setStateCount(settings.stateCount);
            if (settings.citizenCount > 0) country.setCitizenCount(settings.citizenCount);
            if (settings.partyCount > 0) country.setPartyCount(settings.partyCount);

            // Build states: custom or default
            List<State> states = new ArrayList<>();
            if (settings.useCustomStates && statesFile.exists()) {
                List<CustomStatesLoader.StateSpec> specs = CustomStatesLoader.load(statesFile);
                for (CustomStatesLoader.StateSpec spec : specs) {
                    State s = new State(country, spec.name == null ? "" : spec.name);
                    if (spec.counties != null && !spec.counties.isEmpty()) {
                        int id = 0;
                        for (String cname : spec.counties) {
                            s.addCounty(new County(id++, cname, s));
                        }
                    } else if (spec.countyCount != null && spec.countyCount > 0) {
                        for (int i = 0; i < spec.countyCount; i++) {
                            s.addCounty(new County(i, "" + i, s));
                        }
                    }
                    states.add(s);
                }
                if (!states.isEmpty()) country.resetStates(states);
            }

            // If no custom states provided, ensure default country has structure
            if (country.getStates().isEmpty()){
                country.generatePopulationAndRepresentatives();
                return; // keep default flow
            }

            // Load custom citizens (optional)
            int createdCitizens = 0;
            int preferredReps = 0;
            if (settings.useCustomCitizens && citizensFile.exists()){
                List<CustomCitizensLoader.CitizenSpec> cspecs = CustomCitizensLoader.load(citizensFile);
                Map<String, State> stateByName = new HashMap<>();
                for (State s : country.getStates()) stateByName.put(s.getName().toLowerCase(), s);

                // Build global index of county name occurrences for unique-county rule
                Map<String, List<County>> countyIndex = new HashMap<>();
                for (State s : country.getStates()){
                    for (County c : s.getCounties()){
                        countyIndex.computeIfAbsent(c.getName().toLowerCase(), k -> new ArrayList<>()).add(c);
                    }
                }

                Random rnd = new Random();
                for (CustomCitizensLoader.CitizenSpec spec : cspecs){
                    int repeat = spec.count == null ? 1 : Math.max(1, spec.count);
                    for (int i = 0; i < repeat; i++){
                        Citizen c = new Citizen(country);
                        if (spec.age != null) c.setAge(spec.age);
                        if (spec.polarization != null) c.setBias(spec.polarization);
                        if (spec.extremism != null) c.setExtremism(spec.extremism);
                        if (spec.values != null){
                            for (CustomCitizensLoader.ValueSpec vs : spec.values){
                                double w = vs.weight == null ? 1.0 : vs.weight;
                                // Map weight (0.0..1.0) to a 0..10 intensity for Value constructor
                                int intensity = (int)Math.round(Math.max(0, Math.min(10, w * 10)));
                                c.addValue(new Value(vs.name, intensity, 0));
                            }
                        }

                        State s = null;
                        County assignedCounty = null;
                        if (spec.state != null && !spec.state.isEmpty()){
                            s = stateByName.get(spec.state.toLowerCase());
                        }
                        if (spec.county != null && !spec.county.isEmpty()){
                            if (s != null){
                                assignedCounty = s.findCountyByName(spec.county);
                            } else {
                                List<County> matches = countyIndex.getOrDefault(spec.county.toLowerCase(), List.of());
                                if (matches.size() == 1){
                                    assignedCounty = matches.get(0);
                                    s = assignedCounty.getState();
                                } else if (matches.size() > 1){
                                    System.err.println("Ambiguous county name '" + spec.county + "' across multiple states; citizen left unassigned to county");
                                }
                            }
                        }

                        if (s == null){
                            List<State> all = country.getStates();
                            s = all.get(rnd.nextInt(all.size()));
                        }

                        // Add citizen to country/state
                        country.addCitizen(s, c);

                        // County assignment if decided
                        if (assignedCounty != null){
                            c.setCounty(assignedCounty);
                            assignedCounty.addCitizen(c);
                        }

                        // Rep toggle
                        if (spec.representative){
                            c.setRepresentativePreferred(true);
                            preferredReps++;
                        }
                        createdCitizens++;
                    }
                }

                // Distribute remaining unassigned citizens across existing counties within their states
                for (State s : country.getStates()){
                    s.distributeCitizensAcrossExistingCounties();
                }
            }

            // If settings specify citizenCount greater than created, top up randomly
            if (settings.citizenCount > 0 && createdCitizens < settings.citizenCount){
                country.addRandomCitizens(settings.citizenCount - createdCitizens);
                for (State s : country.getStates()){
                    if (s.getCounties().isEmpty()){
                        s.citizensToCounties(1);
                    } else {
                        s.distributeCitizensAcrossExistingCounties();
                    }
                }
            }

            // Parties: replace if provided, otherwise use default updateParties
            if (!settings.parties.isEmpty()){
                List<Party> list = new ArrayList<>();
                for (SimulationSettings.PartySpec ps : settings.parties){
                    Party p = new Party(ps.name, ps.bias);
                    for (String vname : ps.values){
                        p.addValue(new Value(vname));
                    }
                    list.add(p);
                }
                country.replaceParties(list);

                // if settings.partyCount > provided, top up from file
                if (settings.partyCount > list.size()){
                    Party[] possible = RandomScripts.loadPartiesFromFile();
                    List<Party> extras = new ArrayList<>();
                    for (Party p : possible){
                        if (country.getPartiesMap().containsKey(p.getName().toLowerCase())) continue;
                        extras.add(p);
                        if (list.size() + extras.size() >= settings.partyCount) break;
                    }
                    List<Party> combined = new ArrayList<>(country.getParties());
                    combined.addAll(extras);
                    country.replaceParties(combined);
                }
            } else {
                country.updateParties();
            }

            // Prepare reps; enforce representativeCount soft target if provided
            country.prepareRepresentatives();
            if (settings.representativeCount > 0){
                int currentPref = 0;
                for (State s : country.getStates()){
                    for (Citizen c : s.getCitizens()) if (c.isRepresentativePreferred()) currentPref++;
                }
                if (currentPref < settings.representativeCount){
                    int need = settings.representativeCount - currentPref;
                    Random r = new Random();
                    List<Citizen> pool = new ArrayList<>();
                    for (State s : country.getStates()) pool.addAll(s.getCitizens());
                    Collections.shuffle(pool, r);
                    for (Citizen c : pool){
                        if (!c.isRepresentativePreferred()){
                            c.setRepresentativePreferred(true);
                            if (--need == 0) break;
                        }
                    }
                    country.prepareRepresentatives(); // rebuild pools including new prefs
                }
            }

        } catch (Exception ex){
            ex.printStackTrace();
        }
    }



    public static void runPartySweepAndGraph() {

        ValueAssigner.init();
        Random rand = new Random(System.currentTimeMillis());

        // preserve order so chart lines are predictable
        Map<String, ElectionMethod> methods = new LinkedHashMap<>(
                Map.of(
                        "FPTP", VotingUtils::runFPTP,
                        "Strategic FPTP", VotingUtils::runFPTPStrategic,
                        "National Strategic FPTP", (cands, voters) -> nationalPopularity.runFPTPNationalStrategic(cands, voters),
                        "Star", VotingUtils::runStarElection,
                        "RCV", VotingUtils::runRCVElection,
                        "Approval", VotingUtils::runApprovalElection
                )
        );

        // x-axis points (number of parties) with adaptive stepping
        final int MAX_PARTIES = 500;
        List<Integer> partyCounts = new ArrayList<>();
        int p = 2;
        while (p <= MAX_PARTIES) {
            partyCounts.add(p);
            int step = computeStep(p);
            p += step;
        }

        // prepare series storage: for each method name + STV + List Proportional
        Map<String, List<Double>> series = new LinkedHashMap<>();
        series.put("STV", new ArrayList<>());
        for (String name : methods.keySet()) series.put(name, new ArrayList<>());
        series.put("List Proportional", new ArrayList<>());

        System.out.println("Running sweep for party counts: " + partyCounts.size() + " points");

        // iterate party counts
        for (int parties : partyCounts) {
            try {

                // create and run a fresh country for this party count
                country = new Country();
                country.setPartyCount(parties);
                country.runElection();

                nationalPopularity.init();

                // compute "true" popular shares for MSE target
                Map<String, Double> popularShares = OptimizedVoteUtils.computePopularPartyShares(
                        country.posExists,
                        country.getCitizens(),
                        VotingUtils.FavoriteMode.TOP_RANK,
                        true
                );

                // STV baseline -- from the chamber after runElection()
                List<Representative> stvSeats = country.getChamber();
                double mseStv = VotingUtils.computeMSEBetweenSeatsAndPopularVote(stvSeats, popularShares);
                series.get("STV").add(mseStv);

                // each other method by simulating using their ElectionMethod
                for (Map.Entry<String, ElectionMethod> e : methods.entrySet()) {
                    String name = e.getKey();
                    ElectionMethod method = e.getValue();
                    try {
                        List<Representative> seats = country.simulate(method);
                        double mse = VotingUtils.computeMSEBetweenSeatsAndPopularVote(seats, popularShares);
                        series.get(name).add(mse);
                    } catch (Exception innerEx) {
                        innerEx.printStackTrace();
                        series.get(name).add(Double.NaN);
                    }
                }

                // List proportional
                try {
                    List<Representative> proportionalSeats = country.simulateProportional();
                    double mseProp = VotingUtils.computeMSEBetweenSeatsAndPopularVote(proportionalSeats, popularShares);
                    series.get("List Proportional").add(mseProp);
                } catch (Exception innerEx) {
                    innerEx.printStackTrace();
                    series.get("List Proportional").add(Double.NaN);
                }

                System.out.printf("Done parties=%3d  STV=%.6f\n", parties, mseStv);

            } catch (Exception ex) {
                ex.printStackTrace();
                for (List<Double> vals : series.values()) vals.add(Double.NaN);
            }
        }

        // convert x and each y-series to double[] and add to MyLineChartDisplay
        double[] xs = new double[partyCounts.size()];
        for (int i = 0; i < partyCounts.size(); i++) xs[i] = partyCounts.get(i);

        MyLineChartDisplay.clear();
        for (Map.Entry<String, List<Double>> kv : series.entrySet()) {
            String name = kv.getKey();
            List<Double> yList = kv.getValue();

            double[] ys = new double[yList.size()];
            for (int i = 0; i < yList.size(); i++) {
                Double v = yList.get(i);
                ys[i] = (v == null) ? Double.NaN : v;
            }

            MyLineChartDisplay.addLine(name, xs, ys);
        }

        MyLineChartDisplay.show("MSE vs Number of Parties (2 → " + MAX_PARTIES + ")");
    }


    private static int computeStep(int parties) {
        if (parties < 10) return 1;
        if (parties < 50) return 2;
        if (parties < 100) return 5;
        if (parties < 200) return 10;
        return 20;
    }


}