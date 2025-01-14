
import java.io.*;
import java.util.*;
public class Expectimax
{
    static int evalsDone = 0;
    static int maxDepth = 0;
    boolean bod = false;
    public Expectimax(String fileName, int l1size, int l2size)
    {

    }
    public Expectimax(boolean basedOnDeath)
    {
        bod = basedOnDeath;
    }
    public HashMap<String, Double> eval(Board3x3 board, int depth) // To a certain depth.
    {
        HashMap<String, Double> ret = new HashMap<>();
        ArrayList<Double> evals = new ArrayList<>();
        for (String s : board.legalMoves())
        {
//            System.out.println(s);
            if (depth == 1)
            {
                DblArr<int[][], Double> children = board.children(s);
                var boards = children.getKeys();
                var scores = children.getValues();
                double evalNum = 0.0;
                for (int i = 0; i < scores.size(); i++)
                {
                    if (bod)
                    {
                        if (!(new Board3x3(boards.get(i)).isDead()))
                        {
                            ArrayList<Integer> corners = new ArrayList<>();
                            int[][] b = boards.get(i);
                            corners.add(b[0][0]);
                            corners.add(b[2][0]);
                            corners.add(b[0][2]);
                            corners.add(b[2][2]);
                            ArrayList<Integer> edges = new ArrayList<>();
                            edges.add(b[0][1]);
                            edges.add(b[1][0]);
                            edges.add(b[1][2]);
                            edges.add(b[2][1]);
                            int maxCorner = Collections.max(corners);
                            int maxEdge = Collections.max(edges);
                            edges.remove((Integer)maxEdge);
                            int maxEdge2 = Collections.max(edges);
                            double scalar = 1.0;
                            if (b[1][1] > maxEdge2 || b[1][1] > maxEdge)
                                scalar *= 0.0;
//                            double evalNum0 = evalNum;
                            evalNum += (scores.get(i) * ((double) maxCorner / 10)) * scalar;
//                            System.out.println(evalNum - evalNum0 + " " + scores.get(i));
                        }
                    }
                    else
                        evalNum += scores.get(i) * 0;
                }
                ret.put(s, evalNum);
                evals.add(evalNum);
                evalsDone++;
                totalNodes += 1;
            }
            else
            {
                DblArr<int[][], Double> children = board.children(s);
                var boards = children.getKeys();
                var scores = children.getValues();
                double evalNum = 0.0;
                for (int i = 0; i < scores.size(); i++)
                {
                    evalNum += scores.get(i) * eval(new Board3x3(boards.get(i)), depth-1).get(" ");
                }
                ret.put(s, evalNum);
                evals.add(evalNum);
            }
        }
        if (!ret.isEmpty())
        {
            ret.put(" ", Collections.max(evals));
//            System.out.println(board  + "\ndepth " + depth + " eval " + Collections.max(evals));
        }
        else
            ret.put(" ", 0.0);
        return ret;
    }
    static long totalNodes = 0;
    static long wastedNodes = 0;
    static long evalLookups = 0;
    HashMap<Board3x3, Object[]> eval2set = new HashMap<>();
    public HashMap<String, Double> eval2(Board3x3 board, long nodes, int depth) // By nodes.
    {
        if (depth == 1)
        {
            maxDepth = 1;
            Board3x3.children.clear();
        }
        if (depth > maxDepth) {
            maxDepth = depth;
        }
        if (eval2set.containsKey(board))
        {
            Object[] ans = eval2set.get(board);
            if ((long)(ans[0]) >= nodes)
            {
                evalLookups++;
                return (HashMap<String, Double>) ans[1];
            }
        }
        if (nodes == 0)
        {
            return eval(board, 1);
        } // Now the else case: Divide up the nodes.
//        System.out.println("Evaluating " + board + " with " + nodes + " nodes.");
        HashMap<String, Double> ret;
        ArrayList<Double> evals = new ArrayList<>();
        ret = eval(board, 1);
        if (ret.size() == 1)
        {
            wastedNodes += nodes;
            return ret;
        }
        HashMap<String, Double> transform = transform(ret, depth);
//        if (depth == 1)
//            System.out.println(transform);
        long thisNodesLocal = 0;
        for (String s : board.legalMoves())
        {
            long nodesLocal = (long)(Math.ceil(nodes * transform.get(s)));
            thisNodesLocal += nodesLocal;
            DblArr<int[][], Double> children = board.children(s);
            var boards = children.getKeys();
            var scores = children.getValues();

            for (int x = scores.size() - 1; x > -1; x--) // Cleaning the arrays to not waste nodes on dead boards
            {
                if (new Board3x3(boards.get(x)).isDead())
                {
                    boards.remove(x);
                    scores.remove(x);
                }
            }


            double evalNum = 0.0;
            for (int i = 0; i < scores.size(); i++)
            {
                evalNum += scores.get(i) * eval2(new Board3x3(boards.get(i)), (long)(nodesLocal * scores.get(i)), depth + 1).get(" ");
            }
            ret.put(s, evalNum);
            evals.add(evalNum);
        }
//        System.out.println("Utilized " + thisNodesLocal + " out of " + nodes + " allocated nodes. " + transform.values());
        if (!ret.isEmpty())
        {
            ret.put(" ", Collections.max(evals));
            ret.put("DEPTH", (double)maxDepth);
        }
        else
            ret.put(" ", 0.0);
        eval2set.put(board, new Object[]{nodes, ret});
        return ret;
    }
    public HashMap<String, Double> transform(HashMap<String, Double> map, int depth)
    {
        map.remove(" ");
        map.remove("DEPTH");
        ArrayList<String> moves = new ArrayList<>(map.keySet());
        ArrayList<Double> scores = new ArrayList<>(map.values());
        double sum = 0.0;
        for (double d : scores)
            sum += d;
        sum /= scores.size();
        double sd = 0.0;
        for (double d : scores)
            sd += Math.abs(d - sum);
        sd /= scores.size();
        double[] sds = new double[scores.size()];
        for (int x = 0; x < scores.size(); x++)
            sds[x] = (scores.get(x) - sum) / (sd + 0.0001);
        double[] results = new double[sds.length];
        for (int x = 0; x < scores.size() - 1; x++)
            for (int y = x + 1; y < scores.size(); y++)
            {
                double diff = Math.abs(sds[x] - sds[y]);
                double adding = (1 / (diff + 0.1));
                results[x] += adding;
                results[y] += adding;
            }
        double maxScore = 0.0;
        for (int x = 0; x < scores.size(); x++)
            if (scores.get(x) > maxScore)
                maxScore = scores.get(x);
        for (int x = 0; x < scores.size(); x++)
        {
            results[x] += 1 / (sd + 0.0001);
        }
        sum = 0;
        for (double d : results)
            sum += d;
        for (int x = 0; x < results.length; x++)
            results[x] /= sum;
        for (int x = 0; x < scores.size(); x++)
        {
            results[x] += 1.4  * (scores.get(x) - maxScore); // Tweak Factor
            if (results[x] < 0) results[x] = 0;
        }
        sum = 0;
        for (double d : results)
            sum += d;
        for (int x = 0; x < results.length; x++)
            results[x] /= sum;
        sum = 0;
        if (depth < 99)
        {
            for (int x = 0; x < results.length; x++)
                results[x] += 1.0;
            for (double d : results)
                sum += d;
            for (int x = 0; x < results.length; x++)
                results[x] /= sum;
        }
        HashMap<String, Double> ret = new HashMap<>();
        for (int x = 0; x < scores.size(); x++)
            ret.put(moves.get(x), results[x]);
        return ret;
    }
}