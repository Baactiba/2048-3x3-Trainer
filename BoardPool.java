import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stores a precomputed, pruned optimal-play board pool up to the next multiple of 512 tile sum,
 * weighted so every distinct canonical line inside the pool is equally likely.
 */
public class BoardPool {
    final String tableName;
    final boolean playing6561;
    final double spawnRate;
    final double targetProb;
    final long rootCanon;
    final int startLayer;
    final int targetLayer;
    final HashMap<Long, BigInteger> lineCounts;

    BoardPool(String tableName, boolean playing6561, double spawnRate, double targetProb,
              long rootCanon, int startLayer, int targetLayer, HashMap<Long, BigInteger> lineCounts) {
        this.tableName = tableName;
        this.playing6561 = playing6561;
        this.spawnRate = spawnRate;
        this.targetProb = targetProb;
        this.rootCanon = rootCanon;
        this.startLayer = startLayer;
        this.targetLayer = targetLayer;
        this.lineCounts = lineCounts;
    }

    boolean matchesConfig(String tableName, boolean playing6561, double spawnRate, double targetProb) {
        return this.playing6561 == playing6561
                && Double.compare(this.spawnRate, spawnRate) == 0
                && Double.compare(this.targetProb, targetProb) == 0
                && this.tableName.equals(tableName);
    }

    boolean containsPosition(long canon, int layer) {
        if (rootCanon == canon && startLayer == layer)
            return true;
        return layer < targetLayer && lineCounts.containsKey(canon);
    }
}

/**
 * Builds, caches, and samples from {@link BoardPool} instances for fractional SpawnAlgorithm modes.
 */
class BoardPoolManager {
    private static final int MAX_CACHED_POOLS = 8;
    private static final ArrayDeque<BoardPool> CACHE = new ArrayDeque<>();
    static BoardPool activePool = null;

    static void ensurePool(int[] spaces) {
        if (!Space.poolMode) {
            activePool = null;
            return;
        }
        Board3x3 start = new Board3x3(spaces);
        long canon = start.getSmallestSymmetry();
        int startLayer = start.getSum();
        if (startLayer <= 0) {
            activePool = null;
            return;
        }

        if (activePool != null
                && activePool.matchesConfig(Input.tableName, Input.playing6561, Input.spawnRate, Space.poolProb)
                && activePool.containsPosition(canon, startLayer)) {
            return;
        }

        for (BoardPool pool : CACHE) {
            if (pool.matchesConfig(Input.tableName, Input.playing6561, Input.spawnRate, Space.poolProb)
                    && pool.containsPosition(canon, startLayer)) {
                activePool = pool;
                CACHE.remove(pool);
                CACHE.addFirst(pool);
                System.out.println("Reusing cached spawn pool (" + pool.lineCounts.size() + " boards).");
                return;
            }
        }

        int base = Input.playing6561 ? 3 : 2;
        int tileSum = startLayer * base;
        int nextMultipleSum = ((tileSum / 512) + 1) * 512;
        int targetLayer = Math.max(startLayer + 1, nextMultipleSum / base);

        BoardPool built = buildPool(canon, startLayer, targetLayer);
        activePool = built;
        if (built != null) {
            CACHE.addFirst(built);
            while (CACHE.size() > MAX_CACHED_POOLS)
                CACHE.removeLast();
        }
    }

