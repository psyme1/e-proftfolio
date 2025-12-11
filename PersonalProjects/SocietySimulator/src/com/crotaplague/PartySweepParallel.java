package com.crotaplague;

import com.crotaplague.Ballots.ElectionMethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Runs a parallel sweep over number of parties (2 → 700) and charts how
 * the MSE between seats and popular vote changes for each voting method.
 *
 * Notes on concurrency:
 * - Some legacy utilities read Main.country internally. To avoid race conditions,
 *   we perform those sections under a synchronized block while setting Main.country
 *   to the Country instance for that task. This preserves correctness while still
 *   structuring the sweep as a parallel loop.
 */
public final class PartySweepParallel {

    private PartySweepParallel() {}

    public static void run() {
        ValueAssigner.init();

        // Base methods excluding National Strategic (that one will be rebound per-task to a local cache instance)
        List<Map.Entry<String, ElectionMethod>> baseMethods = new ArrayList<>();
        baseMethods.add(Map.entry("FPTP", VotingUtils::runFPTP));
        baseMethods.add(Map.entry("Strategic FPTP", VotingUtils::runFPTPStrategic));
        baseMethods.add(Map.entry("Star", VotingUtils::runStarElection));
        baseMethods.add(Map.entry("RCV", VotingUtils::runRCVElection));
        baseMethods.add(Map.entry("Approval", VotingUtils::runApprovalElection));

        final int MAX_PARTIES = 700;
        List<Integer> partyCounts = new ArrayList<>();
        for (int p = 2; p <= MAX_PARTIES; p += computeStep(p)) partyCounts.add(p);

        // Prepare result container: for each parties value -> method name -> MSE
        ConcurrentMap<Integer, Map<String, Double>> results = new ConcurrentHashMap<>();

        // Include STV and List Proportional in the set of series to chart
        List<String> allSeriesNames = new ArrayList<>();
        allSeriesNames.add("STV");
        allSeriesNames.add("FPTP");
        allSeriesNames.add("Strategic FPTP");
        allSeriesNames.add("National Strategic FPTP");
        allSeriesNames.add("Star");
        allSeriesNames.add("RCV");
        allSeriesNames.add("Approval");
        allSeriesNames.add("List Proportional");

        System.out.println("Parallel sweep party counts: " + partyCounts.size());

        // Parallel over party counts
        partyCounts.parallelStream().forEach(parties -> {
            Map<String, Double> mseMap = new LinkedHashMap<>();
            try {
                Country country = new Country();
                country.setPartyCount(parties);

                // Critical region: utilities touch Main.country internally
                synchronized (PartySweepParallel.class) {
                    Main.country = country;
                    country.runElection();

                    // Create a fresh national popularity cache for this task
                    NationalPopularityCache localPopularity = new NationalPopularityCache();
                    localPopularity.init();

                    // Compute popular shares for MSE target
                    Map<String, Double> popularShares = OptimizedVoteUtils.computePopularPartyShares(
                            country.posExists,
                            country.getCitizens(),
                            VotingUtils.FavoriteMode.TOP_RANK,
                            true
                    );

                    // STV baseline
                    List<Representative> stvSeats = country.getChamber();
                    double mseStv = VotingUtils.computeMSEBetweenSeatsAndPopularVote(stvSeats, popularShares);
                    mseMap.put("STV", mseStv);

                    // Other single-winner methods simulated on this country
                    // Build method map bound to the local popularity cache for this task
                    Map<String, ElectionMethod> methods = new LinkedHashMap<>();
                    methods.put("FPTP", VotingUtils::runFPTP);
                    methods.put("Strategic FPTP", VotingUtils::runFPTPStrategic);
                    methods.put("National Strategic FPTP", (cands, voters) -> localPopularity.runFPTPNationalStrategic(cands, voters));
                    methods.put("Star", VotingUtils::runStarElection);
                    methods.put("RCV", VotingUtils::runRCVElection);
                    methods.put("Approval", VotingUtils::runApprovalElection);

                    for (Map.Entry<String, ElectionMethod> e : methods.entrySet()) {
                        String name = e.getKey();
                        try {
                            List<Representative> seats = country.simulate(e.getValue());
                            double mse = VotingUtils.computeMSEBetweenSeatsAndPopularVote(seats, popularShares);
                            mseMap.put(name, mse);
                        } catch (Exception methodEx) {
                            methodEx.printStackTrace();
                            mseMap.put(name, Double.NaN);
                        }
                    }

                    // Proportional list method
                    try {
                        List<Representative> proportional = country.simulateProportional();
                        double mseProp = VotingUtils.computeMSEBetweenSeatsAndPopularVote(proportional, popularShares);
                        mseMap.put("List Proportional", mseProp);
                    } catch (Exception propEx) {
                        propEx.printStackTrace();
                        mseMap.put("List Proportional", Double.NaN);
                    }
                }

                System.out.printf(Locale.ROOT, "Done parties=%3d  STV=%.6f%n", parties, mseMap.getOrDefault("STV", Double.NaN));
            } catch (Throwable t) {
                t.printStackTrace();
                // Ensure all series have a placeholder value on failure
                for (String s : allSeriesNames) mseMap.put(s, Double.NaN);
            }

            results.put(parties, mseMap);
        });

        // Build X axis
        double[] xs = partyCounts.stream().sorted().mapToDouble(Integer::doubleValue).toArray();

        // Render chart
        MyLineChartDisplay.clear();
        for (String name : allSeriesNames) {
            double[] ys = new double[partyCounts.size()];
            int i = 0;
            for (int parties : partyCounts) {
                Map<String, Double> map = results.getOrDefault(parties, Collections.emptyMap());
                Double v = map.get(name);
                ys[i++] = (v == null) ? Double.NaN : v;
            }
            MyLineChartDisplay.addLine(name, xs, ys);
        }

        MyLineChartDisplay.show("MSE vs Number of Parties (2 → " + MAX_PARTIES + ")");
    }

