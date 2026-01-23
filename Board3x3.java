import com.koloboke.collect.set.LongSet;
import com.koloboke.collect.set.hash.HashLongSets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Board3x3 {

    public static void main(String[] args) throws IOException {
    }

    static final HashMap<Board3x3, Move> stored = new HashMap<>();

    static double[] TILE_VALS = {0, 0, 3.273, 14.545, 45.091, 122.182, 308.364, 744.727, 1745.455, 4002.909, 9029.818};
    static boolean NO_X = false;
    static boolean SPECIAL_SUCCESS = false;
    static boolean THREE_TILE = false;
    static boolean[] SPECIAL_GOAL = new boolean[11];
    static boolean SPECIAL_DEATH = false;
    static int NO_X_GOAL = 8627;
    static int GOAL = 8;
    static int MAX_LAYER = 160;

    long name;
    double finalSuccessRate = -1.0;
    double[] successRates = new double[4];
    int zeroCt = -1;

    public Board3x3(long name) {
        this.name = name;
    }

    public Board3x3(long name, double finalSuccessRate) {
        this.name = name;
        this.finalSuccessRate = finalSuccessRate;
    }

    public Board3x3(int... arr) {
        for (int i = 0; i < arr.length; i++) {
            name |= (long) arr[i] << (4 * i);
        }
    }

    public long getName() {
        return name;
    }

    public Map<Long, Integer> getChildren(int direction, int val) {
        HashMap<Long, Integer> hi = new HashMap<>();
        long merged = merge(direction);
        if (merged != name)
            for (int place = 0; place < 9; place++) {
                if ((merged & (0b1111L << (4 * place))) == 0) {
                    long thing = merged;
                    thing |= (long) val << (4 * place);
                    long ss = getSmallestSymmetry(thing);
                    if (!hi.containsKey(ss))
                        hi.put(ss, 1);
                    else
                        hi.put(ss, hi.get(ss) + 1);

                }
            }
        return hi;
    }

    public HashMap<Board3x3, Double> moveSpawns(int direction, int... vals) {

        direction = (direction + 1) % 4;

        HashMap<Board3x3, Double> hi = new HashMap<>();
        long merged = merge(direction);
        if (merged != name) {
            int zc = new Board3x3(merged).getZeroCt();
            for (int val : vals) {
                double divisor = vals.length == 1 ? zc : zc / (1.7 - 0.8 * val);
                for (int place = 0; place < 9; place++) {
                    if ((merged & (0b1111L << (4 * place))) == 0) {
                        long thing = merged;
                        thing |= (long) val << (4 * place);
                        Board3x3 ss = new Board3x3(thing);
                        if (!hi.containsKey(ss))
                            hi.put(ss, 1.0 / divisor);
                    }
                }
            }
        }
        return hi;
    }

    public Set<Long> getChildrenSpawn(int spawnVal) {
        LongSet hi = HashLongSets.newMutableSet();
        for (int x = 0; x < 4; x++) {
            long merged = merge(x);
            if (merged != name) {
                for (int place = 0; place < 9; place++) {
                    if ((merged & (0b1111L << (4 * place))) == 0) {
                        long thing = merged;
                        thing |= (long) spawnVal << (4 * place);
                        hi.add(getSmallestSymmetry(thing));
                    }
                }
            }
        }
        return hi;
    }

    public static Set<Long> getChildrenSpawn(long name, int spawnVal) {
        LongSet hi = HashLongSets.newMutableSet();
        for (int x = 0; x < 4; x++) {
            long merged = merge(name, x);
            if (merged != name) {
                for (int place = 0; place < 9; place++) {
                    if ((merged & (0b1111L << (4 * place))) == 0) {
                        long thing = merged;
                        thing |= (long) spawnVal << (4 * place);
                        hi.add(getSmallestSymmetry(thing));
                    }
                }
            }
        }
        return hi;
    }

    public static Set<Long> getChildrenSpawnGen(long name, int spawnVal) {

        LongSet hi = HashLongSets.newMutableSet();
        if (!(Board3x3.isSuccess(name) || Board3x3.isDead(name))) {
            for (int x = 0; x < 4; x++) {
                long merged = merge(name, x);
                if (merged != name) {
                    for (int place = 0; place < 9; place++) {
                        if ((merged & (0b1111L << (4 * place))) == 0) {
                            long thing = merged;
                            thing |= (long) spawnVal << (4 * place);
                            hi.add(getSmallestSymmetry(thing));
                        }
                    }
                }
            }
        }
        return hi;
    }

    public long merge(int direction) {
        long ret = 0;
        switch (direction) {
            case 0: { // down
                int row1 = (int) ((name & (0b1111L))) + 16 * (int) ((name & (0b1111L << 12)) >> 12) + 256 * (int) ((name & (0b1111L << 24)) >> 24);
                int row2 = (int) ((name & (0b1111L << 4)) >> 4) + 16 * (int) ((name & (0b1111L << 16)) >> 16) + 256 * (int) ((name & (0b1111L << 28)) >> 28);
                int row3 = (int) ((name & (0b1111L << 8)) >> 8) + 16 * (int) ((name & (0b1111L << 20)) >> 20) + 256 * (int) ((name & (0b1111L << 32)) >> 32);
                int[] merge1 = merges[row1];
                int[] merge2 = merges[row2];
                int[] merge3 = merges[row3];
                ret |= (long) merge1[0] << 24;
                ret |= (long) merge1[1] << 12;
                ret |= merge1[2];
                ret |= (long) merge2[0] << 28;
                ret |= (long) merge2[1] << 16;
                ret |= (long) merge2[2] << 4;
                ret |= (long) merge3[0] << 32;
                ret |= (long) merge3[1] << 20;
                ret |= (long) merge3[2] << 8;
                break;
            }
            case 2: { // up
                int row1 = (int) ((name & (0b1111L << 24)) >> 24) + 16 * (int) ((name & (0b1111L << 12)) >> 12) + 256 * (int) ((name & (0b1111L)));
                int row2 = (int) ((name & (0b1111L << 28)) >> 28) + 16 * (int) ((name & (0b1111L << 16)) >> 16) + 256 * (int) ((name & (0b1111L << 4)) >> 4);
                int row3 = (int) ((name & (0b1111L << 32)) >> 32) + 16 * (int) ((name & (0b1111L << 20)) >> 20) + 256 * (int) ((name & (0b1111L << 8)) >> 8);
                int[] merge1 = merges[row1];
                int[] merge2 = merges[row2];
                int[] merge3 = merges[row3];
                ret |= merge1[0];
                ret |= (long) merge1[1] << 12;
                ret |= (long) merge1[2] << 24;
                ret |= (long) merge2[0] << 4;
                ret |= (long) merge2[1] << 16;
                ret |= (long) merge2[2] << 28;
                ret |= (long) merge3[0] << 8;
                ret |= (long) merge3[1] << 20;
                ret |= (long) merge3[2] << 32;
                break;
            }
            case 1: { // left
                int row1 = (int) ((name & (0b1111L))) + 16 * (int) ((name & (0b1111L << 4)) >> 4) + 256 * (int) ((name & (0b1111L << 8)) >> 8);
                int row2 = (int) ((name & (0b1111L << 12)) >> 12) + 16 * (int) ((name & (0b1111L << 16)) >> 16) + 256 * (int) ((name & (0b1111L << 20)) >> 20);
                int row3 = (int) ((name & (0b1111L << 24)) >> 24) + 16 * (int) ((name & (0b1111L << 28)) >> 28) + 256 * (int) ((name & (0b1111L << 32)) >> 32);
                int[] merge1 = merges[row1];
                int[] merge2 = merges[row2];
                int[] merge3 = merges[row3];
                ret |= (long) merge1[0] << 8;
                ret |= (long) merge1[1] << 4;
                ret |= merge1[2];
                ret |= (long) merge2[0] << 20;
                ret |= (long) merge2[1] << 16;
                ret |= (long) merge2[2] << 12;
                ret |= (long) merge3[0] << 32;
                ret |= (long) merge3[1] << 28;
                ret |= (long) merge3[2] << 24;
                break;
            }
            case 3: { // right
                int row1 = (int) ((name & (0b1111L << 8)) >> 8) + 16 * (int) ((name & (0b1111L << 4)) >> 4) + 256 * (int) ((name & (0b1111L)));
                int row2 = (int) ((name & (0b1111L << 20)) >> 20) + 16 * (int) ((name & (0b1111L << 16)) >> 16) + 256 * (int) ((name & (0b1111L << 12)) >> 12);
                int row3 = (int) ((name & (0b1111L << 32)) >> 32) + 16 * (int) ((name & (0b1111L << 28)) >> 28) + 256 * (int) ((name & (0b1111L << 24)) >> 24);
                int[] merge1 = merges[row1];
                int[] merge2 = merges[row2];
                int[] merge3 = merges[row3];
                ret |= merge1[0];
                ret |= (long) merge1[1] << 4;
                ret |= (long) merge1[2] << 8;
                ret |= (long) merge2[0] << 12;
                ret |= (long) merge2[1] << 16;
                ret |= (long) merge2[2] << 20;
                ret |= (long) merge3[0] << 24;
                ret |= (long) merge3[1] << 28;
                ret |= (long) merge3[2] << 32;
                break;
            }
        }
        return ret;
    }

    public static long merge(long name, int direction) {
        long ret = 0;
        switch (direction) {
            case 0: { // down
                int row1 = (int) ((name & (0b1111L))) + 16 * (int) ((name & (0b1111L << 12)) >> 12) + 256 * (int) ((name & (0b1111L << 24)) >> 24);
                int row2 = (int) ((name & (0b1111L << 4)) >> 4) + 16 * (int) ((name & (0b1111L << 16)) >> 16) + 256 * (int) ((name & (0b1111L << 28)) >> 28);
                int row3 = (int) ((name & (0b1111L << 8)) >> 8) + 16 * (int) ((name & (0b1111L << 20)) >> 20) + 256 * (int) ((name & (0b1111L << 32)) >> 32);
                int[] merge1 = merges[row1];
                int[] merge2 = merges[row2];
                int[] merge3 = merges[row3];
                ret |= (long) merge1[0] << 24;
                ret |= (long) merge1[1] << 12;
                ret |= merge1[2];
                ret |= (long) merge2[0] << 28;
                ret |= (long) merge2[1] << 16;
                ret |= (long) merge2[2] << 4;
                ret |= (long) merge3[0] << 32;
                ret |= (long) merge3[1] << 20;
                ret |= (long) merge3[2] << 8;
                break;
            }
            case 2: { // up
                int row1 = (int) ((name & (0b1111L << 24)) >> 24) + 16 * (int) ((name & (0b1111L << 12)) >> 12) + 256 * (int) ((name & (0b1111L)));
                int row2 = (int) ((name & (0b1111L << 28)) >> 28) + 16 * (int) ((name & (0b1111L << 16)) >> 16) + 256 * (int) ((name & (0b1111L << 4)) >> 4);
                int row3 = (int) ((name & (0b1111L << 32)) >> 32) + 16 * (int) ((name & (0b1111L << 20)) >> 20) + 256 * (int) ((name & (0b1111L << 8)) >> 8);
                int[] merge1 = merges[row1];
                int[] merge2 = merges[row2];
                int[] merge3 = merges[row3];
                ret |= merge1[0];
                ret |= (long) merge1[1] << 12;
                ret |= (long) merge1[2] << 24;
                ret |= (long) merge2[0] << 4;
                ret |= (long) merge2[1] << 16;
                ret |= (long) merge2[2] << 28;
                ret |= (long) merge3[0] << 8;
                ret |= (long) merge3[1] << 20;
                ret |= (long) merge3[2] << 32;
                break;
            }
            case 1: { // left
                int row1 = (int) ((name & (0b1111L))) + 16 * (int) ((name & (0b1111L << 4)) >> 4) + 256 * (int) ((name & (0b1111L << 8)) >> 8);
                int row2 = (int) ((name & (0b1111L << 12)) >> 12) + 16 * (int) ((name & (0b1111L << 16)) >> 16) + 256 * (int) ((name & (0b1111L << 20)) >> 20);
                int row3 = (int) ((name & (0b1111L << 24)) >> 24) + 16 * (int) ((name & (0b1111L << 28)) >> 28) + 256 * (int) ((name & (0b1111L << 32)) >> 32);
                int[] merge1 = merges[row1];
                int[] merge2 = merges[row2];
                int[] merge3 = merges[row3];
                ret |= (long) merge1[0] << 8;
                ret |= (long) merge1[1] << 4;
                ret |= merge1[2];
                ret |= (long) merge2[0] << 20;
                ret |= (long) merge2[1] << 16;
                ret |= (long) merge2[2] << 12;
                ret |= (long) merge3[0] << 32;
                ret |= (long) merge3[1] << 28;
                ret |= (long) merge3[2] << 24;
                break;
            }
            case 3: { // right
                int row1 = (int) ((name & (0b1111L << 8)) >> 8) + 16 * (int) ((name & (0b1111L << 4)) >> 4) + 256 * (int) ((name & (0b1111L)));
                int row2 = (int) ((name & (0b1111L << 20)) >> 20) + 16 * (int) ((name & (0b1111L << 16)) >> 16) + 256 * (int) ((name & (0b1111L << 12)) >> 12);
                int row3 = (int) ((name & (0b1111L << 32)) >> 32) + 16 * (int) ((name & (0b1111L << 28)) >> 28) + 256 * (int) ((name & (0b1111L << 24)) >> 24);
                int[] merge1 = merges[row1];
                int[] merge2 = merges[row2];
                int[] merge3 = merges[row3];
                ret |= merge1[0];
                ret |= (long) merge1[1] << 4;
                ret |= (long) merge1[2] << 8;
                ret |= (long) merge2[0] << 12;
                ret |= (long) merge2[1] << 16;
                ret |= (long) merge2[2] << 20;
                ret |= (long) merge3[0] << 24;
                ret |= (long) merge3[1] << 28;
                ret |= (long) merge3[2] << 32;
                break;
            }
        }
        return ret;
    }

    public double getSuccessRate(int direction) {
        return successRates[direction];
    }

    public double getSuccessRate() {
        return finalSuccessRate;
    }

    public void setSuccessRate(int direction, double d) {
        successRates[direction] = d;
    }

    public void addSuccessRate(int direction, double d) {
        successRates[direction] += d;
    }

    public void finalizeSuccessRate() {
        if (finalSuccessRate == -1.0)
            for (double d : successRates)
                if (d > finalSuccessRate)
                    finalSuccessRate = d;
    }

    public boolean isSuccess() {
        int[] tiles = new int[9];
        if (SPECIAL_SUCCESS) {
            boolean[] found = new boolean[11];
            for (int x = 0; x < 9; x++) {
                int tile = (int) ((name & (0b1111L << (4 * x))) >> (4 * x));
                found[tile] = true;
            }
            for (int x = 0; x < SPECIAL_GOAL.length; x++)
                if (SPECIAL_GOAL[x] && !found[x])
                    return false;
            return true;
        }
        for (int x = 0; x < 9; x++) {
            tiles[x] = (int) ((name & (0b1111L << (4 * x))) >> (4 * x));
            if (tiles[x] == GOAL) {
                if (!(NO_X || THREE_TILE))
                    return true;
            }
        }
        if (THREE_TILE) {
            int zeroCt = 0;
            boolean foundGoal = false;
            for (int i : tiles)
                if (i == 0)
                    zeroCt++;
                else if (i >= GOAL)
                    foundGoal = true;
            if (zeroCt >= 6 && foundGoal)
                return true;
        }
        if (NO_X) {
            double totalSc = 0;
            for (int i : tiles) {
                if (i >= GOAL)
                    return false;
                totalSc += TILE_VALS[i];
            }
            return totalSc >= NO_X_GOAL;
        }
        return false;
    }

    public static boolean isSuccess(long name) {
        int[] tiles = new int[9];
        if (SPECIAL_SUCCESS) {
            boolean[] found = new boolean[11];
            for (int x = 0; x < 9; x++) {
                int tile = (int) ((name & (0b1111L << (4 * x))) >> (4 * x));
                found[tile] = true;
            }
            for (int x = 0; x < SPECIAL_GOAL.length; x++)
                if (SPECIAL_GOAL[x] && !found[x])
                    return false;
            return true;
        }
        for (int x = 0; x < 9; x++) {
            tiles[x] = (int) ((name & (0b1111L << (4 * x))) >> (4 * x));
            if (tiles[x] == GOAL) {
                if (!(NO_X || THREE_TILE))
                    return true;
            }
        }
        if (THREE_TILE) {
            int zeroCt = 0;
            boolean foundGoal = false;
            for (int i : tiles)
                if (i == 0)
                    zeroCt++;
                else if (i >= GOAL)
                    foundGoal = true;
            if (zeroCt >= 6 && foundGoal)
                return true;
        }
        if (NO_X) {
            double totalSc = 0;
            for (int i : tiles) {
                if (i >= GOAL)
                    return false;
                totalSc += TILE_VALS[i];
            }
            return totalSc >= NO_X_GOAL;
        }
        return false;
    }

    public boolean isDead() {
        if (NO_X) {
            for (int x = 0; x < 9; x++)
                if ((name & (0b1111L << (4 * x))) >> (4 * x) >= GOAL)
                    return true;
        }
        int[] tiles = new int[9];
        for (int x = 0; x < 9; x++) {
            tiles[x] = (int) ((name & (0b1111L << (4 * x))) >> (4 * x));
        }
        int sum = 0;
        for (int i : tiles)
            if (i > 0)
                sum += (int) Math.pow(2, i);
        if (sum / 2 > MAX_LAYER)
            return true;
        if (getZeroCt() == 0)
            for (int x = 0; x < 9; x++) {
                if (x % 3 > 0)
                    if (tiles[x] == tiles[x - 1])
                        return false;
                if (x > 2)
                    if (tiles[x] == tiles[x - 3])
                        return false;
            }
        if (SPECIAL_DEATH) {

        }
        return false;
    }

    public static boolean isDead(long name) {
        if (NO_X) {
            for (int x = 0; x < 9; x++)
                if ((name & (0b1111L << (4 * x))) >> (4 * x) >= GOAL)
                    return true;
        }
        int[] tiles = new int[9];
        for (int x = 0; x < 9; x++) {
            tiles[x] = (int) ((name & (0b1111L << (4 * x))) >> (4 * x));
        }
        int sum = 0;
        for (int i : tiles)
            if (i > 0)
                sum += (int) Math.pow(2, i);
        if (sum / 2 > MAX_LAYER)
            return true;
        if (getZeroCt(name) == 0)
            for (int x = 0; x < 9; x++) {
                if (x % 3 > 0)
                    if (tiles[x] == tiles[x - 1])
                        return false;
                if (x > 2)
                    if (tiles[x] == tiles[x - 3])
                        return false;
            }
        if (SPECIAL_DEATH) {

        }
        return false;
    }

    public int getZeroCt() {
        if (zeroCt != -1)
            return zeroCt;
        zeroCt = 0;
        for (int x = 0; x <= 32; x += 4) {
            if (((name & (0b1111L << x)) >> x) == 0)
                zeroCt++;
        }
        return zeroCt;
    }

    public static int getZeroCt(long name) {
        int zeroCt = 0;
        for (int x = 0; x <= 32; x += 4) {
            if (((name & (0b1111L << x)) >> x) == 0)
                zeroCt++;
        }
        return zeroCt;
    }

    public int getSum() {
        int sum = 0;
        for (int x = 0; x <= 32; x += 4)
            sum += (int) Math.pow(2, ((name & (0b1111L << x)) >> x));
        return (sum - getZeroCt()) / 2;
    }

    public long getSmallestSymmetry() {
        int[] syms = rotationsToCheck[getTernarySymmetry(name)];
        if (syms.length == 1)
            return getRotation(name, syms[0]);
        long minSc = Long.MAX_VALUE;
        for (int sym : syms) {
            long bd = getRotation(name, sym);
            if (bd < minSc)
                minSc = bd;
        }
        return minSc;
    }

    static public long getSmallestSymmetry(long name) {
        int[] syms = rotationsToCheck[getTernarySymmetry(name)];
        if (syms.length == 1)
            return getRotation(name, syms[0]);
        long minSc = Long.MAX_VALUE;
        for (int sym : syms) {
            long bd = getRotation(name, sym);
            if (bd < minSc)
                minSc = bd;
        }
        return minSc;
    }

    public int hashCode() {
        return (int) name;
    }

    public boolean equals(Object o) {
        if (o instanceof Board3x3 b) {
            return b.name == this.name;
        }
        return false;
    }

    public String toString() {
        String ret = "";
        for (int yCoord = 2; yCoord >= 0; yCoord--) {
            for (int xCoord = 0; xCoord <= 2; xCoord++)
                ret += ((name >> (4 * (3 * yCoord + xCoord))) % 16) + " ";
            ret += "\n";
        }
        return ret + finalSuccessRate;
    }


    static {
        init();
    }

    static int[][] rotationsToCheck;
    static int[][] merges;

    public static void init() {
        rotationsToCheck = new int[19683][];
        for (int x = 0; x < 19683; x++) {
            long hi = 0;
            for (int y = 0; y < 9; y++) {
                int valHere = (int) ((x / Math.pow(3, y)) % 3);
                if (valHere == 1)
                    hi |= 0b0001L << (4 * y);
                else if (valHere == 2)
                    hi |= 0b0111L << (4 * y);
            }
            long minVal = Long.MAX_VALUE;
            ArrayList<Integer> mins = new ArrayList<>();
            for (int y = 0; y < 8; y++) {
                long val = getTernarySymmetry(getRotation(hi, y));
                if (val == minVal)
                    mins.add(y);
                if (val < minVal) {
                    minVal = val;
                    mins.clear();
                    mins.add(y);
                }
            }
            int[] adding = new int[mins.size()];
            for (int z = 0; z < mins.size(); z++)
                adding[z] = mins.get(z);
            rotationsToCheck[x] = adding;
        }
        merges = new int[4096][];
        for (int x = 0; x < 16; x++)
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    merges[z + 16 * y + 256 * x] = mergeDown(new int[]{x, y, z});
    }

    static public int[] mergeDown(int[] tiles) // Merges the array and returns it. DOES MODIFY THE ARRAY!
    {
        for (int z = 0; z < tiles.length; z++)
            for (int x = tiles.length - 1; x > 0; x--)
                if (tiles[x] == 0) {
                    tiles[x] = tiles[x - 1];
                    tiles[x - 1] = 0;
                }
        for (int x = tiles.length - 1; x > 0; x--)
            if (tiles[x] != 0)
                if (tiles[x] == tiles[x - 1]) {
                    tiles[x - 1] = 0;
                    tiles[x]++;
                }
        for (int z = 0; z < tiles.length; z++)
            for (int x = tiles.length - 1; x > 0; x--)
                if (tiles[x] == 0) {
                    tiles[x] = tiles[x - 1];
                    tiles[x - 1] = 0;
                }
        return tiles;
    }

    public static int getTernarySymmetry(long board) {
        int ret = 0;
        int mult = 1;
        for (int x = 0; x < 9; x++) {
            long l = (board & (0b1111L << (4 * x))) >> (4 * x);
            if (l != 0) {
                if (l <= 2)
                    ret += mult;
                else
                    ret += 2 * mult;
            }
            mult *= 3;
        }
        return ret;
    }

    public static long getRotation(long name, int rot) {
        return switch (rot) {
            case 0 -> name;
            case 1 -> (((name & (0b1111L << 24)) >> 24) | ((name & (0b1111L << 12)) >> 8) | ((name & (0b1111L)) << 8) |
                    ((name & (0b1111L << 28)) >> 16) | ((name & (0b1111L << 16))) | ((name & (0b1111L << 4)) << 16) |
                    ((name & (0b1111L << 32)) >> 8) | ((name & (0b1111L << 20)) << 8) | ((name & (0b1111L << 8)) << 24));

            case 2 ->
                    (((name & (0b1111L << 32)) >> 32) | ((name & (0b1111L << 28)) >> 24) | ((name & (0b1111L) << 24) >> 16) |
                            ((name & (0b1111L << 20)) >> 8) | ((name & (0b1111L << 16))) | ((name & (0b1111L << 12)) << 8) |
                            ((name & (0b1111L << 8)) << 16) | ((name & (0b1111L << 4)) << 24) | ((name & (0b1111L)) << 32));

            case 3 ->
                    (((name & (0b1111L << 8)) >> 8) | ((name & (0b1111L << 20)) >> 16) | ((name & (0b1111L << 32)) >> 24) |
                            ((name & (0b1111L << 4)) << 8) | ((name & (0b1111L << 16))) | ((name & (0b1111L << 28)) >> 8) |
                            ((name & (0b1111L)) << 24) | ((name & (0b1111L << 12)) << 16) | ((name & (0b1111L << 24)) << 8));

            case 4 -> (((name & (0b111100000000000011110000000000001111L))) | ((name & (0b1111L << 8)) << 16)
                    | ((name & (0b1111L) << 24) >> 16)
                    | ((name & (0b111100000000000011110000L)) << 8) | ((name & (0b11110000000000001111L << 12)) >> 8));

            case 5 -> (((name & (0b1111000011110000111100000000L))) | ((name & (0b1111L)) << 32)
                    | ((name & (0b1111L) << 32) >> 32)
                    | ((name & (0b1111000011110000L)) << 16) | ((name & (0b111100001111L << 20)) >> 16));

            case 6 ->
                    (((name & (0b11110000000011110000000011110000L))) | ((name & (0b1111000000001111000000001111L)) << 8)
                            | ((name & (0b111100000000111100000000111100000000L)) >> 8));

            case 7 -> (((name & (0b111111111111L) << 12)) | ((name & (0b111111111111L)) << 24)
                    | ((name & (0b111111111111L) << 24) >> 24));

            default -> 0L;
        };
    }

    public long getRotation(int rot) {
        return getRotation(name, rot);
    }

    public int[] getTiles() {
        int[] tiles = new int[9];
        for (int x = 0; x < 9; x++)
            tiles[x] = (int) ((name & (0b1111L << (4 * x))) >> (4 * x));
        return tiles;
    }

    public String classifytf(int threshold) {
        int[] tiles = getTiles();
        int[] spaces = new int[]{3, 3, 3};
        c:
        for (int x = 0; x < 3; x++) {
            for (int y = 2; y > -1; y--) {
                int coord = x + 3 * y;
                if (tiles[coord] > threshold) {
                    spaces[x] = 2 - y;
                    continue c;
                }
            }
        }
        if (spaces[0] > spaces[2])
            return "Formation" + spaces[2] + spaces[1] + spaces[0];
        return "Formation" + spaces[0] + spaces[1] + spaces[2];
    }
}