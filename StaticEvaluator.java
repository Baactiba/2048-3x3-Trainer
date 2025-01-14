import java.util.*;
import java.io.*;
public class StaticEvaluator
{
    static long minNodes = 4000;
    static double mult = 1.6;
    static long endgame512 = 100000;
    static long endgame1024 = 1000000;
    static Expectimax e;
    static boolean simple = false;
    static long millisecondsComputeTime;
    static public void setModel(String model)
    {
        e = switch(model) {
            case "6" -> new Expectimax("6.txt", 56, 14);
            case "d4.5" -> new Expectimax(true);
            default -> null;
        };
    }
    static public void toggleSimple()
    {
        simple = !simple;
    }
    static public void setSettings(int ms)
    {
        millisecondsComputeTime = ms;
    }
    static public void setSettings(long minNodes, double mult, long endgame512, long endgame1024)
    {
        System.out.println("Settings called.");
        StaticEvaluator.minNodes = minNodes;
        StaticEvaluator.mult = mult;
        StaticEvaluator.endgame512 = endgame512;
        StaticEvaluator.endgame1024 = endgame1024;

    }
    static public int getMoveFromTrainer(int[] space)
    {
        return switch(getMove(new int[][]{ {space[6], space[7], space[8]}, {space[3], space[4], space[5]},
                {space[0], space[1], space[2]}})) {
            case "U" -> 38;
            case "L" -> 37;
            case "D" -> 40;
            case "R" -> 39;
            default -> -1;
        };
    }
    static public String getMove(int[][] board)
    {
        if (millisecondsComputeTime == 0) {
            long decidedDepth = minNodes;
            ArrayList<Integer> vals = new ArrayList<>();
            int zeroCount = 0;
            for (int[] row : board)
                for (int i : row) {
                    vals.add(i);
                    if (i == 0)
                        zeroCount++;
                }
            decidedDepth = (int) (minNodes * Math.pow(mult, 7 - zeroCount));
            Collections.sort(vals);
            int currEndgame = 0;
    /*        if (vals.get(6) > 5) {
                if (vals.get(6) == 5 && vals.contains(4) && vals.contains(3) && vals.contains(2))
                    currEndgame = 5;
                else if (vals.get(6) == 6 && vals.contains(5) && vals.contains(4) && vals.contains(3) && vals.contains(2))
                    currEndgame = 6;
                else if (vals.get(6) == 7 && vals.contains(6) && vals.contains(5) && vals.contains(4) && vals.contains(3) && vals.contains(2))
                    currEndgame = 7;
            } */
            switch (vals.get(6)) {
                case 8, 7 -> decidedDepth = endgame1024;
                case 6, 5 -> decidedDepth = endgame512;
            }
            if (simple)
                decidedDepth = minNodes;
            System.out.println("Finding move with depth " + decidedDepth);
            var answer = e.eval2(new Board3x3(board), decidedDepth, 1);
            ArrayList<String> moves = new ArrayList<>(answer.keySet());
            ArrayList<Double> scores = new ArrayList<>(answer.values());
            double bestScore = scores.get(0);
            for (int x = 1; x < moves.size(); x++)
                if (scores.get(x) == bestScore) {
                    System.out.println("Decided on " + moves.get(x) + " at depth " + answer.get("DEPTH"));
                    return moves.get(x);
                }
            return "BREAK";
        }
        else
        {
            HashMap<String, Double> answer = null;
            e.eval2set.clear();
            long sTime = System.currentTimeMillis();
            long startDepth = minNodes;
            while (System.currentTimeMillis() - sTime < millisecondsComputeTime) {
                answer = e.eval2(new Board3x3(board), startDepth *= 1.7, 1);
            }
            ArrayList<String> moves = new ArrayList<>(answer.keySet());
            ArrayList<Double> scores = new ArrayList<>(answer.values());
            double bestScore = scores.get(0);
            for (int x = 1; x < moves.size(); x++)
                if (scores.get(x) == bestScore) {
                    System.out.println("Decided on " + moves.get(x) + " at depth " + answer.get("DEPTH")
                            + " with " + startDepth + " nodes.");
                    return moves.get(x);
                }
            return "BREAK";
        }
    }
}