    private static int computeStep(int parties) {
        if (parties < 10) return 1;     // 2,3,...,9
        if (parties < 50) return 2;     // 10,12,...,48,50
        if (parties < 100) return 5;    // 55,60,...
        if (parties < 200) return 10;   // 100,110,...,190,200
        if (parties < 400) return 20;   // 220,240,...,380,400
        return 50;                       // 450,500,...,700
    }

    /**
     * Runs a parallel sweep over chamber sizes (min = state count → MAX)
     * and charts how the MSE between seats and popular vote changes for each method.
     *
     * Minimum chamber size is exactly Country#getStateCount().
     */
    public static void runByChamberSize() {
        ValueAssigner.init();

        // Determine minimum chamber size from a probe Country (state count is fixed per Country)
        int minChamber;
        try {
            Country probe = new Country();
            minChamber = probe.getStateCount();
        } catch (Throwable t) {
            // Fallback to a sane default if construction fails
            minChamber = 75;
        }

        final int MAX_CHAMBER = Math.max(minChamber, 2000);

        List<Integer> chamberSizes = new ArrayList<>();
        for (int size = minChamber; size <= MAX_CHAMBER; size += computeChamberStep(size)) chamberSizes.add(size);

        // Include STV and List Proportional in the set of series to chart (keep series order consistent)
        List<String> allSeriesNames = new ArrayList<>();
        allSeriesNames.add("STV");
        allSeriesNames.add("FPTP");
        allSeriesNames.add("Strategic FPTP");
        allSeriesNames.add("National Strategic FPTP");
        allSeriesNames.add("Star");
        allSeriesNames.add("RCV");
        allSeriesNames.add("Approval");
        allSeriesNames.add("List Proportional");

        System.out.println("Chamber sweep (sequential) sizes: " + chamberSizes.size());

        // Stream results directly to the chart to avoid holding large arrays/maps in memory
        MyLineChartDisplay.clear();

        // Use sequential iteration to limit peak memory usage
        for (int chamberSize : chamberSizes) {
            Map<String, Double> mseMap = new LinkedHashMap<>();
            try {
                Country country = new Country();
                country.setChamberSize(chamberSize);

                synchronized (PartySweepParallel.class) {
                    Main.country = country;
                    country.runElection();

                    NationalPopularityCache localPopularity = new NationalPopularityCache();
                    localPopularity.init();

                    Map<String, Double> popularShares = OptimizedVoteUtils.computePopularPartyShares(
                            country.posExists,
                            country.getCitizens(),
                            VotingUtils.FavoriteMode.TOP_RANK,
                            true
                    );

                    // STV baseline (already in country)
                    List<Representative> stvSeats = country.getChamber();
                    double mseStv = VotingUtils.computeMSEBetweenSeatsAndPopularVote(stvSeats, popularShares);
                    mseMap.put("STV", mseStv);

                    Map<String, ElectionMethod> methods = new LinkedHashMap<>();
                    methods.put("FPTP", VotingUtils::runFPTP);
                    methods.put("Strategic FPTP", VotingUtils::runFPTPStrategic);
                    methods.put("National Strategic FPTP", (cands, voters) -> localPopularity.runFPTPNationalStrategic(cands, voters));
                    methods.put("Star", VotingUtils::runStarElection);
                    methods.put("RCV", VotingUtils::runRCVElection);
                    methods.put("Approval", VotingUtils::runApprovalElection);

                    for (Map.Entry<String, ElectionMethod> e : methods.entrySet()) {
                        String name = e.getKey();
                        try {
                            List<Representative> seats = country.simulate(e.getValue());
                            double mse = VotingUtils.computeMSEBetweenSeatsAndPopularVote(seats, popularShares);
                            mseMap.put(name, mse);
                        } catch (Exception methodEx) {
                            methodEx.printStackTrace();
                            mseMap.put(name, Double.NaN);
                        }
                    }

                    try {
                        List<Representative> proportional = country.simulateProportional();
                        double mseProp = VotingUtils.computeMSEBetweenSeatsAndPopularVote(proportional, popularShares);
                        mseMap.put("List Proportional", mseProp);
                    } catch (Exception propEx) {
                        propEx.printStackTrace();
                        mseMap.put("List Proportional", Double.NaN);
                    }
                }

                System.out.printf(Locale.ROOT, "Done chamber=%4d  STV=%.6f%n", chamberSize, mseMap.getOrDefault("STV", Double.NaN));
            } catch (Throwable t) {
                t.printStackTrace();
                for (String s : allSeriesNames) mseMap.put(s, Double.NaN);
            }

            // Push points to chart incrementally
            double x = chamberSize;
            for (String series : allSeriesNames) {
                double y = mseMap.getOrDefault(series, Double.NaN);
                MyLineChartDisplay.addPoint(series, x, y);
            }

            // Encourage earlier GC of heavy objects from this iteration
            mseMap.clear();
        }

        MyLineChartDisplay.show("MSE vs Chamber Size (" + minChamber + " → " + MAX_CHAMBER + ")");
    }

    private static int computeChamberStep(int size) {
        if (size < 200) return 5;      // finer at the low end
        if (size < 500) return 10;
        if (size < 1000) return 20;
        if (size < 2000) return 50;
        return 100;
    }
}