    /**
     * Samples an in-pool spawn on the post-merge board so every distinct line in the pool is equally likely.
     * Returns false if past target or no in-pool child exists (caller falls back to learny).
     */
    static boolean spawnFromPool(int[] space, ArrayList<Integer> empties) {
        BoardPool pool = activePool;
        if (pool == null || empties.isEmpty())
            return false;

        Board3x3 post = new Board3x3(space);
        if (post.getSum() >= pool.targetLayer)
            return false;

        ArrayList<BigInteger> weights = new ArrayList<>();
        ArrayList<ArrayList<int[]>> groups = new ArrayList<>();
        HashMap<Long, Integer> groupIdx = new HashMap<>();

        for (int emptyPos : empties) {
            for (int sv = 1; sv <= 2; sv++) {
                long child = post.name | ((long) sv << (4 * emptyPos));
                long childCanon = Board3x3.getSmallestSymmetry(child);
                BigInteger lc = pool.lineCounts.get(childCanon);
                if (lc == null || lc.signum() <= 0)
                    continue;

                Integer idx = groupIdx.get(childCanon);
                if (idx == null) {
                    idx = weights.size();
                    groupIdx.put(childCanon, idx);
                    weights.add(lc);
                    groups.add(new ArrayList<>());
                }
                groups.get(idx).add(new int[]{emptyPos, sv});
            }
        }

        if (weights.isEmpty())
            return false;

        BigInteger total = BigInteger.ZERO;
        for (BigInteger w : weights)
            total = total.add(w);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        BigInteger r;
        int bits = total.bitLength();
        do {
            r = new BigInteger(bits, rng);
        } while (r.compareTo(total) >= 0);

        for (int i = 0; i < weights.size(); i++) {
            BigInteger w = weights.get(i);
            if (r.compareTo(w) < 0) {
                ArrayList<int[]> spawns = groups.get(i);
                int[] chosen = spawns.get(rng.nextInt(spawns.size()));
                space[chosen[0]] = chosen[1];
                return true;
            }
            r = r.subtract(w);
        }
        return false;
    }

    private static class Node {
        int id;
        long canon;
        int layer;
        double reach;
        int[] childIds;
        double[] childWeights;
        long[] childCanons;
        int[] childLayers;
    }

    private static class BestMoveResult {
        double sr;
        long[] canons;
        int[] layers;
        double[] weights;
    }

