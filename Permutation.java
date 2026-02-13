import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Permutation {

    private static final int CHAIN_LENGTH = 4;
    private static final int MAX_DIGIT = 9;
    private static final int TARGET_SUM = 14;

    /**
     * A triple (3 numbers summing to 14) with state for chain search:
     * - circleIndex: which circle (0..3) this triple occupies in the current chain, or -1 if not placed
     * - inUse: true while this triple is part of the chain being built
     */
    public static class TripleState {
        public final List<Integer> numbers;
        public int circleIndex;
        public boolean inUse;

        public TripleState(List<Integer> numbers) {
            this.numbers = new ArrayList<>(numbers);
            this.circleIndex = -1;
            this.inUse = false;
        }

        public void placeInCircle(int circle) {
            this.circleIndex = circle;
            this.inUse = true;
        }

        public void unplace() {
            this.circleIndex = -1;
            this.inUse = false;
        }
    }

    public static void main(String[] args) {
        List<List<Integer>> triples = findTriplesSumTo14();
        System.out.println("Combinations summing to " + TARGET_SUM + ":");
        triples.forEach(System.out::println);
        System.out.println("Total: " + triples.size());
        System.out.println();

        List<List<List<Integer>>> chains = findValidChains(triples);
        System.out.println("Chains of 4 circles (each shares exactly 1 number with the next, digits 1–9 at most once):");
        chains.forEach(Permutation::printChain);
        System.out.println("Total chains: " + chains.size());
    }

    // --- Triples (sum to 14) ---

    public static List<List<Integer>> findTriplesSumTo14() {
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            for (int j = i + 1; j <= 8; j++) {
                for (int k = j + 1; k <= MAX_DIGIT; k++) {
                    if (i + j + k == TARGET_SUM) {
                        result.add(List.of(i, j, k));
                    }
                }
            }
        }
        return result;
    }

    // --- Chain rules ---

    public static boolean shareExactlyOne(List<Integer> a, List<Integer> b) {
        return countShared(a, b) == 1;
    }

    public static int countShared(List<Integer> a, List<Integer> b) {
        int count = 0;
        for (Integer x : a) {
            if (b.contains(x)) count++;
        }
        return count;
    }

    public static int getSharedNumber(List<Integer> a, List<Integer> b) {
        for (Integer x : a) {
            if (b.contains(x)) return x;
        }
        return -1;
    }

    public static Set<Integer> allDigits(List<List<Integer>> chain) {
        Set<Integer> set = new HashSet<>();
        for (List<Integer> circle : chain) set.addAll(circle);
        return set;
    }

    // --- Neighbor graph: for each triple index, indices of triples that share exactly one number ---

    public static List<List<Integer>> buildNeighborGraph(List<List<Integer>> triples) {
        int n = triples.size();
        List<List<Integer>> neighbors = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<Integer> adj = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (j != i && shareExactlyOne(triples.get(i), triples.get(j))) {
                    adj.add(j);
                }
            }
            neighbors.add(adj);
        }
        return neighbors;
    }

    // --- Find all valid chains by DFS (only follow neighbors, skip inUse triples) ---

    public static List<List<List<Integer>>> findValidChains(List<List<Integer>> triples) {
        List<List<List<Integer>>> result = new ArrayList<>();
        List<List<Integer>> neighbors = buildNeighborGraph(triples);
        List<TripleState> states = new ArrayList<>();
        for (List<Integer> t : triples) states.add(new TripleState(t));

        List<Integer> chainIndices = new ArrayList<>(CHAIN_LENGTH);
        Set<Integer> usedDigits = new HashSet<>(MAX_DIGIT);

        for (int i = 0; i < triples.size(); i++) {
            chainIndices.add(i);
            states.get(i).placeInCircle(0);
            usedDigits.addAll(states.get(i).numbers);
            Set<Integer> firstCircleAvailable = new HashSet<>(triples.get(i));  // all 3 can link to next
            dfs(i, 1, chainIndices, usedDigits, firstCircleAvailable, triples, neighbors, states, result);
            usedDigits.clear();
            states.get(i).unplace();
            chainIndices.clear();
        }
        return result;
    }

    /** Digits in the last circle that can be used as the link to the next (excludes the digit that linked into the last circle). */
    private static void dfs(int lastIdx, int depth,
                            List<Integer> chainIndices, Set<Integer> usedDigits,
                            Set<Integer> digitsAvailableForLink,  // only these can be the shared digit (each digit at most 2 consecutive circles)
                            List<List<Integer>> triples, List<List<Integer>> neighbors,
                            List<TripleState> states, List<List<List<Integer>>> result) {
        if (depth == CHAIN_LENGTH) {
            List<List<Integer>> chain = new ArrayList<>();
            for (int idx : chainIndices) chain.add(new ArrayList<>(triples.get(idx)));
            result.add(chain);
            return;
        }

        for (int nextIdx : neighbors.get(lastIdx)) {
            if (states.get(nextIdx).inUse) continue;

            List<Integer> nums = triples.get(nextIdx);
            int shared = -1;
            int newCount = 0;
            for (Integer d : nums) {
                if (digitsAvailableForLink.contains(d)) shared = d;
                else if (usedDigits.contains(d)) { shared = -2; break; }  // digit used in earlier circle
                else newCount++;
            }
            if (shared == -1 || shared == -2 || newCount != 2) continue;

            chainIndices.add(nextIdx);
            states.get(nextIdx).placeInCircle(depth);
            Set<Integer> newAvailable = new HashSet<>(nums);
            newAvailable.remove(shared);  // next link must be one of the two "new" digits in this circle
            for (Integer d : nums) if (!d.equals(shared)) usedDigits.add(d);

            dfs(nextIdx, depth + 1, chainIndices, usedDigits, newAvailable, triples, neighbors, states, result);

            for (Integer d : nums) if (!d.equals(shared)) usedDigits.remove(d);
            states.get(nextIdx).unplace();
            chainIndices.remove(chainIndices.size() - 1);
        }
    }

    // --- Output ---

    public static void printChain(List<List<Integer>> chain) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chain.size(); i++) {
            if (i > 0) {
                sb.append("  --").append(getSharedNumber(chain.get(i - 1), chain.get(i))).append(" shared--> ");
            }
            sb.append(chain.get(i));
        }
        sb.append("  [digits: ").append(allDigits(chain)).append("]");
        System.out.println(sb);
    }
}
