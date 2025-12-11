package com.crotaplague;

import com.crotaplague.Ballots.STVBallot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class RandomScripts {
    /**
     * Loads a comma-separated list from a text file and returns a trimmed String[] of names.
     * @param filePath - path to the comma-separated text file (e.g., "names_list.txt")
     * @return String[] array of names (trimmed, empty entries removed)
     * @throws IOException if the file can't be read
     */
    public static String[] generateNames(String filePath) {
        Path path = Paths.get(filePath);
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);

            // Split on commas, trim whitespace, filter empties, return array
            return Arrays.stream(content.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        }catch(IOException e) {
            return new String[0];
        }
    }

    public static String[] generateLaws(){
        String[] laws = {"Abortion", "Drinking", "Marijuana", "Gay Marriage", "Illegal Immigration",
                "Affirmative Action", "Transgenderism", "Homosexuality", "Atheism", "Littering", "Religious Liberty", "School Prayer",
                "Recreational Drugs", "Smoking", "Dueling", "Gambling", "Betting", "Online Gambling"};
        return laws;
    }

    public static String[] generateFirstNames() {
        return new String[]{
                // original
                "Max","Matthew","Lorna","John","Mark","Mathias","Thomas","Peter","Frodo","Patrick",
                "Squidward","Henry","Bob","Keira","Aisling","Fredrick","Shaggy","Alice","Evelyn",
                "Olivia","Noah","Liam","Emma","Ava","Sophia","Mason","Lucas","Charlotte","Amelia",
                "Harper","Ethan","Logan","Grace","Zoe","Dylan","Ivy","Julian","Miles","Nora",
                "Silas","Mira","Oscar","Finn","Riley","Sienna","Gideon","Rose","David",
                "Conor","Connor","Steven","Kevin",

                // your fictional ones
                "Sherlock","Arthur","Watson","Bilbo","Gandalf","Aragorn","Legolas","Gimli","Elrond",
                "Thorin","Eowyn","Galadriel","Boromir","Sauron","Samwise","Pippin","Merry",
                "Ezio","Altair","Geralt","Ciri","Yennefer","Triss","Dandelion",
                "Aloy","Kratos","Atreus","Zelda","Link","Ganondorf",
                "Cloud","Tifa","Aerith","Barret","Sephiroth",
                "Mario","Luigi","Peach","Bowser","Garnet","Amethyst","Pearl",

                // more historical / myth
                "Hector","Achilles","Paris","Hera","Athena","Apollo","Zeus","Hestia","Demeter",
                "Hermes","Hephaestus","Perseus","Medusa","Orion","Theseus","Minos","Cadmus",
                "Isis","Osiris","Ra","Horus","Anubis","Set","Nephthys","Seshat",

                // Roman / medieval / etc
                "Hadrian","Trajan","Tiberius","Caligula","Nero","Vespasian","Constantine",
                "Eleanor","Isabella","Joan","Richard","Edward","Harold","William","Godric",
                "Alaric","Roderic","Sigurd","Erik","Bjorn","Magnus","Sigrid",

                // added fantasy batch
                "Aldric","Elowen","Thalos","Kael","Lyra","Dorian","Seren","Eldrin","Nyssa","Korvan",
                "Isolde","Varyn","Selene","Kaida","Torin","Lyandra","Vaelis","Riven","Sorrel",

                // Dragon Age, Mass Effect, etc
                "Alistair","Morrigan","Leliana","Fenris","Hawke",
                "Shepard","Tali","Garrus","Liara","Wrex",

                // various fiction
                "Ryu","Ken","Chun","Cassandra","Merlin","Morgana","Gawain","Lancelot","Guinevere",
                "Hector","Nami","Zoro","Sanji","Robin","Usopp","Franky","Brook","Chopper",
                "Ripley","Hicks","Newt","Dutch","Blain",
                "Astarion","Karlach","Shadowheart","LaeZel","Halsin",
                "Korra","Aang","Katara","Sokka","Toph","Zuko","Azula",

                // Destiny, OC style
                "Phoenix","Nova","Ember","Onyx","Crota","Oryx","Solara","Kaelen","Rhydian",

                // new large additions
                "Titus","Cassius","Octavia","Vesta","Lucius","Marcellus","Flavia","Sabine","Rufus",
                "Jorah","Drogo","Missandei","Olenna","Margaery","Podrick","Gendry","Barristan",
                "Sheev","Obiwan","Anakin","Padme","Ahsoka","Revan","Malak","Bastila","Satele",
                "Varric","Isabela","Anders","Bethany","Carver","Orsino","Loghain",
                "Ellie","Joel","Tess","Abby","Dina","Lev",
                "Booker","Elizabeth","Sullivan","Cole","Delsin",
                " Shepard","Tychus","Nova","Raynor","Arcturus","Zeratul","Artanis","Rohana",
                "Jaina","Thrall","Sylvanas","Uther","Anduin","Varian","Grom","Cairne","Voljin",
                "Aeris","Kass","Riven","Fiora","Vi","Caitlyn","Jinx","Ryze","Sona","Ahri",
                "Ezreal","Rakan","Xayah",

                // generic fantasy extras
                "Kelwyn","Faelar","Vaelis","Torwyn","Marwyn","Elira","Theron","Zira","Aren","Soren",
                "Liora","Korin","Talindra","Vexen","Malora","Zeren","Rys","Kalen","Oswin"
        };
    }


    public static String[] generateLastNames() {
        return new String[]{
                // original real
                "Smith","Johnson","Williams","Brown","Jones","Miller","Davis","Garcia","Rodriguez",
                "Martinez","Hernandez","Lopez","Gonzalez","Wilson","Anderson","Thomas","Taylor",
                "Moore","Jackson","Martin","Lee","Perez","Thompson","White","Harris","Sanchez",
                "Clark","Ramirez","Lewis","Robinson","Walker","Young","Allen","King","Wright",
                "Scott","Torres","Nguyen","Hill","Flores","Green","Adams","Nelson","Baker",
                "Hall","Rivera","Campbell","Mitchell","Carter","Roberts",

                // fictional
                "Holmes","Watson","Stark","Lannister","Targaryen","Baggins","Brandybuck","Took",
                "Oakenshield","Stormcloak","Dragonborn","Kenway","Auditore","Rivia","Zora",
                "Hyrule","Kusanagi","Strife","Lockhart","Valentine","Highwind",

                // fantasy batch
                "Stormwind","Nightbloom","Ironhart","Duskwhisper","Brightshield","Ravensong",
                "Oakensoul","Frostvale","Shadowmere","Goldbloom","Starfall","Moonforge",
                "Dawncaster","Winterthorn","Ashwalker","Evercrest","Duneveil","Silvertide",

                // new additions
                "Hawthorne","Blackwood","Ravencrest","Duskwood","Stonehelm","Grimvale",
                "Ironwood","Stormrider","Emberfall","Nightshade","Silverkeep","Blackspear",
                "Redwyn","Morrowind","Kingsglaive","Wolfsbane","Dragonscale","Deepwater",
                "Brightmore","Thorne","Underwood","Ashford","Stormborn","Wintermere",

                // more real or fantasy friendly
                "Windsor","Holloway","Briarwood","Coldspring","Ashenford","Silverbrook",
                "Longshore","Ironridge","Stormford","Ridgewell","Brightmoor","Marigold",
                "Thundershield","Emberforge","Duskridge","Sunwhisper","Drakemoor","Goldshore",
                "Wraithfall","Stormvale","Dawnhollow","Fangridge","Fellwater","Whiteshade"
        };
    }

    private static final char[] MIDDLE_INITIALS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static String getRandomName() {
        String[] first = generateFirstNames();
        String[] last = generateLastNames();

        int fi = (int) (Math.random() * first.length);
        int li = (int) (Math.random() * last.length);

        char middle = MIDDLE_INITIALS[(int)(Math.random() * MIDDLE_INITIALS.length)];

        return first[fi] + " " + middle + ". " + last[li];
    }



    public static int biasByAge(int age){
        age -= 35;
        return (int) Math.round(-0.255 * age);
    }

    public static int search(int value, int[] a) {
        final int n = a.length;
        final int first = a[0];
        final int last = a[n - 1];

        if (value <= first) {
            return first;
        }
        if (value >= last) {
            return last;
        }

        int lo = 0;
        int hi = n - 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int mv = a[mid];

            if (value < mv) {
                hi = mid - 1;
            } else if (value > mv) {
                lo = mid + 1;
            } else {
                return mv;
            }
        }
        // choose closest between a[lo] and a[hi]
        int loVal = a[lo];
        int hiVal = a[hi];
        return (loVal - value) < (value - hiVal) ? loVal : hiVal;
    }

    public static Party search(int value, Party[] a) {
        final int n = a.length;
        final int firstBias = a[0].getBias();
        final int lastBias = a[n - 1].getBias();

        if (value <= firstBias) {
            return a[0];
        }
        if (value >= lastBias) {
            return a[n - 1];
        }

        int lo = 0;
        int hi = n - 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int mv = a[mid].getBias();

            if (value < mv) {
                hi = mid - 1;
            } else if (value > mv) {
                lo = mid + 1;
            } else {
                return a[mid];
            }
        }
        int loBias = a[lo].getBias();
        int hiBias = a[hi].getBias();
        return (loBias - value) < (value - hiBias) ? a[lo] : a[hi];
    }

    public static Representative search(int value, Representative[] a) {
        final int n = a.length;
        final int firstBias = a[0].getBias();
        final int lastBias = a[n - 1].getBias();
        if (value <= firstBias) {
            return a[0];
        }
        if (value >= lastBias) {
            return a[n - 1];
        }

        int lo = 0;
        int hi = n - 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int mv = a[mid].getBias();

            if (value < mv) {
                hi = mid - 1;
            } else if (value > mv) {
                lo = mid + 1;
            } else {
                return a[mid];
            }
        }
        int loBias = a[lo].getBias();
        int hiBias = a[hi].getBias();
        return (loBias - value) < (value - hiBias) ? a[lo] : a[hi];
    }

    public static Representative[] rank(Citizen c, List<Representative> reps){
        Representative[] arr = new Representative[reps.size()];
        int cho = java.util.concurrent.ThreadLocalRandom.current().nextInt(3, 6);
        List<Representative> choices = new ArrayList<>();
        for(int i = 0; i < cho; i++){
            Representative[] a = new Representative[reps.size()]; reps.toArray(a);
            if(reps.isEmpty()) break;
            choices.add(search(c.getBias(), a)); reps.remove(choices.get(i));
        }

        return choices.toArray(arr);
    }

    public static Representative rankedChoice(Set<Map.Entry<Representative, List<Citizen>>> vote){
        Representative rep = null;
        if(!vote.isEmpty()) System.out.print("yep yep " + vote.size());
        for(int i = 0; i < vote.size(); i++){
            Map.Entry<Representative, List<Citizen>> smallest = null;
            for(Map.Entry<Representative, List<Citizen>> ent : vote){
                if(smallest == null){ smallest = ent; continue;}
                List<Citizen> voters = ent.getValue();
                if(voters.size() < smallest.getValue().size()) smallest = ent;
            }
            rep = smallest.getKey();
        }

        return rep;
    }

    public static Party[] loadPartiesFromFile() {
        List<Party> partyList = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(Main.path));
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1).trim();
                }
                int lastSpaceIndex = line.lastIndexOf(' ');
                if (lastSpaceIndex == -1 || lastSpaceIndex == line.length() - 1) {
                    System.err.println("Invalid line (no space before score): " + line);
                    continue;
                }

                String name = line.substring(0, lastSpaceIndex).trim();
                String scorePart = line.substring(lastSpaceIndex + 1).trim();

                int score;
                try {
                    score = Integer.parseInt(scorePart);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid score in line: " + line);
                    continue;
                }

                partyList.add(new Party(name, score));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return partyList.toArray(new Party[0]);
    }

    public static Comparator<Party> unstableComparator(boolean unstable) {
        if (!unstable) {
            return Comparator.comparingInt(Party::getBias);
        }
        Random random = new Random();
        return (a, b) -> {
            int biasDiff = Integer.compare(a.getBias(), b.getBias());
            if (biasDiff != 0) return biasDiff;
            return random.nextBoolean() ? -1 : 1;
        };
    }

    public static List<VotingBlock> createBlocks(State state) {
        List<County> counties = new ArrayList<>(state.getCounties());
        int total = counties.size();
        List<Integer> blockSizes = new ArrayList<>();

        if (total <= 0) return new ArrayList<>();
        if (total < 3) {
            return Collections.singletonList(new VotingBlock(new ArrayList<>(counties)));
        }

        boolean found = false;
        int max7 = total / 7;
        for (int n7 = max7; n7 >= 0 && !found; n7--) {
            int remAfter7 = total - 7 * n7;
            int max5 = remAfter7 / 5;
            for (int n5 = max5; n5 >= 0 && !found; n5--) {
                int rem = remAfter7 - 5 * n5;
                if (rem % 3 == 0) {
                    int n3 = rem / 3;
                    for (int i = 0; i < n7; i++) blockSizes.add(7);
                    for (int i = 0; i < n5; i++) blockSizes.add(5);
                    for (int i = 0; i < n3; i++) blockSizes.add(3);
                    found = true;
                }
            }
        }

        if (!found) {
            int maxBlocks = Math.max(1, total / 3);
            int targetBlocks = Math.max(1, (int) Math.round((double) total / 6.0));
            if (targetBlocks > maxBlocks) targetBlocks = maxBlocks;

            while (targetBlocks > 1) {
                int base = total / targetBlocks;
                int remainder = total % targetBlocks;
                if (base >= 3) {
                    for (int i = 0; i < targetBlocks; i++) {
                        blockSizes.add(base + (i < remainder ? 1 : 0));
                    }
                    break;
                }
                targetBlocks--;
            }
            if (blockSizes.isEmpty()) {
                blockSizes.add(total);
            }
        }

        int sum = blockSizes.stream().mapToInt(Integer::intValue).sum();
        if (sum != total) {
            if (sum < total) {
                int missing = total - sum;
                int last = blockSizes.size() - 1;
                blockSizes.set(last, blockSizes.get(last) + missing);
            } else {
                int excess = sum - total;
                for (int i = blockSizes.size() - 1; i >= 0 && excess > 0; i--) {
                    int take = Math.min(blockSizes.get(i) - 3, excess); // keep >=3
                    if (take > 0) {
                        blockSizes.set(i, blockSizes.get(i) - take);
                        excess -= take;
                    }
                }
                while (blockSizes.stream().mapToInt(Integer::intValue).sum() > total && blockSizes.size() > 1) {
                    int last = blockSizes.remove(blockSizes.size() - 1);
                    blockSizes.set(blockSizes.size() - 1, blockSizes.get(blockSizes.size() - 1) + last);
                }
            }
        }

        List<VotingBlock> blocks = new ArrayList<>();
        int index = 0;
        for (int blockSize : blockSizes) {
            int end = Math.min(index + blockSize, counties.size());
            List<County> sub = new ArrayList<>(counties.subList(index, end));
            blocks.add(new VotingBlock(sub));
            index = end;
        }

        if (index < counties.size()) {
            List<County> tail = counties.subList(index, counties.size());
            if (!blocks.isEmpty()) {
                List<County> merged = new ArrayList<>(blocks.get(blocks.size() - 1).getCounties());
                merged.addAll(tail);
                blocks.set(blocks.size() - 1, new VotingBlock(merged));
            } else {
                blocks.add(new VotingBlock(new ArrayList<>(tail)));
            }
        }

        return blocks;
    }


    /**
     * Build a candidate pool by sampling citizens from each block.
     *
     * @param blocks     voting blocks
     * @param multiplier how many candidates per seat to request (1.0 = exactly seats,
     *                   1.5 = seats + 50% extra, 2.0 = double, etc.)
     * @return list of Representative candidates
     */
    public static List<Representative> assignRepresentatives(List<VotingBlock> blocks, double multiplier, Desire d) {
        // Pre-size roughly: for each block seats ~= counties.size(), desired ~= seats*multiplier
        int approx = 0;
        for (VotingBlock b : blocks) approx += Math.max(1, b.getCounties().size());
        approx = (int)Math.ceil(approx * Math.max(1.0, multiplier));

        List<Representative> representatives = new ArrayList<>(approx);
        // Track chosen citizens to avoid duplicates across blocks
        Set<Citizen> usedCitizens = new HashSet<>(approx * 2);

        ThreadLocalRandom tlr = ThreadLocalRandom.current();

        for (VotingBlock block : blocks) {
            // Gather citizens for this block, pre-sizing to avoid resizes
            int cap = 0;
            for (County county : block.getCounties()) cap += county.getCitizens().size();
            List<Citizen> blockCitizens = new ArrayList<>(cap);
            for (County county : block.getCounties()) {
                blockCitizens.addAll(county.getCitizens());
            }

            // Shuffle in-place using ThreadLocalRandom for speed
            for (int i = blockCitizens.size() - 1; i > 0; i--) {
                int j = tlr.nextInt(i + 1);
                Collections.swap(blockCitizens, i, j);
            }

            int seats = Math.max(1, block.getCounties().size());
            int desired = Math.max(seats, (int) Math.ceil(seats * multiplier));

            int added = 0;
            for (int idx = 0, n = blockCitizens.size(); idx < n && added < desired; idx++) {
                Citizen c = blockCitizens.get(idx);
                if (!usedCitizens.add(c)) continue;

                int age = c.getAge();
                if (d == Desire.CHAMBERLAIN) {
                    if (age < 35) continue;
                } else {
                    if (age < 18) continue;
                }
                representatives.add(new Representative(c, d));
                added++;
            }
        }

        return representatives;
    }

    /**
     * Run STV per block using the provided candidate pool.
     *
     * @param blocks        list of voting blocks for which to run STV
     * @param candidatePool global pool of Representative candidates
     * @return combined winners from all blocks
     */
    public static List<Representative> runStv(List<VotingBlock> blocks,
                                              List<Representative> candidatePool) {
        List<Representative> allWinners = new ArrayList<>();

        for (VotingBlock block : blocks) {
            int seats = block.getCounties().size();
            if (seats <= 0) continue;

            // Filter candidate pool for this block (preserve original order)
            // Use a HashSet for O(1) membership tests, then a simple for-loop to avoid stream overhead
            Set<County> blockCounties = new HashSet<>(block.getCounties());
            List<Representative> blockCandidates = new ArrayList<>(candidatePool.size());
            for (int i = 0, n = candidatePool.size(); i < n; i++) {
                Representative r = candidatePool.get(i);
                County rc = r.getCounty();
                if (rc != null && blockCounties.contains(rc)) blockCandidates.add(r);
            }

            if (blockCandidates.isEmpty()) continue;
            if (blockCandidates.size() <= seats) {
                allWinners.addAll(blockCandidates);
                continue;
            }

            List<Citizen> voters = block.getAllCitizens();
            int totalVoters = voters.size();
            if (totalVoters == 0) continue;

            // Index candidates for array-based operations
            int nCands = blockCandidates.size();
            Map<Representative, Integer> candIndex = new HashMap<>(nCands * 2);
            for (int i = 0; i < nCands; i++) candIndex.put(blockCandidates.get(i), i);

            // Build ballots as arrays of indices to avoid repeated map/set lookups
            final class BallotA {
                final int[] prefs;
                final double weight;
                BallotA(int[] p, double w) { this.prefs = p; this.weight = w; }
            }
            List<BallotA> ballots = new ArrayList<>(totalVoters);
            for (int vi = 0; vi < totalVoters; vi++) {
                Citizen voter = voters.get(vi);
                List<Representative> ranking = VotingUtils.rankCandidatesForVoter(voter, blockCandidates);
                STVBallot b = new STVBallot(ranking);
                List<Representative> prefs = b.getPreferences();
                int m = prefs.size();
                int[] pi = new int[m];
                for (int i = 0; i < m; i++) {
                    Integer idx = candIndex.get(prefs.get(i));
                    pi[i] = (idx == null ? -1 : idx);
                }
                ballots.add(new BallotA(pi, b.getWeight()));
            }

            double quota = Math.floor((double) totalVoters / (seats + 1)) + 1;

            // Active/elected flags preserve insertion order via activeOrder list
            boolean[] activeFlag = new boolean[nCands];
            boolean[] electedFlag = new boolean[nCands];
            List<Integer> activeOrder = new ArrayList<>(nCands);
            for (int i = 0; i < nCands; i++) { activeFlag[i] = true; activeOrder.add(i); }

            double[] keep = new double[nCands];
            Arrays.fill(keep, 1.0);

            final double TOL = 1e-9;
            final int MAX_ITER = 1000;
            boolean stabilized = false;

            for (int iter = 0; iter < MAX_ITER && !stabilized; iter++) {
                double[] totals = new double[nCands];

                // Tally with fractional transfers
                for (BallotA b : ballots) {
                    double transfer = b.weight;
                    int[] prefs = b.prefs;
                    for (int pi : prefs) {
                        if (pi < 0) continue;
                        if (!activeFlag[pi]) continue;
                        totals[pi] += transfer;
                        double k = keep[pi];
                        transfer = transfer * (1.0 - k);
                        if (transfer < 1e-12) break;
                    }
                }

                // Determine newly elected
                boolean anyChange = false;
                for (int i = 0; i < nCands; i++) {
                    if (activeFlag[i] && totals[i] >= quota - 1e-12) {
                        if (!electedFlag[i]) {
                            electedFlag[i] = true;
                            anyChange = true;
                        }
                    }
                }

                // Update keep factors for elected
                for (int i = 0; i < nCands; i++) {
                    if (!electedFlag[i]) continue;
                    double totalFor = totals[i];
                    double newKeep = totalFor > 0.0 ? Math.min(1.0, quota / totalFor) : 0.0;
                    double oldKeep = keep[i];
                    if (Math.abs(newKeep - oldKeep) > TOL) {
                        keep[i] = newKeep;
                        anyChange = true;
                    } else {
                        keep[i] = newKeep;
                    }
                }

                if (!anyChange) {
                    int electedCount = 0, activeCount = 0;
                    for (int i = 0; i < nCands; i++) { if (electedFlag[i]) electedCount++; if (activeFlag[i]) activeCount++; }

                    if (electedCount >= seats) {
                        stabilized = true;
                        break;
                    }
                    if (activeCount <= (seats - electedCount)) {
                        for (int i = 0; i < nCands; i++) if (activeFlag[i]) electedFlag[i] = true;
                        stabilized = true;
                        break;
                    }

                    // Eliminate the lowest (tie-break randomly), preserving active insertion order
                    int toEliminate = -1;
                    double minVotes = Double.MAX_VALUE;
                    for (int idx : activeOrder) {
                        if (!activeFlag[idx]) continue;
                        if (electedFlag[idx]) continue;
                        double v = totals[idx];
                        if (v < minVotes) {
                            minVotes = v;
                            toEliminate = idx;
                        } else if (v == minVotes) {
                            if (java.util.concurrent.ThreadLocalRandom.current().nextBoolean()) toEliminate = idx;
                        }
                    }

                    if (toEliminate >= 0) {
                        activeFlag[toEliminate] = false;
                        keep[toEliminate] = 0.0;
                        anyChange = true;
                    } else {
                        stabilized = true;
                        break;
                    }
                }
            }

            // If not enough elected, fill by highest remaining totals using final pass
            int electedCount = 0;
            for (boolean b : electedFlag) if (b) electedCount++;
            if (electedCount < seats) {
                double[] finalTotals = new double[nCands];
                for (BallotA b : ballots) {
                    double transfer = b.weight;
                    for (int pi : b.prefs) {
                        if (pi < 0) continue;
                        if (!activeFlag[pi]) continue;
                        finalTotals[pi] += transfer;
                        double k = keep[pi];
                        transfer = transfer * (1.0 - k);
                        if (transfer < 1e-12) break;
                    }
                }

                // Build remaining list preserving active order, then stable sort by totals desc
                List<Integer> remainingIdx = new ArrayList<>();
                for (int idx : activeOrder) if (activeFlag[idx] && !electedFlag[idx]) remainingIdx.add(idx);
                remainingIdx.sort((ia, ib) -> Double.compare(finalTotals[ib], finalTotals[ia]));

                for (int idx : remainingIdx) {
                    if (electedCount >= seats) break;
                    electedFlag[idx] = true;
                    electedCount++;
                }
            }

            // Collect winners in original candidate object order
            for (int i = 0; i < nCands; i++) if (electedFlag[i]) allWinners.add(blockCandidates.get(i));
        }

        return allWinners;
    }


    public static Map<State, Integer> apportionByHamilton(List<State> states, int totalSeats) {
        Map<State, Integer> allocation = new LinkedHashMap<>();
        if (states == null || states.isEmpty() || totalSeats <= 0) return allocation;

        final int n = states.size();

        // If totalSeats < n, assign 1 to the first totalSeats states, 0 to the rest (deterministic)
        if (totalSeats < n) {
            for (int i = 0; i < n; i++) allocation.put(states.get(i), i < totalSeats ? 1 : 0);
            return allocation;
        }

        // Baseline: give 1 to each state (no-zero baseline)
        for (int i = 0; i < n; i++) allocation.put(states.get(i), 1);
        int seatsLeft = totalSeats - n;

        // Totals and populations
        long totalPopulation = 0L;
        int[] pop = new int[n];
        for (int i = 0; i < n; i++) {
            int p = states.get(i).getCitizens().size();
            pop[i] = p;
            totalPopulation += p;
        }

        if (totalPopulation <= 0L) {
            // fallback: distribute evenly in round-robin
            int i = 0;
            while (seatsLeft-- > 0) {
                State s = states.get(i++ % n);
                allocation.put(s, allocation.get(s) + 1);
            }
            return allocation;
        }

        // Compute exact quotas for the remaining seats using a common factor
        double factor = seatsLeft / (double) totalPopulation;
        int[] floors = new int[n];
        double[] rema = new double[n];

        int assigned = 0;
        for (int i = 0; i < n; i++) {
            double exact = pop[i] * factor;
            int fl = (int) Math.floor(exact);
            floors[i] = fl;
            rema[i] = exact - fl;
            assigned += fl;
        }

        // Apply floors to baseline allocations
        for (int i = 0; i < n; i++) {
            State s = states.get(i);
            allocation.put(s, allocation.get(s) + floors[i]);
        }
        int remaining = seatsLeft - assigned;

        // Build index array for sorting by remainder desc, population desc, state.toString asc
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (ia, ib) -> {
            int c1 = Double.compare(rema[ib], rema[ia]);
            if (c1 != 0) return c1;
            int c2 = Integer.compare(pop[ib], pop[ia]); // larger pop first
            if (c2 != 0) return c2;
            // deterministic fallback
            String sa = states.get(ia).toString();
            String sb = states.get(ib).toString();
            return sa.compareTo(sb); // ascending
        });

        // Distribute remaining seats following the sorted order
        for (int i = 0; i < remaining; i++) {
            int si = idx[i % n];
            State s = states.get(si);
            allocation.put(s, allocation.get(s) + 1);
        }

        // Final sanity: ensure exact total equals totalSeats (rare)
        int sum = 0;
        for (int i = 0; i < n; i++) sum += allocation.get(states.get(i));
        if (sum != totalSeats) {
            int diff = totalSeats - sum;
            if (diff > 0) {
                // add to largest populations
                Integer[] byPop = new Integer[n];
                for (int i = 0; i < n; i++) byPop[i] = i;
                Arrays.sort(byPop, (a, b) -> Integer.compare(pop[b], pop[a]));
                int i = 0;
                while (diff-- > 0) {
                    State s = states.get(byPop[i++ % n]);
                    allocation.put(s, allocation.get(s) + 1);
                }
            } else if (diff < 0) {
                // remove extras from smallest populations where possible (keeping >= 1)
                diff = -diff;
                Integer[] byPopAsc = new Integer[n];
                for (int i = 0; i < n; i++) byPopAsc[i] = i;
                Arrays.sort(byPopAsc, Comparator.comparingInt(a -> pop[a]));
                int i = 0;
                while (diff-- > 0) {
                    State s = states.get(byPopAsc[i % n]);
                    Integer cur = allocation.get(s);
                    if (cur != null && cur > 1) {
                        allocation.put(s, cur - 1);
                    } else {
                        boolean found = false;
                        for (int j = 0; j < n; j++) {
                            State cand = states.get(byPopAsc[j]);
                            int cv = allocation.get(cand);
                            if (cv > 1) {
                                allocation.put(cand, cv - 1);
                                found = true;
                                break;
                            }
                        }
                        if (!found) break;
                    }
                    i++;
                }
            }
        }

        return allocation;
    }

    public static double randomMultiplier() {
        double mean = 1.7;
        double stdDev = 0.3;

        double value = mean + ThreadLocalRandom.current().nextGaussian() * stdDev;

        return Math.max(1.0, value);
    }


    public static Party search(Citizen citizen, Party[] parties) {
        if (parties == null || parties.length == 0) return null;
        if (citizen == null) return parties[0];

        double cBias = citizen.getBias(); // 0..100
        List<Value> cValues = citizen.getValues() == null ? Collections.emptyList() : citizen.getValues();

        // Prebuild party value maps for O(1) lookups
        Map<Party, Map<String, Value>> partyValueMap = new HashMap<>();
        for (Party p : parties) {
            Map<String, Value> map = new HashMap<>();
            if (p.getValues() != null) {
                for (Value pv : p.getValues()) map.put(pv.getName(), pv);
            }
            partyValueMap.put(p, map);
        }

        Party best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        // weights: tune these if desired
        final double BIAS_WEIGHT = 0.60;   // how important overall bias is
        final double VALUE_WEIGHT = 0.40;  // how important values are (scaled by bias proximity)

        for (Party p : parties) {
            double pBias = p.getBias();
            double normBiasDist = Math.abs(cBias - pBias) / 100.0;    // 0..1
            double biasSimilarity = 1.0 - normBiasDist;              // 1.0 = identical bias, 0 = opposite extremes

            // --- compute value alignment score ---
            Map<String, Value> pMap = partyValueMap.get(p);

            double sumWeightedSimilarity = 0.0;
            double sumMaxWeight = 0.0;
            double penalty = 0.0;
            double bestSingleAlignment = 0.0;

            for (Value cv : cValues) {
                double cvPolar = cv.getPolarization() / 10.0; // 0..1
                sumMaxWeight += cvPolar; // each citizen value could at most contribute cvPolar

                Value pv = pMap.get(cv.getName());
                if (pv != null) {
                    // opinion similarity 0..1 (1 = identical opinion)
                    double opinionSim = (10.0 - Math.abs(cv.getOpinion() - pv.getOpinion())) / 10.0;
                    // weight by average polarization (0..1)
                    double weight = (cv.getPolarization() + pv.getPolarization()) / 20.0;
                    double contrib = opinionSim * weight; // 0..1 * 0..1 => 0..1-ish
                    sumWeightedSimilarity += contrib;
                    bestSingleAlignment = Math.max(bestSingleAlignment, contrib);
                } else {
                    // party doesn't have that value: penalize proportional to how much citizen cares
                    // small penalty so missing single issues doesn't doom matching
                    penalty += cvPolar * 0.15;
                }
            }

            // Normalize values score to roughly -inf..1, then clamp to [0,1]
            double rawValuesScore = 0.0;
            if (sumMaxWeight > 0) rawValuesScore = (sumWeightedSimilarity - penalty) / sumMaxWeight;
            // clamp to [0,1]
            rawValuesScore = Math.max(0.0, Math.min(1.0, rawValuesScore));

            // Values matter more when party bias is close to citizen's bias:
            // Use a multiplier that is near 1 when normBiasDist is small, and drops when parties are distant.
            double valuesMultiplier = 1.0 - (normBiasDist * normBiasDist); // close biases => nearly 1, distant => small
            double weightedValuesPart = rawValuesScore * valuesMultiplier;

            // Final scored combination
            double score = (BIAS_WEIGHT * biasSimilarity) + (VALUE_WEIGHT * weightedValuesPart);

            // --- Eligibility / realism checks ---
            // Disallow extreme mismatch unless strong single-issue alignment exists:
            // Conditions to be considered "joinable":
            boolean basicBiasOk = biasSimilarity >= 0.20; // within ±20 bias points = reasonably close
            boolean strongValuePull = bestSingleAlignment >= 0.75 && biasSimilarity >= 0.10; // strong single issue + somewhat close
            boolean tolerantIfModerate = rawValuesScore >= 0.65 && biasSimilarity >= 0.15; // well-aligned values + mild bias proximity

            boolean joinable = basicBiasOk || strongValuePull || tolerantIfModerate;

            // If not joinable, give a heavy penalty to the score (so it won't win unless all parties are unjoinable)
            if (!joinable) score -= 1.0; // tune penalty magnitude as desired

            // Track best
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }

        // Fallback: if best was extremely penalized (all unjoinable), return the nearest-by-bias party instead
        if (best != null && bestScore < -0.5) {
            System.out.println("Well, this did run at least once");
            Party nearest = parties[0];
            double nearestDist = Math.abs(cBias - nearest.getBias());
            for (Party p : parties) {
                double d = Math.abs(cBias - p.getBias());
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }
            return nearest;
        }

        return best;
    }

    /**
     * Load party -> values lines from `filename` and add crotaplague.com.Value objects to parties map.
     * Expected line format:
     *   crotaplague.com.Party Name: value name 7 8, another value 5 -6, ...
     *
     * Uses the public static Map<String, crotaplague.com.Party> parties (keys are lowercase party names).
     */
    public static void loadPartyValuesFromFile(Country country) {
        String filename = "C:\\Users\\dsyme\\Downloads\\SocietySimulation\\SocietySimulation\\src\\PartyValues.txt";
        if (filename == null || filename.isBlank()) return;

        // Ensure parties map exists (it should be declared elsewhere as public static Map<String, crotaplague.com.Party> parties)
        if (country.parties == null) {
            country.parties = new HashMap<>(); // defensive — if you don't want this, remove this line.
        }

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int colon = line.indexOf(':');
                if (colon < 0) {
                    // malformed: no colon; skip
                    System.err.println("Skipping malformed line " + lineNo + ": no ':' found.");
                    continue;
                }

                String partyName = line.substring(0, colon).trim();
                if (partyName.isEmpty()) {
                    System.err.println("Skipping malformed line " + lineNo + ": empty party name.");
                    continue;
                }

                String key = partyName.toLowerCase();
                Party party = country.parties.get(key);
                if (party == null) {
                    // party not present in the map — skip (or optionally create one if desired)
                    //System.err.println("Warning: party not found in parties map for line " + lineNo + ": '" + partyName + "'. Skipping values for it.");
                    continue;
                }

                String rest = line.substring(colon + 1).trim();
                if (rest.isEmpty()) continue;

                // split on commas to get value tokens: "value name 7 8"
                String[] tokens = rest.split(",");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.isEmpty()) continue;

                    // Parse from the end: last two whitespace-separated tokens should be numbers (polarization, opinion)
                    String[] parts = token.split("\\s+");
                    if (parts.length < 3) {
                        System.err.println("Skipping malformed value token on line " + lineNo + ": '" + token + "'");
                        continue;
                    }

                    String opinionStr = parts[parts.length - 1];
                    String polStr = parts[parts.length - 2];
                    String valueName = String.join(" ", Arrays.copyOf(parts, parts.length - 2)).trim();

                    if (valueName.isEmpty()) {
                        System.err.println("Skipping value with empty name on line " + lineNo + ": '" + token + "'");
                        continue;
                    }

                    int pol, opin;
                    try {
                        pol = Integer.parseInt(polStr);
                        opin = Integer.parseInt(opinionStr);
                    } catch (NumberFormatException nfe) {
                        System.err.println("Skipping value with non-numeric polarization/opinion on line " + lineNo + ": '" + token + "'");
                        continue;
                    }

                    // clamp ranges defensively (crotaplague.com.Value constructor also clamps, but being explicit helps)
                    pol = Math.max(0, Math.min(10, pol));
                    opin = Math.max(-10, Math.min(10, opin));

                    // avoid duplicate by name (case-insensitive)
                    boolean alreadyHas = false;
                    List<Value> existing = party.getValues();
                    if (existing != null) {
                        for (Value ev : existing) {
                            if (ev.getName().equalsIgnoreCase(valueName)) {
                                alreadyHas = true;
                                break;
                            }
                        }
                    }

                    if (!alreadyHas) {
                        Value v = new Value(valueName, pol, opin);
                        party.addValue(v);
                    } else {
                        // optionally update existing value? currently skip duplicates
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Party largestParty(List<Representative> reps) {
        if (reps == null || reps.isEmpty()) return null;

        // Reserve a reasonable initial capacity to reduce rehashing (approx distinct parties)
        Map<Party, Integer> counts = new HashMap<>(Math.max(16, reps.size() / 2 + 1));

        Party best = null;
        int bestCount = 0;

        for (Representative r : reps) {
            if (r == null) continue;
            Party p = r.getParty();
            if (p == null) continue;

            int cnt = counts.merge(p, 1, Integer::sum);

            if (cnt > bestCount) {
                best = p;
                bestCount = cnt;
            } else if (cnt == bestCount && best != null) {
                String a = p.getName() == null ? "" : p.getName();
                String b = best.getName() == null ? "" : best.getName();
                if (a.compareToIgnoreCase(b) < 0) {
                    best = p;
                }
            } else if (best == null) {
                best = p;
                bestCount = cnt;
            }
        }

        return best;
    }

    /**
     * Return true if any Representative in the list belongs to the given party.
     * Works with null
     */
    public static boolean repsContainsParty(List<Representative> reps, Party party) {
        if (reps == null || party == null) return false;
        for (Representative r : reps) {
            if (r == null) continue;
            Party p = r.getParty();
            if (party.equals(p)) return true;
        }
        return false;
    }

    /**
     * Alternate: compare by party name (case-insensitive). Use this if crotaplague.com.Party.equals is not implemented
     * or party objects aren't the same identity but names match.
     */
    public static boolean repsContainsPartyByName(List<Representative> reps, Party party) {
        if (reps == null || party == null) return false;
        String target = party.getName() == null ? "" : party.getName().toLowerCase(Locale.ROOT);
        for (Representative r : reps) {
            if (r == null) continue;
            Party p = r.getParty();
            if (p == null) continue;
            String name = p.getName() == null ? "" : p.getName().toLowerCase(Locale.ROOT);
            if (target.equals(name)) return true;
        }
        return false;
    }

    private static final Random RNG = new Random();

    public static double normalClamped(double mean, double std) {
        double value = mean + RNG.nextGaussian() * std;
        if (value < 0) value = 0;
        if (value > 1) value = 1;
        return value;
    }

    public static double normalClamped() {
        return normalClamped(0.25, 0.15);
    }

}