    private static BoardPool buildPool(long rootCanon, int startLayer, int targetLayer) {
        long t0 = System.currentTimeMillis();
        int sv2Delta = Input.playing6561 ? 3 : 2;
        int maxNeededLayer = Math.min(1024, targetLayer + sv2Delta);

        System.out.printf("Building spawn pool (p=%.4f) for layer %d -> %d...%n",
                Space.poolProb, startLayer, targetLayer);

        long[][] keys = new long[maxNeededLayer + 1][];
        double[][] vals = new double[maxNeededLayer + 1][];
        try {
            loadLayerSlice(Input.tableDir, startLayer + 1, maxNeededLayer, keys, vals);
        } catch (IOException e) {
            System.out.println("Failed to load table slice for pool: " + e.getMessage());
            return null;
        }

        BestMoveResult rootRes = evalBestMove(rootCanon, startLayer, keys, vals, sv2Delta);
        if (rootRes.sr <= 0.0 || rootRes.canons == null || rootRes.canons.length == 0) {
            System.out.println("Start position has 0% table success rate; falling back to learny.");
            return null;
        }

        int threads = Math.min(16, Math.max(1, Runtime.getRuntime().availableProcessors()));
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        try {
            @SuppressWarnings("unchecked")
            HashMap<Long, Node>[] dag = new HashMap[targetLayer + 1];
            for (int i = startLayer; i <= targetLayer; i++)
                dag[i] = new HashMap<>();

            List<Node> allNodes = new ArrayList<>();
            Node root = new Node();
            root.id = 0;
            root.canon = rootCanon;
            root.layer = startLayer;
            root.reach = 1.0;
            dag[startLayer].put(rootCanon, root);
            allNodes.add(root);

            for (int L = startLayer; L < targetLayer; L++) {
                HashMap<Long, Node> curMap = dag[L];
                if (curMap.isEmpty())
                    continue;
                Node[] nodes = curMap.values().toArray(new Node[0]);
                int chunks = Math.min(threads, nodes.length);
                int chunkSize = (nodes.length + chunks - 1) / chunks;
                List<Callable<Void>> tasks = new ArrayList<>(chunks);
                final int curL = L;
                for (int c = 0; c < chunks; c++) {
                    int sIdx = c * chunkSize;
                    int eIdx = Math.min(nodes.length, sIdx + chunkSize);
                    tasks.add(() -> {
                        for (int i = sIdx; i < eIdx; i++) {
                            Node nd = nodes[i];
                            BestMoveResult res = evalBestMove(nd.canon, curL, keys, vals, sv2Delta);
                            nd.childCanons = res.canons;
                            nd.childLayers = res.layers;
                            nd.childWeights = res.weights;
                        }
                        return null;
                    });
                }
                exec.invokeAll(tasks);

                for (Node nd : nodes) {
                    if (nd.childCanons == null)
                        continue;
                    nd.childIds = new int[nd.childCanons.length];
                    for (int j = 0; j < nd.childCanons.length; j++) {
                        int storeL = Math.min(nd.childLayers[j], targetLayer);
                        Node child = dag[storeL].get(nd.childCanons[j]);
                        if (child == null) {
                            child = new Node();
                            child.id = allNodes.size();
                            child.canon = nd.childCanons[j];
                            child.layer = storeL;
                            dag[storeL].put(nd.childCanons[j], child);
                            allNodes.add(child);
                        }
                        nd.childIds[j] = child.id;
                        child.reach += nd.reach * nd.childWeights[j];
                    }
                    nd.childCanons = null;
                    nd.childLayers = null;
                }
            }

            int nNodes = allNodes.size();
            Node[] nodesByLayer = allNodes.toArray(new Node[0]);
            Arrays.sort(nodesByLayer, Comparator.comparingInt(a -> a.layer));

            double[] sortedReaches = new double[nNodes];
            for (int i = 0; i < nNodes; i++)
                sortedReaches[i] = nodesByLayer[i].reach;
            Arrays.sort(sortedReaches);

            double[] stay = new double[nNodes];
            double maxPossibleStay = evalStay(nodesByLayer, stay, 0.0, targetLayer);
            double effectiveTarget = Math.min(Space.poolProb, maxPossibleStay);

            int lo = 0;
            int hi = nNodes - 1;
            int bestIdx = 0;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                double s = evalStay(nodesByLayer, stay, sortedReaches[mid], targetLayer);
                if (s >= effectiveTarget) {
                    bestIdx = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            double finalStay = evalStay(nodesByLayer, stay, sortedReaches[bestIdx], targetLayer);

            // Forward reachability filter inside the backward-pruned pool so no unreachable nodes remain
            boolean[] reachableInPool = new boolean[nNodes];
            reachableInPool[0] = stay[0] > 0.0;
            for (Node nd : nodesByLayer) {
                if (!reachableInPool[nd.id] || nd.childIds == null)
                    continue;
                for (int cid : nd.childIds) {
                    if (stay[cid] > 0.0)
                        reachableInPool[cid] = true;
                }
            }

            // Backward line-count DP over distinct canonical paths in the pruned pool
            BigInteger[] lines = new BigInteger[nNodes];
            HashMap<Long, BigInteger> poolNodes = new HashMap<>();
            for (int i = nodesByLayer.length - 1; i >= 0; i--) {
                Node nd = nodesByLayer[i];
                if (!reachableInPool[nd.id] || stay[nd.id] <= 0.0) {
                    lines[nd.id] = BigInteger.ZERO;
                    continue;
                }
                if (nd.layer >= targetLayer) {
                    lines[nd.id] = BigInteger.ONE;
                } else {
                    BigInteger sum = BigInteger.ZERO;
                    for (int cid : nd.childIds) {
                        if (reachableInPool[cid] && stay[cid] > 0.0)
                            sum = sum.add(lines[cid]);
                    }
                    lines[nd.id] = sum;
                }
                if (lines[nd.id].signum() > 0)
                    poolNodes.put(nd.canon, lines[nd.id]);
            }

            System.out.printf("Spawn pool built in %d ms: %d / %d boards, stayProb=%.4f (target=%.4f)%n",
                    System.currentTimeMillis() - t0, poolNodes.size(), nNodes, finalStay, Space.poolProb);

            return new BoardPool(Input.tableName, Input.playing6561, Input.spawnRate, Space.poolProb,
                    rootCanon, startLayer, targetLayer, poolNodes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            exec.shutdown();
        }
    }

    private static double evalStay(Node[] nodesByLayer, double[] stay, double thresh, int targetLayer) {
        for (int i = nodesByLayer.length - 1; i >= 0; i--) {
            Node nd = nodesByLayer[i];
            if (nd.reach < thresh) {
                stay[nd.id] = 0.0;
            } else if (nd.layer >= targetLayer) {
                stay[nd.id] = 1.0;
            } else if (nd.childIds == null) {
                stay[nd.id] = 0.0;
            } else {
                double s = 0.0;
                for (int j = 0; j < nd.childIds.length; j++) {
                    double cs = stay[nd.childIds[j]];
                    if (cs > 0.0)
                        s += nd.childWeights[j] * cs;
                }
                stay[nd.id] = s;
            }
        }
        return stay[0];
    }

    private static void loadLayerSlice(File tableDir, int minL, int maxL, long[][] keys, double[][] vals) throws IOException {
        if (tableDir == null)
            return;
        for (int l = minL; l <= maxL; l++) {
            Path p = tableDir.toPath().resolve(l + ".txt");
            if (!Files.exists(p))
                continue;
            byte[] raw = Files.readAllBytes(p);
            int n = raw.length / 16;
            long[] k = new long[n];
            double[] v = new double[n];
            ByteBuffer bb = ByteBuffer.wrap(raw);
            for (int i = 0; i < n; i++) {
                k[i] = bb.getLong();
                v[i] = bb.getDouble();
            }
            keys[l] = k;
            vals[l] = v;
        }
    }

    private static double lookup(long[][] keys, double[][] vals, int layer, long canon) {
        if (layer < 0 || layer >= keys.length)
            return 0.0;
        long[] k = keys[layer];
        if (k == null)
            return 0.0;
        int idx = Arrays.binarySearch(k, canon);
        return idx >= 0 ? vals[layer][idx] : 0.0;
    }

    private static BestMoveResult evalBestMove(long canon, int layer, long[][] keys, double[][] vals, int sv2Delta) {
        BestMoveResult res = new BestMoveResult();
        double[] mvRates = new double[4];
        double bestSr = 0.0;
        for (int mv = 0; mv < 4; mv++) {
            long merged = Board3x3.merge(canon, mv);
            if (merged == canon)
                continue;
            int zc = Board3x3.getZeroCt(merged);
            double mvRate = 0.0;
            for (int sv = 1; sv <= 2; sv++) {
                double factor = (sv == 1 ? (1.0 - Input.spawnRate) : Input.spawnRate) / zc;
                int nl = layer + (sv == 1 ? 1 : sv2Delta);
                for (int place = 0; place < 9; place++) {
                    if ((merged & (0b1111L << (4 * place))) == 0) {
                        mvRate += factor * lookup(keys, vals, nl,
                                Board3x3.getSmallestSymmetry(merged | ((long) sv << (4 * place))));
                    }
                }
            }
            mvRates[mv] = mvRate;
            if (mvRate > bestSr)
                bestSr = mvRate;
        }
        res.sr = bestSr;
        if (bestSr <= 0.0)
            return res;

        double optThreshold = bestSr * (1.0 - 1e-14);
        int optCount = 0;
        for (int mv = 0; mv < 4; mv++) {
            if (mvRates[mv] >= optThreshold)
                optCount++;
        }

        HashMap<Long, Double> map1 = new HashMap<>();
        HashMap<Long, Double> map2 = new HashMap<>();
        for (int mv = 0; mv < 4; mv++) {
            if (mvRates[mv] < optThreshold)
                continue;
            long merged = Board3x3.merge(canon, mv);
            int zc = Board3x3.getZeroCt(merged);
            double mvSr = mvRates[mv];
            for (int sv = 1; sv <= 2; sv++) {
                double p = (sv == 1 ? (1.0 - Input.spawnRate) : Input.spawnRate) / zc;
                int nl = layer + (sv == 1 ? 1 : sv2Delta);
                HashMap<Long, Double> m = (sv == 1) ? map1 : map2;
                for (int place = 0; place < 9; place++) {
                    if ((merged & (0b1111L << (4 * place))) == 0) {
                        long childCanon = Board3x3.getSmallestSymmetry(merged | ((long) sv << (4 * place)));
                        double childSr = lookup(keys, vals, nl, childCanon);
                        if (childSr > 0.0)
                            m.merge(childCanon, (p * childSr / mvSr) / optCount, Double::sum);
                    }
                }
            }
        }
        int sz = map1.size() + map2.size();
        res.canons = new long[sz];
        res.layers = new int[sz];
        res.weights = new double[sz];
        int idx = 0;
        for (var e : map1.entrySet()) {
            res.canons[idx] = e.getKey();
            res.layers[idx] = layer + 1;
            res.weights[idx] = e.getValue();
            idx++;
        }
        for (var e : map2.entrySet()) {
            res.canons[idx] = e.getKey();
            res.layers[idx] = layer + sv2Delta;
            res.weights[idx] = e.getValue();
            idx++;
        }
        return res;
    }
}
